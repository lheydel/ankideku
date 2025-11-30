package com.ankideku.ui.screens.main.actions

import com.ankideku.data.remote.anki.AnkiConnectException
import com.ankideku.domain.usecase.suggestion.SessionException
import com.ankideku.domain.usecase.suggestion.SessionEvent
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SessionOrchestrator
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.ui.screens.main.ChatMessageType
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface SessionActions {
    fun startSession(prompt: String)
    fun cancelSession()
    fun loadSession(sessionId: Long)
    fun deleteSession(sessionId: Long)
    fun clearSession()
}

class SessionActionsImpl(
    private val ctx: ViewModelContext,
    private val sessionOrchestrator: SessionOrchestrator,
    private val sessionFinder: SessionFinder,
    private val suggestionFinder: SuggestionFinder,
) : SessionActions {

    private var sessionJob: Job? = null

    override fun startSession(prompt: String) {
        val deck = ctx.currentState.selectedDeck ?: return
        if (prompt.isBlank() || ctx.currentState.isProcessing) return

        sessionJob?.cancel()
        sessionJob = ctx.scope.launch {
            ctx.addChatMessage(prompt, ChatMessageType.UserPrompt)
            ctx.addChatMessage("Starting session...", ChatMessageType.SystemInfo)

            try {
                sessionOrchestrator.startSession(deck.id, prompt).collect { event ->
                    handleSessionEvent(event)
                }
            } catch (e: CancellationException) {
                // Ignore - expected on cancel
            } catch (e: SessionException) {
                ctx.addChatMessage(e.message ?: "Session error", ChatMessageType.Error)
            } catch (e: AnkiConnectException) {
                ctx.showToast(e.message ?: "AnkiConnect error", ToastType.Error)
            } catch (e: Exception) {
                ctx.addChatMessage("Session failed: ${e.message}", ChatMessageType.Error)
            }
        }
    }

    override fun cancelSession() {
        val sessionId = ctx.currentState.currentSession?.id ?: return
        ctx.scope.launch {
            sessionOrchestrator.cancelSession(sessionId)
        }
    }

    override fun loadSession(sessionId: Long) {
        ctx.scope.launch {
            try {
                val session = sessionFinder.getById(sessionId)
                if (session == null) {
                    ctx.showToast("Session not found", ToastType.Error)
                    return@launch
                }

                val suggestions = suggestionFinder.observePendingForSession(sessionId).first()
                ctx.update {
                    copy(
                        currentSession = session,
                        suggestions = suggestions,
                        currentSuggestionIndex = 0,
                        editedFields = emptyMap(),
                        isEditing = false,
                    )
                }
            } catch (e: Exception) {
                ctx.showToast("Failed to load session: ${e.message}", ToastType.Error)
            }
        }
    }

    override fun deleteSession(sessionId: Long) {
        ctx.scope.launch {
            try {
                sessionFinder.delete(sessionId)

                // If we deleted the current session, clear it
                if (ctx.currentState.currentSession?.id == sessionId) {
                    clearSession()
                }

                // Refresh sessions list
                val deckId = ctx.currentState.selectedDeck?.id
                if (deckId != null) {
                    val sessions = sessionFinder.getForDeck(deckId)
                    ctx.update { copy(sessions = sessions) }
                }

                ctx.showToast("Session deleted", ToastType.Success)
            } catch (e: Exception) {
                ctx.showToast("Failed to delete session: ${e.message}", ToastType.Error)
            }
        }
    }

    override fun clearSession() {
        ctx.update {
            copy(
                currentSession = null,
                suggestions = emptyList(),
                currentSuggestionIndex = 0,
                editedFields = emptyMap(),
                isEditing = false,
            )
        }
    }

    private fun handleSessionEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.Created -> {
                ctx.scope.launch {
                    val session = sessionFinder.getById(event.sessionId)
                    ctx.update { copy(currentSession = session) }
                }
                ctx.addChatMessage("Processing ${event.noteCount} notes...", ChatMessageType.SystemInfo)
            }
            is SessionEvent.BatchStarted -> {
                // Could update progress UI here
            }
            is SessionEvent.BatchCompleted -> {
                ctx.scope.launch {
                    val session = sessionFinder.getById(event.sessionId)
                    val suggestions = suggestionFinder.observePendingForSession(event.sessionId).first()
                    ctx.update {
                        copy(
                            currentSession = session,
                            suggestions = suggestions,
                        )
                    }
                }
            }
            is SessionEvent.BatchFailed -> {
                ctx.addChatMessage("Batch ${event.batch} failed: ${event.error}", ChatMessageType.Error)
            }
            is SessionEvent.Completed -> {
                ctx.scope.launch {
                    val session = sessionFinder.getById(event.sessionId)
                    ctx.update { copy(currentSession = session) }
                }
                ctx.addChatMessage("Session completed! ${event.totalSuggestions} suggestions generated.", ChatMessageType.SessionResult)
            }
            is SessionEvent.Failed -> {
                ctx.scope.launch {
                    val session = sessionFinder.getById(event.sessionId)
                    ctx.update { copy(currentSession = session) }
                }
                ctx.addChatMessage("Session failed: ${event.error}", ChatMessageType.Error)
            }
            is SessionEvent.Cancelled -> {
                ctx.addChatMessage("Session cancelled.", ChatMessageType.SystemInfo)
            }
        }
    }
}
