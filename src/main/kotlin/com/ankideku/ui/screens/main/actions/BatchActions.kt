package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionStatus
import com.ankideku.domain.sel.SelService
import com.ankideku.domain.sel.SelResult
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.usecase.suggestion.BatchConflictStrategy
import com.ankideku.domain.usecase.suggestion.BatchPreCheckResult
import com.ankideku.domain.usecase.suggestion.BatchReviewFeature
import com.ankideku.domain.usecase.suggestion.BatchReviewResult
import com.ankideku.ui.screens.main.BatchAction
import com.ankideku.ui.screens.main.BatchProgress
import com.ankideku.ui.screens.main.DialogState
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.launch

interface BatchActions {
    fun openBatchFilter()
    fun closeBatchFilter()
    fun executeBatchQuery(query: SelQuery)
    fun clearBatchFilter()
    fun batchAcceptAll()
    fun batchRejectAll()
    fun confirmBatchWithStrategy(action: BatchAction, strategy: BatchConflictStrategy)
    fun cancelBatchDialog()
}

class BatchActionsImpl(
    private val ctx: ViewModelContext,
    private val batchReviewFeature: BatchReviewFeature,
    private val selService: SelService,
) : BatchActions {

    override fun openBatchFilter() {
        // This just signals that the SelBuilderWindow should open
        // The actual opening is handled by MainScreen's state
        // We might add a flag here if needed, but for now UI manages this
    }

    override fun closeBatchFilter() {
        // Close without applying - just dismiss the window
    }

    override fun executeBatchQuery(query: SelQuery) {
        ctx.scope.launch {
            ctx.withLoading(
                setLoading = { copy(isBatchProcessing = true) },
                resetLoading = { copy(isBatchProcessing = false) },
            ) {
                val filtered = executeAndFilter(query)
                if (filtered == null) {
                    ctx.showToast("Query must target Suggestions", ToastType.Error)
                    return@withLoading
                }

                ctx.update {
                    copy(
                        batchFilteredSuggestions = filtered,
                        batchQuery = query,
                        currentSuggestionIndex = 0,
                    )
                }

                if (filtered.isEmpty()) {
                    ctx.showToast("No pending suggestions match your query", ToastType.Info)
                }
            }
        }
    }

    override fun clearBatchFilter() {
        ctx.update {
            copy(
                batchFilteredSuggestions = null,
                batchQuery = null,
                currentSuggestionIndex = 0,
            )
        }
    }

    override fun batchAcceptAll() = startBatchAction(BatchAction.Accept)

    override fun batchRejectAll() = startBatchAction(BatchAction.Reject)

    private fun startBatchAction(action: BatchAction) {
        val suggestions = ctx.currentState.batchFilteredSuggestions ?: return
        if (suggestions.isEmpty()) return

        ctx.scope.launch {
            ctx.update { copy(isBatchProcessing = true) }
            try {
                when (val preCheck = batchReviewFeature.preCheckConflicts(suggestions)) {
                    is BatchPreCheckResult.Ready -> {
                        // No conflicts - proceed directly
                        try {
                            executeBatchAction(action, suggestions, BatchConflictStrategy.Force)
                        } finally {
                            ctx.update { copy(isBatchProcessing = false, batchProgress = null) }
                        }
                    }
                    is BatchPreCheckResult.HasConflicts -> {
                        // Show conflict dialog
                        ctx.update {
                            copy(
                                dialogState = DialogState.BatchConflict(
                                    action = action,
                                    conflicts = preCheck.conflicts,
                                    nonConflicting = preCheck.nonConflicting,
                                ),
                                isBatchProcessing = false,
                            )
                        }
                    }
                    is BatchPreCheckResult.Error -> {
                        ctx.showToast("Pre-check failed: ${preCheck.message}", ToastType.Error)
                        ctx.update { copy(isBatchProcessing = false) }
                    }
                }
            } catch (e: Exception) {
                ctx.showToast("Failed: ${e.message}", ToastType.Error)
                ctx.update { copy(isBatchProcessing = false, batchProgress = null) }
            }
        }
    }

    override fun confirmBatchWithStrategy(action: BatchAction, strategy: BatchConflictStrategy) {
        val suggestions = ctx.currentState.batchFilteredSuggestions ?: return
        ctx.update { copy(dialogState = null) }

        ctx.scope.launch {
            ctx.withBatchProcessing {
                executeBatchAction(action, suggestions, strategy)
            }
        }
    }

    override fun cancelBatchDialog() {
        ctx.update { copy(dialogState = null) }
    }

    private suspend fun executeBatchAction(
        action: BatchAction,
        suggestions: List<Suggestion>,
        strategy: BatchConflictStrategy,
    ) {
        // Initialize progress
        ctx.update { copy(batchProgress = BatchProgress(current = 0, total = suggestions.size)) }

        val onProgress: (Int, Int) -> Unit = { current, total ->
            ctx.update { copy(batchProgress = BatchProgress(current = current, total = total)) }
        }

        val result = when (action) {
            BatchAction.Accept -> batchReviewFeature.batchAccept(suggestions, strategy, onProgress)
            BatchAction.Reject -> batchReviewFeature.batchReject(suggestions, strategy, onProgress)
        }

        when (result) {
            is BatchReviewResult.Success -> {
                val verb = if (action == BatchAction.Accept) "Accepted" else "Rejected"
                val message = if (result.skipped > 0) {
                    "$verb ${result.processed} suggestions, ${result.skipped} skipped"
                } else {
                    "$verb ${result.processed} suggestions"
                }
                ctx.showToast(message, ToastType.Success)

                // If we skipped conflicts, refresh the filter to show remaining
                if (strategy == BatchConflictStrategy.SkipConflicts && result.skipped > 0) {
                    refreshBatchFilter()
                } else {
                    clearBatchFilter()
                }
            }
            is BatchReviewResult.Error -> {
                ctx.showToast("Failed: ${result.message}", ToastType.Error)
            }
        }
    }

    private suspend fun refreshBatchFilter() {
        val query = ctx.currentState.batchQuery ?: return
        val filtered = executeAndFilter(query)
        if (filtered == null) {
            clearBatchFilter()
            return
        }
        ctx.update {
            copy(
                batchFilteredSuggestions = filtered,
                currentSuggestionIndex = 0,
            )
        }
    }

    /**
     * Execute a SEL query and filter results to pending suggestions in the current session.
     * Returns null if the query doesn't target suggestions.
     */
    private suspend fun executeAndFilter(query: SelQuery): List<Suggestion>? {
        val result = selService.execute(query)
        if (result !is SelResult.Suggestions) return null

        val currentSessionId = ctx.currentState.currentSession?.id
        return result.items.filter { suggestion ->
            suggestion.sessionId == currentSessionId &&
                suggestion.status == SuggestionStatus.Pending
        }
    }
}
