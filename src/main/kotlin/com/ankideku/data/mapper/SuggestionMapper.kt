package com.ankideku.data.mapper

import com.ankideku.data.local.database.Field_value
import com.ankideku.data.local.database.Suggestion as DbSuggestion
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionStatus

fun DbSuggestion.toDomain(fieldValues: List<Field_value>): Suggestion {
    val byContext = fieldValues.byContext()
    return Suggestion(
        id = id,
        sessionId = session_id,
        noteId = note_id,
        originalFields = (byContext[FieldContext.Original] ?: emptyList()).toNoteFields(),
        changes = (byContext[FieldContext.Changes] ?: emptyList()).toStringMap(),
        reasoning = reasoning,
        status = SuggestionStatus.fromDbString(status),
        editedChanges = byContext[FieldContext.Edited]?.toStringMap(),
        createdAt = created_at,
        decidedAt = decided_at,
    )
}
