package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.repository.ReviewSessionRepository
import com.ankideku.domain.usecase.review.ApplyResult
import com.ankideku.domain.usecase.review.ReviewMessageResult
import com.ankideku.domain.usecase.review.ReviewSessionOrchestrator
import com.ankideku.domain.usecase.review.ReviewSessionResult
import com.ankideku.ui.screens.main.ReviewSessionState
import com.ankideku.ui.screens.main.ToastType
import com.ankideku.ui.screens.main.toUi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface ReviewChatActions {
    fun startReviewSession()
    fun endReviewSession()
    fun sendMessage(content: String, includeSuggestionContext: Boolean = false)
    fun applyReviewSuggestion(reviewSuggestionId: Long)
    fun dismissReviewSuggestion(reviewSuggestionId: Long)
    fun resetConversation()
}

class ReviewChatActionsImpl(
    private val ctx: ViewModelContext,
    private val orchestrator: ReviewSessionOrchestrator,
    private val reviewSessionRepository: ReviewSessionRepository,
) : ReviewChatActions {

    override fun startReviewSession() {
        val session = ctx.currentState.currentSession ?: return

        ctx.scope.launch {
            updateReviewState { copy(isLoading = true, error = null) }

            when (val result = orchestrator.startReviewSession(session.id)) {
                is ReviewSessionResult.Started -> {
                    updateReviewState { copy(isActive = true, isLoading = false) }
                    observeMessages(result.reviewSessionId)
                    observeMemory(result.reviewSessionId)
                }
                is ReviewSessionResult.Resumed -> {
                    updateReviewState { copy(isActive = true, isLoading = false) }
                    observeMessages(result.reviewSessionId)
                    observeMemory(result.reviewSessionId)
                }
                is ReviewSessionResult.Reset -> {
                    // Shouldn't happen on start
                }
                is ReviewSessionResult.Error -> {
                    updateReviewState { copy(isLoading = false, error = result.message) }
                    ctx.showToast("Failed to start review: ${result.message}", ToastType.Error)
                }
            }
        }
    }

    override fun endReviewSession() {
        ctx.scope.launch {
            orchestrator.endReviewSession()
            updateReviewState { ReviewSessionState() }
        }
    }

    override fun sendMessage(content: String, includeSuggestionContext: Boolean) {
        if (content.isBlank()) return

        val currentSuggestionId = if (includeSuggestionContext) {
            ctx.currentState.currentSuggestion?.id
        } else null

        ctx.scope.launch {
            updateReviewState { copy(isLoading = true, error = null) }

            when (val result = orchestrator.sendMessage(
                content = content,
                currentSuggestionId = currentSuggestionId,
                includeCurrentSuggestion = includeSuggestionContext,
            )) {
                is ReviewMessageResult.Success -> {
                    updateReviewState { copy(isLoading = false) }
                    // Messages are updated via flow observation
                    // Update pending suggestions
                    refreshPendingSuggestions()
                }
                is ReviewMessageResult.Error -> {
                    updateReviewState { copy(isLoading = false, error = result.message) }
                    ctx.showToast("Message failed: ${result.message}", ToastType.Error)
                }
            }
        }
    }

    override fun applyReviewSuggestion(reviewSuggestionId: Long) {
        ctx.scope.launch {
            when (val result = orchestrator.applyReviewSuggestion(reviewSuggestionId)) {
                is ApplyResult.Success -> {
                    ctx.showToast("Suggestion applied", ToastType.Success)
                    refreshPendingSuggestions()
                }
                is ApplyResult.Error -> {
                    ctx.showToast("Failed to apply: ${result.message}", ToastType.Error)
                }
            }
        }
    }

    override fun dismissReviewSuggestion(reviewSuggestionId: Long) {
        ctx.scope.launch {
            orchestrator.dismissReviewSuggestion(reviewSuggestionId)
            refreshPendingSuggestions()
        }
    }

    override fun resetConversation() {
        ctx.scope.launch {
            updateReviewState { copy(isLoading = true) }

            when (val result = orchestrator.resetConversation()) {
                is ReviewSessionResult.Reset -> {
                    updateReviewState {
                        copy(
                            messages = emptyList(),
                            pendingSuggestions = emptyList(),
                            isLoading = false,
                        )
                    }
                    ctx.showToast("Conversation reset", ToastType.Info)
                }
                is ReviewSessionResult.Error -> {
                    updateReviewState { copy(isLoading = false, error = result.message) }
                    ctx.showToast("Reset failed: ${result.message}", ToastType.Error)
                }
                else -> {
                    updateReviewState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun observeMessages(reviewSessionId: Long) {
        ctx.scope.launch {
            reviewSessionRepository.getMessages(reviewSessionId).collectLatest { messages ->
                updateReviewState {
                    copy(messages = messages.map { it.toUi() })
                }
            }
        }
    }

    private fun observeMemory(reviewSessionId: Long) {
        ctx.scope.launch {
            // Memory doesn't have a flow, fetch on demand
            val memory = orchestrator.getMemory()
            updateReviewState { copy(memory = memory) }
        }
    }

    private suspend fun refreshPendingSuggestions() {
        val pending = orchestrator.getPendingReviewSuggestions()
        updateReviewState {
            copy(pendingSuggestions = pending.map { it.toUi() })
        }

        // Also refresh memory since it may have changed
        val memory = orchestrator.getMemory()
        updateReviewState { copy(memory = memory) }
    }

    private inline fun updateReviewState(crossinline transform: ReviewSessionState.() -> ReviewSessionState) {
        ctx.update {
            copy(reviewSessionState = reviewSessionState.transform())
        }
    }
}
