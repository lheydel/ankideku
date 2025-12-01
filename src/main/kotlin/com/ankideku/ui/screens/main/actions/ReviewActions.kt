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
    fun selectSuggestion(index: Int)
    fun editField(fieldName: String, value: String)
    fun toggleEditMode()
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
                        // Flow observation will remove accepted item and update list
                        resetEditState()
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
                    is ReviewResult.Success -> {
                        // Flow observation will remove rejected item and update list
                        resetEditState()
                    }
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
                    is ReviewResult.Success -> {
                        // Flow observation will move item to end and update list
                        resetEditState()
                    }
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
            copy(editedFields = editedFields + (fieldName to value))
        }
    }

    override fun toggleEditMode() {
        val state = ctx.currentState
        val suggestion = state.currentSuggestion ?: return

        if (state.isEditMode) {
            // Exiting edit mode - save edits if any
            ctx.scope.launch {
                val editedChanges = state.editedFields.takeIf { it.isNotEmpty() }
                if (editedChanges != null) {
                    // Filter to only include actual changes (different from AI suggestion)
                    val actualEdits = filterActualEdits(editedChanges, suggestion)
                    if (actualEdits.isNotEmpty()) {
                        reviewSuggestionFeature.saveEdits(suggestion.id, actualEdits)
                        ctx.update {
                            copy(
                                isEditMode = false,
                                hasManualEdits = true,
                                showOriginal = false,
                            )
                        }
                    } else {
                        // No actual changes - clear edits if previously saved
                        if (state.hasManualEdits) {
                            reviewSuggestionFeature.clearEdits(suggestion.id)
                        }
                        ctx.update {
                            copy(
                                isEditMode = false,
                                hasManualEdits = false,
                                editedFields = emptyMap(),
                            )
                        }
                    }
                } else {
                    ctx.update { copy(isEditMode = false) }
                }
            }
        } else {
            // Entering edit mode - initialize editedFields with current values
            val initialEdits = if (state.hasManualEdits && state.editedFields.isNotEmpty()) {
                state.editedFields
            } else {
                // Start with AI suggestions as base
                suggestion.changes.toMutableMap()
            }
            ctx.update {
                copy(
                    isEditMode = true,
                    editedFields = initialEdits,
                    showOriginal = false,
                )
            }
        }
    }

    private fun filterActualEdits(
        editedChanges: Map<String, String>,
        suggestion: Suggestion,
    ): Map<String, String> {
        return editedChanges.filter { (fieldName, editedValue) ->
            val aiValue = suggestion.changes[fieldName] ?: suggestion.originalFields[fieldName]?.value
            editedValue != aiValue
        }
    }

    override fun toggleOriginalView() {
        ctx.update { copy(showOriginal = !showOriginal) }
    }

    override fun revertEdits() {
        val suggestion = ctx.currentState.currentSuggestion ?: return

        ctx.scope.launch {
            if (ctx.currentState.hasManualEdits) {
                reviewSuggestionFeature.clearEdits(suggestion.id)
            }
            ctx.update {
                copy(
                    editedFields = emptyMap(),
                    hasManualEdits = false,
                    showOriginal = false,
                )
            }
        }
    }

    override fun selectSuggestion(index: Int) {
        val suggestions = ctx.currentState.suggestions
        if (index in suggestions.indices) {
            // Load hasManualEdits from the suggestion's editedChanges
            val suggestion = suggestions[index]
            val hasEdits = suggestion.editedChanges?.isNotEmpty() == true
            ctx.update {
                copy(
                    currentSuggestionIndex = index,
                    editedFields = suggestion.editedChanges ?: emptyMap(),
                    isEditMode = false,
                    hasManualEdits = hasEdits,
                    showOriginal = false,
                    viewingHistoryEntry = null,
                )
            }
        }
    }

    private fun resetEditState() {
        ctx.update {
            copy(
                editedFields = emptyMap(),
                isEditMode = false,
                hasManualEdits = false,
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
                    resetEditState()
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
            resetEditState()
        }
    }

    private fun dismissDialog() {
        ctx.update { copy(dialogState = null) }
    }
}
