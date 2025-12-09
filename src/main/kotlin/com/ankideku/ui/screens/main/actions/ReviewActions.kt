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
            ctx.withActionLoading {
                if (editedChanges != null) {
                    reviewSuggestionFeature.saveEdits(suggestion.id, editedChanges)
                }

                when (val result = reviewSuggestionFeature.accept(suggestion.id)) {
                    is ReviewResult.Success -> {
                        ctx.resetEditState()
                        ctx.showToast("Changes applied", ToastType.Success)
                    }
                    is ReviewResult.Conflict -> {
                        showConflictDialog(suggestion, result.currentFields)
                    }
                    is ReviewResult.Error -> {
                        ctx.showToast("Failed to apply: ${result.message}", ToastType.Error)
                    }
                }
            }
        }
    }

    override fun rejectSuggestion() {
        val suggestion = ctx.currentState.currentSuggestion ?: return

        ctx.scope.launch {
            ctx.withActionLoading {
                when (val result = reviewSuggestionFeature.reject(suggestion.id)) {
                    is ReviewResult.Success -> ctx.resetEditState()
                    is ReviewResult.Error -> ctx.showToast("Failed to reject: ${result.message}", ToastType.Error)
                    is ReviewResult.Conflict -> { /* shouldn't happen for reject */ }
                }
            }
        }
    }

    override fun skipSuggestion() {
        val suggestion = ctx.currentState.currentSuggestion ?: return

        ctx.scope.launch {
            ctx.withActionLoading {
                when (val result = reviewSuggestionFeature.skip(suggestion.id)) {
                    is ReviewResult.Success -> ctx.resetEditState()
                    is ReviewResult.Error -> ctx.showToast("Failed to skip: ${result.message}", ToastType.Error)
                    is ReviewResult.Conflict -> { /* shouldn't happen for skip */ }
                }
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
        val suggestions = ctx.currentState.displayedSuggestions
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

    private fun showConflictDialog(suggestion: Suggestion, currentFields: Map<String, NoteField>) {
        ctx.update {
            copy(
                dialogState = DialogState.Conflict(
                    suggestion = suggestion,
                    currentFields = currentFields,
                    onUseAi = { resolveConflictWithAi(suggestion.id) },
                    onRefresh = { resolveConflictWithRefresh(suggestion.id, currentFields) },
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
                    ctx.resetEditState()
                    ctx.showToast("Changes applied (overwritten)", ToastType.Success)
                }
                is ReviewResult.Error -> ctx.showToast("Failed: ${result.message}", ToastType.Error)
                is ReviewResult.Conflict -> { /* shouldn't happen for force accept */ }
            }
        }
    }

    private fun resolveConflictWithRefresh(suggestionId: SuggestionId, currentFields: Map<String, NoteField>) {
        ctx.scope.launch {
            reviewSuggestionFeature.refreshConflict(suggestionId, currentFields)
            dismissDialog()
            ctx.resetEditState()
            ctx.showToast("Card refreshed with current state", ToastType.Info)
        }
    }

    private fun dismissDialog() {
        ctx.update { copy(dialogState = null) }
    }
}
