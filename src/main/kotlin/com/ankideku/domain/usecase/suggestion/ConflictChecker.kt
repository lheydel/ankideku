package com.ankideku.domain.usecase.suggestion

import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.data.remote.anki.AnkiConnectException
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Suggestion

/**
 * Service for checking if suggestions have conflicts with their current Anki notes.
 * A conflict occurs when the note has been modified in Anki since the suggestion was created.
 */
class ConflictChecker(
    private val ankiClient: AnkiConnectClient,
) {
    /**
     * Check a single suggestion for conflicts.
     */
    suspend fun check(suggestion: Suggestion): ConflictCheckResult {
        val results = checkAll(listOf(suggestion))
        return results[suggestion.noteId] ?: ConflictCheckResult.Error("Note not found in results")
    }

    /**
     * Check multiple suggestions for conflicts in a single API call.
     * Returns a map of noteId to conflict check result.
     */
    suspend fun checkAll(suggestions: List<Suggestion>): Map<Long, ConflictCheckResult> {
        if (suggestions.isEmpty()) {
            return emptyMap()
        }

        val noteIds = suggestions.map { it.noteId }

        val noteInfos = try {
            ankiClient.notesInfo(noteIds)
        } catch (e: AnkiConnectException) {
            // Return error for all suggestions
            return suggestions.associate { it.noteId to ConflictCheckResult.Error(e.message ?: "Failed to fetch notes") }
        }

        // Map noteId to current fields (filter out invalid entries for deleted notes)
        val currentFieldsMap = noteInfos
            .filter { it.isValid }
            .associate { noteInfo ->
                noteInfo.noteId to noteInfo.fields.mapValues { (name, field) ->
                    NoteField(name = name, value = field.value, order = field.order)
                }
            }

        return suggestions.associate { suggestion ->
            val currentFields = currentFieldsMap[suggestion.noteId]

            val result = if (currentFields == null) {
                ConflictCheckResult.NoteDeleted
            } else {
                val hasConflict = hasFieldChanges(suggestion.originalFields, currentFields)
                if (hasConflict) {
                    ConflictCheckResult.Conflict(currentFields)
                } else {
                    ConflictCheckResult.NoConflict
                }
            }

            suggestion.noteId to result
        }
    }

    /**
     * Check if any original fields differ from current fields.
     */
    private fun hasFieldChanges(
        originalFields: Map<String, NoteField>,
        currentFields: Map<String, NoteField>,
    ): Boolean {
        return originalFields.any { (name, original) ->
            currentFields[name]?.value != original.value
        }
    }
}

/**
 * Result of checking a single suggestion for conflicts.
 */
sealed class ConflictCheckResult {
    /** No conflict - note fields match the original. */
    data object NoConflict : ConflictCheckResult()

    /** Note was deleted in Anki. */
    data object NoteDeleted : ConflictCheckResult()

    /** Note fields have changed since the suggestion was created. */
    data class Conflict(val currentFields: Map<String, NoteField>) : ConflictCheckResult()

    /** Error occurred while checking. */
    data class Error(val message: String) : ConflictCheckResult()
}
