package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.usecase.suggestion.ReviewResult
import com.ankideku.domain.usecase.suggestion.ReviewSuggestionFeature
import com.ankideku.ui.screens.main.DialogState
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.launch

interface ReviewActions {
    fun acceptSuggestion()
    fun rejectSuggestion()
    fun skipSuggestion()
    fun editField(fieldName: String, value: String)
    fun toggleOriginalView()
    fun revertEdits()
}

class ReviewActionsImpl(
    private val ctx: ViewModelContext,
    private val reviewSuggestionFeature: ReviewSuggestionFeature,
) : ReviewActions {

    override fun acceptSuggestion() {
        val suggestion = ctx.currentState.currentSuggestion ?: return
        val editedChanges = ctx.currentState.editedFields.takeIf { it.isNotEmpty() }

        ctx.scope.launch {
            ctx.update { copy(isActionLoading = true) }
            try {
                if (editedChanges != null) {
                    reviewSuggestionFeature.saveEdits(suggestion.id, editedChanges)
                }

                when (val result = reviewSuggestionFeature.accept(suggestion.id)) {
                    is ReviewResult.Success -> {
                        moveToNextSuggestion()
                        ctx.showToast("Changes applied", ToastType.Success)
                    }
                    is ReviewResult.Conflict -> {
                        showConflictDialog(suggestion, result.currentFields)
                    }
                    is ReviewResult.Error -> {
                        ctx.showToast("Failed to apply: ${result.message}", ToastType.Error)
                    }
                }
            } finally {
                ctx.update { copy(isActionLoading = false) }
            }
        }
    }

    override fun rejectSuggestion() {
        val suggestion = ctx.currentState.currentSuggestion ?: return

        ctx.scope.launch {
            ctx.update { copy(isActionLoading = true) }
            try {
                when (val result = reviewSuggestionFeature.reject(suggestion.id)) {
                    is ReviewResult.Success -> moveToNextSuggestion()
                    is ReviewResult.Error -> ctx.showToast("Failed to reject: ${result.message}", ToastType.Error)
                    is ReviewResult.Conflict -> { /* shouldn't happen for reject */ }
                }
            } finally {
                ctx.update { copy(isActionLoading = false) }
            }
        }
    }

    override fun skipSuggestion() {
        val suggestion = ctx.currentState.currentSuggestion ?: return

        ctx.scope.launch {
            ctx.update { copy(isActionLoading = true) }
            try {
                when (val result = reviewSuggestionFeature.skip(suggestion.id)) {
                    is ReviewResult.Success -> moveToNextSuggestion()
                    is ReviewResult.Error -> ctx.showToast("Failed to skip: ${result.message}", ToastType.Error)
                    is ReviewResult.Conflict -> { /* shouldn't happen for skip */ }
                }
            } finally {
                ctx.update { copy(isActionLoading = false) }
            }
        }
    }

    override fun editField(fieldName: String, value: String) {
        ctx.update {
            copy(
                editedFields = editedFields + (fieldName to value),
                isEditing = true,
            )
        }
    }

    override fun toggleOriginalView() {
        ctx.update { copy(showOriginal = !showOriginal) }
    }

    override fun revertEdits() {
        ctx.update {
            copy(
                editedFields = emptyMap(),
                isEditing = false,
            )
        }
    }

    private fun moveToNextSuggestion() {
        ctx.update {
            val newSuggestions = suggestions.filterNot { s -> s.id == currentSuggestion?.id }
            copy(
                suggestions = newSuggestions,
                currentSuggestionIndex = 0,
                editedFields = emptyMap(),
                isEditing = false,
            )
        }
    }

    private fun showConflictDialog(suggestion: Suggestion, currentFields: Map<String, NoteField>) {
        ctx.update {
            copy(
                dialogState = DialogState.Conflict(
                    suggestion = suggestion,
                    currentFields = currentFields,
                    onUseAi = { resolveConflictWithAi(suggestion.id) },
                    onUseCurrent = { resolveConflictWithCurrent(suggestion.id) },
                    onCancel = { dismissDialog() },
                ),
            )
        }
    }

    private fun resolveConflictWithAi(suggestionId: SuggestionId) {
        ctx.scope.launch {
            val editedChanges = ctx.currentState.editedFields.takeIf { it.isNotEmpty() }
            if (editedChanges != null) {
                reviewSuggestionFeature.saveEdits(suggestionId, editedChanges)
            }

            when (val result = reviewSuggestionFeature.forceAccept(suggestionId)) {
                is ReviewResult.Success -> {
                    dismissDialog()
                    moveToNextSuggestion()
                    ctx.showToast("Changes applied (overwritten)", ToastType.Success)
                }
                is ReviewResult.Error -> ctx.showToast("Failed: ${result.message}", ToastType.Error)
                is ReviewResult.Conflict -> { /* shouldn't happen for force accept */ }
            }
        }
    }

    private fun resolveConflictWithCurrent(suggestionId: SuggestionId) {
        ctx.scope.launch {
            reviewSuggestionFeature.skip(suggestionId)
            dismissDialog()
            moveToNextSuggestion()
        }
    }

    private fun dismissDialog() {
        ctx.update { copy(dialogState = null) }
    }
}
