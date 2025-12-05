package com.ankideku.domain.sel.schema

import com.ankideku.data.mapper.FieldContext
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelType

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
        EntityProperty("id", "id", SelType.Number, "ID"),
        EntityProperty("deckId", "deck_id", SelType.Number),
        EntityProperty("deckName", "deck_name", SelType.String, "Deck"),
        EntityProperty("modelName", "model_name", SelType.String, "Note Type"),
        EntityProperty("tags", "tags", SelType.String),
        EntityProperty("mod", "mod", SelType.Number, "Last Modification Date"),
        EntityProperty("estimatedTokens", "estimated_tokens", SelType.Number, "Estimated Tokens"),
        EntityProperty("createdAt", "created_at", SelType.Number),
        EntityProperty("updatedAt", "updated_at", SelType.Number),
    ),
    fieldContexts = listOf(
        SelFieldContext("fields", FieldContext.NOTE_FIELDS.dbValue),
    ),
    relations = emptyList(),
    fieldValueFkColumn = "note_id",
    scopes = listOf(
        EntityScope("deck", "Deck", "deckId", ScopeType.Deck),
    ),
)

val SuggestionSchema = EntitySchema(
    type = EntityType.Suggestion,
    sqlTable = "suggestion",
    sqlAlias = "sugg",
    properties = listOf(
        EntityProperty("id", "id", SelType.Number, "ID"),
        EntityProperty("noteId", "note_id", SelType.Number, "Note ID"),
        EntityProperty("sessionId", "session_id", SelType.Number),
        EntityProperty("reasoning", "reasoning", SelType.String, "Reasoning"),
        EntityProperty("status", "status", SelType.String, "Status"),
        EntityProperty("createdAt", "created_at", SelType.Number, "Creation Date"),
        EntityProperty("decidedAt", "decided_at", SelType.Number, "Review Date"),
        EntityProperty("skippedAt", "skipped_at", SelType.Number),
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
    scopes = listOf(
        EntityScope("session", "Session", "sessionId", ScopeType.Session),
    ),
)

val SessionSchema = EntitySchema(
    type = EntityType.Session,
    sqlTable = "session",
    sqlAlias = "sess",
    properties = listOf(
        EntityProperty("id", "id", SelType.String),
        EntityProperty("deckId", "deck_id", SelType.Number),
        EntityProperty("status", "status", SelType.String),
        EntityProperty("createdAt", "created_at", SelType.Number),
    ),
    fieldContexts = emptyList(),
    relations = emptyList(),
)

val HistoryEntrySchema = EntitySchema(
    type = EntityType.HistoryEntry,
    sqlTable = "history_entry",
    sqlAlias = "hist",
    properties = listOf(
        EntityProperty("id", "id", SelType.Number, "ID"),
        EntityProperty("noteId", "note_id", SelType.Number, "Note ID"),
        EntityProperty("suggestionId", "suggestion_id", SelType.Number, "Suggestion ID"),
        EntityProperty("sessionId", "session_id", SelType.Number),
        EntityProperty("deckId", "deck_id", SelType.Number),
        EntityProperty("deckName", "deck_name", SelType.String, "Deck"),
        EntityProperty("action", "action", SelType.String),
        EntityProperty("reasoning", "reasoning", SelType.String, "Reasoning"),
        EntityProperty("timestamp", "timestamp", SelType.Number),
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
    scopes = listOf(
        EntityScope("session", "Session", "sessionId", ScopeType.Session),
        EntityScope("deck", "Deck", "deckId", ScopeType.Deck),
    ),
)
