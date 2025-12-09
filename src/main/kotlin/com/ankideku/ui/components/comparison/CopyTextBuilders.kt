package com.ankideku.ui.components.comparison

import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Suggestion

fun buildSuggestionCopyText(suggestion: Suggestion, editedFields: Map<String, String>): String {
    return buildCopyText(
        reasoning = suggestion.reasoning,
        originalFields = suggestion.originalFields,
        changes = suggestion.changes,
        editedFields = editedFields,
    )
}

fun buildHistoryCopyText(entry: HistoryEntry): String {
    return buildCopyText(
        reasoning = entry.reasoning.orEmpty(),
        originalFields = entry.originalFields,
        changes = entry.appliedChanges ?: entry.aiChanges,
        editedFields = emptyMap(),
    )
}

fun buildNoteCopyText(note: Note) = buildString {
    appendLine("## Note Preview")
    appendLine("Note Type: ${note.modelName}")
    appendLine("Deck: ${note.deckName}")
    if (note.tags.isNotEmpty()) {
        appendLine("Tags: ${note.tags.joinToString(", ")}")
    }
    appendLine()
    appendLine("## Fields")
    note.fields.values.sortedBy { it.order }.forEach { field ->
        appendLine("**${field.name}:** ${field.value}")
    }
}

private fun buildCopyText(
    reasoning: String,
    originalFields: Map<String, NoteField>,
    changes: Map<String, String>,
    editedFields: Map<String, String>,
) = buildString {
    appendLine("## AI Reasoning")
    appendLine(reasoning)
    appendLine()
    appendLine("## Original Card")
    originalFields.forEach { (name, field) -> appendLine("**$name:** ${field.value}") }
    appendLine()
    appendLine("## Changes")
    changes.forEach { (name, value) -> appendLine("**$name:** ${editedFields[name] ?: value}") }
}
