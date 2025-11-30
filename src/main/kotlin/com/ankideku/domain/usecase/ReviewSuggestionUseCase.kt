package com.ankideku.domain.usecase

import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.data.remote.anki.AnkiConnectException
import com.ankideku.domain.model.*
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.domain.service.TransactionService
import com.ankideku.util.onIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles reviewing (accepting/rejecting/skipping) AI suggestions.
 * Uses TransactionService for atomic operations.
 */
class ReviewSuggestionUseCase(
    private val sessionRepository: SessionRepository,
    private val deckRepository: DeckRepository,
    private val historyRepository: HistoryRepository,
    private val transactionService: TransactionService,
    private val ankiClient: AnkiConnectClient,
) {
    /**
     * Accept a suggestion and apply changes to Anki.
     * @return Result with conflict info if note was modified since suggestion
     */
    suspend fun accept(suggestionId: SuggestionId): ReviewResult {
        return doAccept(suggestionId, checkConflict = true)
    }

    /**
     * Accept a suggestion even if there's a conflict.
     * Use this when user chooses "Use AI" in conflict dialog.
     */
    suspend fun forceAccept(suggestionId: SuggestionId): ReviewResult {
        return doAccept(suggestionId, checkConflict = false)
    }

    /**
     * Reject a suggestion (no changes applied).
     */
    suspend fun reject(suggestionId: SuggestionId): ReviewResult {
        return recordDecision(suggestionId, SuggestionStatus.Rejected, ReviewAction.Reject)
    }

    /**
     * Skip a suggestion for now (can review later).
     */
    suspend fun skip(suggestionId: SuggestionId): ReviewResult {
        return recordDecision(suggestionId, SuggestionStatus.Skipped, ReviewAction.Skip)
    }

    /**
     * Save user edits to a suggestion (without accepting).
     */
    suspend fun saveEdits(suggestionId: SuggestionId, editedChanges: Map<String, String>): ReviewResult {
        onIO { sessionRepository.saveEditedChanges(suggestionId, editedChanges) }
        return ReviewResult.Success
    }

    /**
     * Clear user edits from a suggestion (revert to AI suggestion).
     */
    suspend fun clearEdits(suggestionId: SuggestionId): ReviewResult {
        onIO { sessionRepository.clearEditedChanges(suggestionId) }
        return ReviewResult.Success
    }

    private suspend fun doAccept(suggestionId: SuggestionId, checkConflict: Boolean): ReviewResult {
        val suggestion = onIO { sessionRepository.getSuggestionById(suggestionId) }
            ?: return ReviewResult.Error("Suggestion not found")

        val session = onIO { sessionRepository.getSession(suggestion.sessionId) }
            ?: return ReviewResult.Error("Session not found")

        // Check for conflicts if requested
        if (checkConflict) {
            val conflictCheck = try {
                checkForConflict(suggestion)
            } catch (e: AnkiConnectException) {
                return ReviewResult.Error("Failed to check for conflicts: ${e.message}")
            }

            when (conflictCheck) {
                is ConflictCheck.Conflict -> return ReviewResult.Conflict(
                    suggestionId = suggestionId,
                    currentFields = conflictCheck.currentFields,
                )
                ConflictCheck.NoteDeleted -> return ReviewResult.Error("Note was deleted in Anki")
                ConflictCheck.NoConflict -> { /* proceed */ }
            }
        }

        // Determine which changes to apply (user edits take priority)
        val changesToApply = suggestion.editedChanges ?: suggestion.changes

        // Apply changes to Anki (external, cannot be rolled back)
        try {
            ankiClient.updateNoteFields(suggestion.noteId, changesToApply)
        } catch (e: AnkiConnectException) {
            return ReviewResult.Error("Failed to update note in Anki: ${e.message}")
        }

        // All DB operations in a single transaction
        val userEdits = calculateUserEdits(changesToApply, suggestion.changes)
        val historyEntry = createHistoryEntry(suggestion, session, ReviewAction.Accept, changesToApply, userEdits)

        transactionService.runInTransaction {
            deckRepository.updateNoteFields(suggestion.noteId, changesToApply)
            sessionRepository.updateSuggestionStatus(suggestionId, SuggestionStatus.Accepted)
            historyRepository.saveEntry(historyEntry)
        }

        return ReviewResult.Success
    }

    private suspend fun recordDecision(
        suggestionId: SuggestionId,
        status: SuggestionStatus,
        action: ReviewAction,
    ): ReviewResult {
        val suggestion = onIO { sessionRepository.getSuggestionById(suggestionId) }
            ?: return ReviewResult.Error("Suggestion not found")

        val session = onIO { sessionRepository.getSession(suggestion.sessionId) }
            ?: return ReviewResult.Error("Session not found")

        val historyEntry = createHistoryEntry(suggestion, session, action, appliedChanges = null, userEdits = null)

        transactionService.runInTransaction {
            sessionRepository.updateSuggestionStatus(suggestionId, status)
            historyRepository.saveEntry(historyEntry)
        }

        return ReviewResult.Success
    }

    private fun calculateUserEdits(
        changesToApply: Map<String, String>,
        aiChanges: Map<String, String>,
    ): Map<String, String>? {
        if (changesToApply == aiChanges) return null
        return changesToApply.filter { (key, value) -> aiChanges[key] != value }
            .takeIf { it.isNotEmpty() }
    }

    private fun createHistoryEntry(
        suggestion: Suggestion,
        session: Session,
        action: ReviewAction,
        appliedChanges: Map<String, String>?,
        userEdits: Map<String, String>?,
    ) = HistoryEntry(
        sessionId = suggestion.sessionId,
        noteId = suggestion.noteId,
        deckId = session.deckId,
        deckName = session.deckName,
        action = action,
        originalFields = suggestion.originalFields,
        aiChanges = suggestion.changes,
        appliedChanges = appliedChanges,
        userEdits = userEdits,
        reasoning = suggestion.reasoning,
        timestamp = System.currentTimeMillis(),
    )

    private suspend fun checkForConflict(suggestion: Suggestion): ConflictCheck {
        val noteInfos = ankiClient.notesInfo(listOf(suggestion.noteId))
        if (noteInfos.isEmpty()) {
            return ConflictCheck.NoteDeleted
        }

        val noteInfo = noteInfos.first()
        val currentFields = noteInfo.fields.mapValues { (name, field) ->
            NoteField(name = name, value = field.value, order = field.order)
        }

        val hasFieldChanges = suggestion.originalFields.any { (name, original) ->
            currentFields[name]?.value != original.value
        }

        return if (hasFieldChanges) {
            ConflictCheck.Conflict(currentFields)
        } else {
            ConflictCheck.NoConflict
        }
    }
}

/**
 * Result of a review action
 */
sealed class ReviewResult {
    data object Success : ReviewResult()
    data class Error(val message: String) : ReviewResult()
    data class Conflict(
        val suggestionId: SuggestionId,
        val currentFields: Map<String, NoteField>,
    ) : ReviewResult()
}

private sealed class ConflictCheck {
    data object NoConflict : ConflictCheck()
    data object NoteDeleted : ConflictCheck()
    data class Conflict(val currentFields: Map<String, NoteField>) : ConflictCheck()
}
