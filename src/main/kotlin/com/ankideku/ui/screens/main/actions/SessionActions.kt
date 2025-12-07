package com.ankideku.ui.screens.main.actions

import com.ankideku.data.remote.anki.AnkiConnectException
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.domain.usecase.deck.SyncProgress
import com.ankideku.domain.usecase.suggestion.SessionException
import com.ankideku.domain.usecase.suggestion.SessionEvent
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SessionOrchestrator
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.ChatMessageType
import com.ankideku.ui.screens.main.SyncProgressUi
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface SessionActions {
    fun observeSessions()
    fun startSession(prompt: String)
    fun cancelSession()
    fun loadSession(sessionId: Long)
    fun deleteSession(sessionId: Long)
    fun clearSession()
    fun observeSuggestionsForSession(sessionId: Long)
    fun stopObservingSuggestions()
    fun refreshSuggestionBaselines()
}

class SessionActionsImpl(
    private val ctx: ViewModelContext,
    private val sessionOrchestrator: SessionOrchestrator,
    private val sessionFinder: SessionFinder,
    private val suggestionFinder: SuggestionFinder,
    private val suggestionRepository: SuggestionRepository,
    private val syncDeckFeature: SyncDeckFeature,
    private val deckFinder: DeckFinder,
) : SessionActions {

    private var sessionJob: Job? = null
    private var suggestionsJob: Job? = null

    override fun observeSessions() {
        ctx.scope.launch {
            sessionFinder.observeAll().collect { sessions ->
                ctx.update { copy(sessions = sessions) }
            }
        }
    }

    override fun startSession(prompt: String) {
        var deck = ctx.currentState.selectedDeck ?: return
        if (prompt.isBlank() || ctx.currentState.isProcessing) return

        sessionJob?.cancel()
        sessionJob = ctx.scope.launch {
            ctx.addChatMessage(prompt, ChatMessageType.UserPrompt)

            // Sync first if force sync is enabled OR deck hasn't been synced yet
            val needsSync = ctx.currentState.forceSyncBeforeStart || deck.lastSyncTimestamp == null
            if (needsSync) {
                ctx.addChatMessage("Syncing deck...", ChatMessageType.SystemInfo)
                ctx.update { copy(isSyncing = true) }

                try {
                    syncDeckFeature(deck.id).collect { progress ->
                        val uiProgress = when (progress) {
                            is SyncProgress.Starting -> SyncProgressUi(
                                deckName = progress.deckName,
                                statusText = if (progress.isIncremental) "Incremental sync..." else "Full sync...",
                            )
                            is SyncProgress.SyncingSubDeck -> SyncProgressUi(
                                deckName = progress.subDeckName,
                                statusText = "Syncing ${progress.subDeckName}",
                                step = progress.step,
                                totalSteps = progress.totalSteps,
                            )
                            is SyncProgress.SavingToCache -> SyncProgressUi(
                                deckName = deck.name,
                                statusText = "Saving ${progress.noteCount} notes...",
                            )
                            is SyncProgress.Completed -> null
                        }
                        ctx.update { copy(syncProgress = uiProgress) }
                    }

                    // Reload deck after sync with aggregated stats
                    val updatedDeck = deckFinder.getByIdWithAggregatedStats(deck.id)
                    if (updatedDeck != null) {
                        deck = updatedDeck
                        ctx.update {
                            copy(
                                selectedDeck = updatedDeck,
                                decks = decks.map { d -> if (d.id == deck.id) updatedDeck else d },
                            )
                        }
                    }
                } catch (e: Exception) {
                    ctx.addChatMessage("Sync failed: ${e.message}", ChatMessageType.Error)
                    ctx.update { copy(isSyncing = false, syncProgress = null) }
                    return@launch
                } finally {
                    ctx.update { copy(isSyncing = false, syncProgress = null) }
                }
            }

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

                // Get initial count for chat messages
                val suggestions = suggestionFinder.observePendingForSession(sessionId).first()

                val chatMessages = buildSessionChatMessages(session, suggestions.size)

                ctx.update {
                    copy(
                        currentSession = session,
                        isEditMode = false,
                        chatMessages = chatMessages,
                    )
                }

                // Start observing suggestions - this will update the list reactively
                observeSuggestionsForSession(sessionId)
            } catch (e: Exception) {
                ctx.showToast("Failed to load session: ${e.message}", ToastType.Error)
            }
        }
    }

    private fun buildSessionChatMessages(session: Session, suggestionsCount: Int): List<ChatMessage> {
        // System message with session info (matching V1 format)
        val sessionInfo = buildString {
            append("Session ${session.id}")
            append(" - ${formatDate(session.createdAt)}")
            append("\nDeck: ${session.deckName}")
            append("\n${formatNumber(session.progress.totalCards)} cards")
            append("\nTokens estimate: ${formatNumber(session.progress.inputTokens)} in / ${formatNumber(session.progress.outputTokens)} out")
        }

        // Contextual tip based on session state
        val tip = getContextualTip(session.state, suggestionsCount)

        return listOf(
            ChatMessage(content = sessionInfo, type = ChatMessageType.SystemInfo),
            ChatMessage(content = session.prompt, type = ChatMessageType.UserPrompt),
            ChatMessage(content = tip, type = ChatMessageType.SessionResult),
        )
    }

    private fun getContextualTip(state: SessionState, suggestionsCount: Int): String {
        return when (state) {
            SessionState.Pending, SessionState.Running -> {
                if (suggestionsCount > 0) "← You can review suggestions as they arrive"
                else "Processing your request..."
            }
            SessionState.Completed -> {
                if (suggestionsCount > 0) "← Review suggestions in the card view"
                else "No suggestions found for this deck."
            }
            is SessionState.Failed -> "Try again with a different prompt or check the logs."
            SessionState.Cancelled -> {
                if (suggestionsCount > 0) "← Review the suggestions found before cancellation"
                else "Session was cancelled before finding suggestions."
            }
            SessionState.Incomplete -> {
                if (suggestionsCount > 0) "← Session was interrupted. You can review the suggestions found."
                else "Session was interrupted before finding suggestions."
            }
        }
    }

    private fun formatNumber(n: Int): String = "%,d".format(n)

    private fun formatDate(timestamp: Long): String {
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        return localDate.format(formatter)
    }

    override fun deleteSession(sessionId: Long) {
        ctx.scope.launch {
            try {
                sessionFinder.delete(sessionId)

                // If we deleted the current session, clear it
                if (ctx.currentState.currentSession?.id == sessionId) {
                    clearSession()
                }

                ctx.showToast("Session deleted", ToastType.Success)
            } catch (e: Exception) {
                ctx.showToast("Failed to delete session: ${e.message}", ToastType.Error)
            }
        }
    }

    override fun clearSession() {
        stopObservingSuggestions()
        ctx.update {
            copy(
                currentSession = null,
                suggestions = emptyList(),
                currentSuggestionIndex = 0,
                editedFields = emptyMap(),
                isEditMode = false,
                hasManualEdits = false,
                chatMessages = emptyList(), // Reset to show welcome state
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
                // Start observing suggestions - will update reactively as batches complete
                observeSuggestionsForSession(event.sessionId)
                ctx.addChatMessage("Processing ${event.noteCount} notes...", ChatMessageType.SystemInfo)
            }
            is SessionEvent.BatchStarted -> {
                // Could update progress UI here
            }
            is SessionEvent.BatchCompleted -> {
                // Suggestions are updated via Flow observation - just update session
                ctx.scope.launch {
                    val session = sessionFinder.getById(event.sessionId)
                    ctx.update { copy(currentSession = session) }
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

    override fun observeSuggestionsForSession(sessionId: Long) {
        suggestionsJob?.cancel()
        suggestionsJob = ctx.scope.launch {
            suggestionFinder.observePendingForSession(sessionId).collect { suggestions ->
                val state = ctx.currentState

                // Preserve index position (clamped to list size)
                // This ensures skip/accept/reject move to the next card naturally
                val newIndex = if (suggestions.isEmpty()) {
                    0
                } else {
                    state.currentSuggestionIndex.coerceIn(0, suggestions.lastIndex)
                }

                // Load edits for the new current suggestion (if it changed)
                val newCurrentSuggestion = suggestions.getOrNull(newIndex)
                val currentSuggestionChanged = newCurrentSuggestion?.id != state.currentSuggestion?.id

                val editedFields = if (currentSuggestionChanged) {
                    newCurrentSuggestion?.editedChanges ?: emptyMap()
                } else {
                    state.editedFields
                }
                val hasEdits = if (currentSuggestionChanged) {
                    newCurrentSuggestion?.editedChanges?.isNotEmpty() == true
                } else {
                    state.hasManualEdits
                }

                ctx.update {
                    copy(
                        suggestions = suggestions,
                        currentSuggestionIndex = newIndex,
                        editedFields = editedFields,
                        hasManualEdits = hasEdits,
                    )
                }
            }
        }
    }

    override fun stopObservingSuggestions() {
        suggestionsJob?.cancel()
        suggestionsJob = null
    }

    override fun refreshSuggestionBaselines() {
        val session = ctx.currentState.currentSession ?: return
        if (ctx.currentState.isSyncing) return

        ctx.scope.launch {
            ctx.update { copy(isSyncing = true) }

            try {
                // Sync deck to get latest notes
                syncDeckFeature(session.deckId).collect { progress ->
                    val uiProgress = when (progress) {
                        is SyncProgress.Starting -> SyncProgressUi(
                            deckName = progress.deckName,
                            statusText = if (progress.isIncremental) "Syncing..." else "Full sync...",
                        )
                        is SyncProgress.SyncingSubDeck -> SyncProgressUi(
                            deckName = progress.subDeckName,
                            statusText = "Syncing ${progress.subDeckName}",
                            step = progress.step,
                            totalSteps = progress.totalSteps,
                        )
                        is SyncProgress.SavingToCache -> SyncProgressUi(
                            deckName = session.deckName,
                            statusText = "Saving...",
                        )
                        is SyncProgress.Completed -> null
                    }
                    ctx.update { copy(syncProgress = uiProgress) }
                }

                // Update original fields from cached notes (also touches suggestions to trigger Flow)
                val updated = suggestionRepository.refreshOriginalFields(session.id)
                ctx.showToast("Updated $updated suggestion baselines", ToastType.Success)

            } catch (e: CancellationException) {
                // Ignore
            } catch (e: Exception) {
                ctx.showToast("Refresh failed: ${e.message}", ToastType.Error)
            } finally {
                ctx.update { copy(isSyncing = false, syncProgress = null) }
            }
        }
    }
}
