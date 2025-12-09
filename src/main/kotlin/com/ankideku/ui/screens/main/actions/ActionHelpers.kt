package com.ankideku.ui.screens.main.actions

import com.ankideku.ui.screens.main.MainUiState

/**
 * Wraps an action with loading state management.
 * Sets loading state before execution and resets after (even on failure).
 */
suspend inline fun <T> ViewModelContext.withLoading(
    crossinline setLoading: MainUiState.() -> MainUiState,
    crossinline resetLoading: MainUiState.() -> MainUiState,
    block: () -> T,
): T {
    update { setLoading() }
    try {
        return block()
    } finally {
        update { resetLoading() }
    }
}

/**
 * Convenience wrapper for single-item action loading (accept/reject/skip).
 */
suspend inline fun <T> ViewModelContext.withActionLoading(block: () -> T): T =
    withLoading(
        setLoading = { copy(isActionLoading = true) },
        resetLoading = { copy(isActionLoading = false) },
        block = block,
    )

/**
 * Convenience wrapper for batch processing loading state.
 */
suspend inline fun <T> ViewModelContext.withBatchProcessing(block: () -> T): T =
    withLoading(
        setLoading = { copy(isBatchProcessing = true) },
        resetLoading = { copy(isBatchProcessing = false, batchProgress = null) },
        block = block,
    )

/**
 * Resets edit mode state after accepting/rejecting/skipping a suggestion.
 */
fun ViewModelContext.resetEditState() {
    update {
        copy(
            editedFields = emptyMap(),
            isEditMode = false,
            hasManualEdits = false,
        )
    }
}
