package com.ankideku.domain.sel.schema

import com.ankideku.data.mapper.FieldContext
import com.ankideku.domain.sel.model.EntityType

/**
 * Registry of all entity schemas.
 *
 * Provides lookup of entity metadata for SQL generation:
 * - Table names and aliases
 * - Property (column) mappings
 * - Field context validation
 * - Entity relations
 */
object SelEntityRegistry {
    private val schemas = mutableMapOf<EntityType, EntitySchema>()

    fun register(schema: EntitySchema) {
        schemas[schema.type] = schema
    }

    operator fun get(type: EntityType): EntitySchema =
        schemas[type] ?: error("Unknown entity type: $type")

    fun getProperty(type: EntityType, selKey: String): EntityProperty =
        get(type).getProperty(selKey)
            ?: error("Unknown property '$selKey' for entity $type")

    fun getFieldContext(type: EntityType, selKey: String): SelFieldContext =
        get(type).getFieldContext(selKey)
            ?: error("Unknown field context '$selKey' for entity $type")

    fun getRelation(source: EntityType, target: EntityType): EntityRelation? =
        get(source).relations.find { it.targetEntity == target }

    init {
        register(NoteSchema)
        register(SuggestionSchema)
        register(SessionSchema)
        register(HistoryEntrySchema)
    }
}

// ==================== Entity Schema Definitions ====================

val NoteSchema = EntitySchema(
    type = EntityType.Note,
    sqlTable = "cached_note",
    sqlAlias = "note",
    properties = listOf(
        EntityProperty("id", "id", PropertyType.Long),
        EntityProperty("deckId", "deck_id", PropertyType.Long),
        EntityProperty("deckName", "deck_name", PropertyType.String),
        EntityProperty("modelName", "model_name", PropertyType.String),
        EntityProperty("tags", "tags", PropertyType.String),
        EntityProperty("mod", "mod", PropertyType.Long),
        EntityProperty("estimatedTokens", "estimated_tokens", PropertyType.Int),
        EntityProperty("createdAt", "created_at", PropertyType.Timestamp),
        EntityProperty("updatedAt", "updated_at", PropertyType.Timestamp),
    ),
    fieldContexts = listOf(
        SelFieldContext("fields", FieldContext.NOTE_FIELDS.dbValue),
    ),
    relations = emptyList(),
    fieldValueFkColumn = "note_id",
)

val SuggestionSchema = EntitySchema(
    type = EntityType.Suggestion,
    sqlTable = "suggestion",
    sqlAlias = "sugg",
    properties = listOf(
        EntityProperty("id", "id", PropertyType.Long),
        EntityProperty("noteId", "note_id", PropertyType.Long),
        EntityProperty("sessionId", "session_id", PropertyType.Long),
        EntityProperty("reasoning", "reasoning", PropertyType.String),
        EntityProperty("status", "status", PropertyType.String),
        EntityProperty("createdAt", "created_at", PropertyType.Timestamp),
        EntityProperty("decidedAt", "decided_at", PropertyType.Timestamp),
        EntityProperty("skippedAt", "skipped_at", PropertyType.Timestamp),
    ),
    fieldContexts = listOf(
        SelFieldContext("original", FieldContext.SUGG_ORIGINAL.dbValue),
        SelFieldContext("changes", FieldContext.SUGG_CHANGES.dbValue),
        SelFieldContext("edited", FieldContext.SUGG_EDITED.dbValue),
    ),
    relations = listOf(
        EntityRelation(EntityType.Note, "noteId", "id"),
        EntityRelation(EntityType.Session, "sessionId", "id"),
    ),
    fieldValueFkColumn = "suggestion_id",
)

val SessionSchema = EntitySchema(
    type = EntityType.Session,
    sqlTable = "session",
    sqlAlias = "sess",
    properties = listOf(
        EntityProperty("id", "id", PropertyType.String),
        EntityProperty("deckId", "deck_id", PropertyType.Long),
        EntityProperty("status", "status", PropertyType.String),
        EntityProperty("createdAt", "created_at", PropertyType.Timestamp),
    ),
    fieldContexts = emptyList(),
    relations = emptyList(),
)

val HistoryEntrySchema = EntitySchema(
    type = EntityType.HistoryEntry,
    sqlTable = "history_entry",
    sqlAlias = "hist",
    properties = listOf(
        EntityProperty("id", "id", PropertyType.Long),
        EntityProperty("noteId", "note_id", PropertyType.Long),
        EntityProperty("suggestionId", "suggestion_id", PropertyType.Long),
        EntityProperty("sessionId", "session_id", PropertyType.Long),
        EntityProperty("deckId", "deck_id", PropertyType.Long),
        EntityProperty("deckName", "deck_name", PropertyType.String),
        EntityProperty("action", "action", PropertyType.String),
        EntityProperty("reasoning", "reasoning", PropertyType.String),
        EntityProperty("timestamp", "timestamp", PropertyType.Timestamp),
    ),
    fieldContexts = listOf(
        SelFieldContext("original", FieldContext.HIST_ORIGINAL.dbValue),
        SelFieldContext("aiChanges", FieldContext.HIST_AI_CHANGES.dbValue),
        SelFieldContext("applied", FieldContext.HIST_APPLIED.dbValue),
        SelFieldContext("userEdits", FieldContext.HIST_EDITED.dbValue),
    ),
    relations = listOf(
        EntityRelation(EntityType.Note, "noteId", "id"),
        EntityRelation(EntityType.Suggestion, "suggestionId", "id"),
    ),
    fieldValueFkColumn = "history_id",
)
