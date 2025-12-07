package com.ankideku.domain.usecase.suggestion

import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.data.remote.anki.AnkiConnectException
import com.ankideku.domain.model.*
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.domain.service.TransactionService
import com.ankideku.util.onIO

/**
 * Handles batch accept/reject operations for suggestions with conflict pre-checking.
 */
class BatchReviewFeature(
    private val sessionRepository: SessionRepository,
    private val suggestionRepository: SuggestionRepository,
    private val deckRepository: DeckRepository,
    private val historyRepository: HistoryRepository,
    private val transactionService: TransactionService,
    private val conflictChecker: ConflictChecker,
    private val ankiClient: AnkiConnectClient,
) {
    /**
     * Pre-check all suggestions for conflicts before batch action.
     * A conflict occurs when the note has been modified in Anki since the suggestion was created.
     */
    suspend fun preCheckConflicts(suggestions: List<Suggestion>): BatchPreCheckResult {
        if (suggestions.isEmpty()) {
            return BatchPreCheckResult.Ready(emptyList())
        }

        val checkResults = conflictChecker.checkAll(suggestions)

        val conflicts = mutableListOf<ConflictInfo>()
        val nonConflicting = mutableListOf<Suggestion>()

        for (suggestion in suggestions) {
            when (val result = checkResults[suggestion.noteId]) {
                is ConflictCheckResult.NoConflict -> nonConflicting.add(suggestion)
                is ConflictCheckResult.Conflict -> conflicts.add(
                    ConflictInfo(
                        suggestion = suggestion,
                        currentFields = result.currentFields,
                        isDeleted = false,
                    )
                )
                is ConflictCheckResult.NoteDeleted -> conflicts.add(
                    ConflictInfo(
                        suggestion = suggestion,
                        currentFields = emptyMap(),
                        isDeleted = true,
                    )
                )
                is ConflictCheckResult.Error -> return BatchPreCheckResult.Error(result.message)
                null -> return BatchPreCheckResult.Error("Missing check result for note ${suggestion.noteId}")
            }
        }

        return if (conflicts.isEmpty()) {
            BatchPreCheckResult.Ready(suggestions)
        } else {
            BatchPreCheckResult.HasConflicts(conflicts, nonConflicting)
        }
    }

    /**
     * Batch accept suggestions with the given strategy for handling conflicts.
     * @param onProgress Called with (current, total) as each suggestion is processed
     */
    suspend fun batchAccept(
        suggestions: List<Suggestion>,
        strategy: BatchConflictStrategy,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): BatchReviewResult = batchReview(suggestions, strategy, ReviewAction.Accept, onProgress)

    /**
     * Batch reject suggestions with the given strategy for handling conflicts.
     * @param onProgress Called with (current, total) as each suggestion is processed
     */
    suspend fun batchReject(
        suggestions: List<Suggestion>,
        strategy: BatchConflictStrategy,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): BatchReviewResult = batchReview(suggestions, strategy, ReviewAction.Reject, onProgress)

    private suspend fun batchReview(
        suggestions: List<Suggestion>,
        strategy: BatchConflictStrategy,
        action: ReviewAction,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): BatchReviewResult {
        if (suggestions.isEmpty()) {
            return BatchReviewResult.Success(processed = 0, skipped = 0)
        }

        // Only check conflicts if we need to skip them
        val (toProcess, skipped) = when (strategy) {
            BatchConflictStrategy.Force -> suggestions to 0
            BatchConflictStrategy.SkipConflicts -> {
                when (val preCheck = preCheckConflicts(suggestions)) {
                    is BatchPreCheckResult.Ready -> preCheck.suggestions to 0
                    is BatchPreCheckResult.HasConflicts -> preCheck.nonConflicting to preCheck.conflicts.size
                    is BatchPreCheckResult.Error -> return BatchReviewResult.Error(preCheck.message)
                }
            }
        }

        if (toProcess.isEmpty()) {
            return BatchReviewResult.Success(processed = 0, skipped = skipped)
        }

        // Get session for history entries
        val sessionId = toProcess.first().sessionId
        val session = onIO { sessionRepository.getById(sessionId) }
            ?: return BatchReviewResult.Error("Session not found")

        // For accept: apply changes to Anki (not transactional - partial failures possible)
        val (successfulSuggestions, ankiFailures) = if (action == ReviewAction.Accept) {
            applyChangesToAnki(toProcess, onProgress)
        } else {
            toProcess to 0
        }

        // Update DB in a single transaction
        val status = if (action == ReviewAction.Accept) SuggestionStatus.Accepted else SuggestionStatus.Rejected
        val totalForProgress = successfulSuggestions.size

        transactionService.runInTransaction {
            for ((index, suggestion) in successfulSuggestions.withIndex()) {
                val appliedChanges = if (action == ReviewAction.Accept) {
                    suggestion.editedChanges ?: suggestion.changes
                } else null

                val userEdits = appliedChanges?.let { calculateUserEdits(it, suggestion.changes) }

                val historyEntry = createHistoryEntry(
                    suggestion = suggestion,
                    session = session,
                    action = action,
                    appliedChanges = appliedChanges,
                    userEdits = userEdits,
                )

                if (action == ReviewAction.Accept && appliedChanges != null) {
                    deckRepository.updateNoteFields(suggestion.noteId, appliedChanges)
                }
                suggestionRepository.updateStatus(suggestion.id, status)
                historyRepository.saveEntry(historyEntry)

                // Emit progress for rejects (accepts already emitted during Anki update)
                if (action == ReviewAction.Reject) {
                    onProgress?.invoke(index + 1, totalForProgress)
                }
            }
        }

        return BatchReviewResult.Success(
            processed = successfulSuggestions.size,
            skipped = skipped + ankiFailures,
        )
    }

    private suspend fun applyChangesToAnki(
        suggestions: List<Suggestion>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): Pair<List<Suggestion>, Int> {
        var failures = 0
        val successful = mutableListOf<Suggestion>()
        val total = suggestions.size

        for ((index, suggestion) in suggestions.withIndex()) {
            val changesToApply = suggestion.editedChanges ?: suggestion.changes
            try {
                ankiClient.updateNoteFields(suggestion.noteId, changesToApply)
                successful.add(suggestion)
            } catch (e: AnkiConnectException) {
                failures++
            }
            onProgress?.invoke(index + 1, total)
        }

        return successful to failures
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
        modelName = suggestion.modelName,
        action = action,
        originalFields = suggestion.originalFields,
        aiChanges = suggestion.changes,
        appliedChanges = appliedChanges,
        userEdits = userEdits,
        reasoning = suggestion.reasoning,
        timestamp = System.currentTimeMillis(),
    )
}

/**
 * Result of pre-checking suggestions for conflicts.
 */
sealed class BatchPreCheckResult {
    data class Ready(val suggestions: List<Suggestion>) : BatchPreCheckResult()
    data class HasConflicts(
        val conflicts: List<ConflictInfo>,
        val nonConflicting: List<Suggestion>,
    ) : BatchPreCheckResult()
    data class Error(val message: String) : BatchPreCheckResult()
}

/**
 * Information about a conflicting suggestion.
 */
data class ConflictInfo(
    val suggestion: Suggestion,
    val currentFields: Map<String, NoteField>,
    val isDeleted: Boolean = false,
)

/**
 * Strategy for handling conflicts during batch operations.
 */
enum class BatchConflictStrategy {
    /** Apply changes even to conflicting suggestions (overwrite). */
    Force,
    /** Skip conflicting suggestions, process only non-conflicting ones. */
    SkipConflicts,
}

/**
 * Result of a batch review action.
 */
sealed class BatchReviewResult {
    data class Success(val processed: Int, val skipped: Int) : BatchReviewResult()
    data class Error(val message: String) : BatchReviewResult()
}
