package com.ankideku.data.mapper

import com.ankideku.data.local.database.Field_value
import com.ankideku.data.local.database.FieldValueQueries
import com.ankideku.domain.model.NoteField

enum class FieldContext(val dbValue: String) {
    // Note fields
    NOTE_FIELDS("fields"),
    // Shared (used by both suggestion and history for original fields)
    ORIGINAL("original"),
    // Suggestion contexts
    SUGG_CHANGES("changes"),
    SUGG_EDITED("edited"),
    // History contexts
    HIST_AI_CHANGES("ai_changes"),
    HIST_APPLIED("applied"),
    HIST_EDITED("user_edits"),
    // Review suggestion contexts
    REVIEW_SUGG_CHANGES("review_changes");

    companion object {
        private val byDbValue = entries.associateBy { it.dbValue }
        fun fromDbValue(value: String): FieldContext = byDbValue[value]
            ?: error("Unknown FieldContext: $value")
    }
}

// Field value owner - exactly one is set
sealed class FieldOwner {
    data class Note(val id: Long) : FieldOwner()
    data class Suggestion(val id: Long) : FieldOwner()
    data class History(val id: Long) : FieldOwner()
    data class ReviewSuggestion(val id: Long) : FieldOwner()
}

// Insert helper
fun FieldValueQueries.insertField(
    owner: FieldOwner,
    context: FieldContext,
    fieldName: String,
    fieldValue: String,
    fieldOrder: Long,
) {
    val (noteId, suggestionId, historyId, reviewSuggestionId) = when (owner) {
        is FieldOwner.Note -> FieldOwnerId(noteId = owner.id)
        is FieldOwner.Suggestion -> FieldOwnerId(suggestionId = owner.id)
        is FieldOwner.History -> FieldOwnerId(historyId = owner.id)
        is FieldOwner.ReviewSuggestion -> FieldOwnerId(reviewSuggestionId = owner.id)
    }
    insertField(noteId, suggestionId, historyId, reviewSuggestionId, context.dbValue, fieldName, fieldValue, fieldOrder)
}

private data class FieldOwnerId(
    val noteId: Long? = null,
    val suggestionId: Long? = null,
    val historyId: Long? = null,
    val reviewSuggestionId: Long? = null,
)

// Batch insert helpers
fun FieldValueQueries.insertNoteFields(
    noteId: Long,
    fields: Map<String, NoteField>,
) {
    val owner = FieldOwner.Note(noteId)
    for (field in fields.values) {
        insertField(owner, FieldContext.NOTE_FIELDS, field.name, field.value, field.order.toLong())
    }
}

fun FieldValueQueries.insertFields(
    owner: FieldOwner,
    context: FieldContext,
    fields: Map<String, NoteField>,
) {
    for (field in fields.values) {
        insertField(owner, context, field.name, field.value, field.order.toLong())
    }
}

fun FieldValueQueries.insertFieldsFromMap(
    owner: FieldOwner,
    context: FieldContext,
    fields: Map<String, String>,
) {
    fields.entries.forEachIndexed { index, (name, value) ->
        insertField(owner, context, name, value, index.toLong())
    }
}

// Conversion helpers
fun List<Field_value>.toNoteFields(): Map<String, NoteField> = associate { fv ->
    fv.field_name to NoteField(
        name = fv.field_name,
        value = fv.field_value,
        order = fv.field_order.toInt(),
    )
}

fun List<Field_value>.toStringMap(): Map<String, String> = associate { fv ->
    fv.field_name to fv.field_value
}

fun List<Field_value>.byContext(): Map<FieldContext, List<Field_value>> =
    groupBy { FieldContext.fromDbValue(it.context) }
