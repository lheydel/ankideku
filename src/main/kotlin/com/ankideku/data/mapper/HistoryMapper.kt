package com.ankideku.data.mapper

import com.ankideku.data.local.database.Field_value
import com.ankideku.data.local.database.History_entry
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.ReviewAction

fun History_entry.toDomain(fieldValues: List<Field_value>, modelName: String): HistoryEntry {
    val byContext = fieldValues.byContext()
    return HistoryEntry(
        id = id,
        sessionId = session_id,
        noteId = note_id,
        deckId = deck_id,
        deckName = deck_name,
        modelName = modelName,
        action = ReviewAction.fromDbString(action),
        originalFields = (byContext[FieldContext.ORIGINAL] ?: emptyList()).toNoteFields(),
        aiChanges = (byContext[FieldContext.HIST_AI_CHANGES] ?: emptyList()).toStringMap(),
        appliedChanges = byContext[FieldContext.HIST_APPLIED]?.toStringMap(),
        userEdits = byContext[FieldContext.HIST_EDITED]?.toStringMap(),
        reasoning = reasoning,
        timestamp = timestamp,
    )
}
