package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.FieldOwner
import com.ankideku.data.mapper.insertFields
import com.ankideku.data.mapper.insertFieldsFromMap
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.model.SuggestionStatus
import com.ankideku.domain.repository.SuggestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ankideku.data.local.database.Suggestion as DbSuggestion

class SqlSuggestionRepository(
    private val database: AnkiDekuDb,
) : SuggestionRepository {

    override fun getForSession(sessionId: SessionId): Flow<List<Suggestion>> {
        return database.suggestionQueries.getSuggestionsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> mapWithFields(entities) }
    }

    override fun getPendingForSession(sessionId: SessionId): Flow<List<Suggestion>> {
        return database.suggestionQueries.getPendingSuggestionsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> mapWithFields(entities) }
    }

    override fun getById(suggestionId: SuggestionId): Suggestion? {
        val entity = database.suggestionQueries.getSuggestionById(suggestionId).executeAsOneOrNull()
            ?: return null
        val fields = database.fieldValueQueries.getFieldsForSuggestions(listOf(suggestionId)).executeAsList()
        val modelName = database.deckCacheQueries.getModelNamesForNotes(listOf(entity.note_id)).executeAsList()
            .firstOrNull()?.model_name ?: ""
        return entity.toDomain(fields, modelName)
    }

    override fun save(suggestion: Suggestion) {
        saveAll(listOf(suggestion))
    }

    override fun saveAll(suggestions: List<Suggestion>) {
        if (suggestions.isEmpty()) return
        for (suggestion in suggestions) {
            database.transaction {
                database.suggestionQueries.insertSuggestion(
                    session_id = suggestion.sessionId,
                    note_id = suggestion.noteId,
                    reasoning = suggestion.reasoning,
                    status = suggestion.status.dbString,
                    created_at = System.currentTimeMillis(),
                )
                val suggestionId = database.suggestionQueries.lastInsertedSuggestionId().executeAsOne()
                val owner = FieldOwner.Suggestion(suggestionId)
                database.fieldValueQueries.insertFields(owner, FieldContext.ORIGINAL, suggestion.originalFields)
                database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.SUGG_CHANGES, suggestion.changes)
            }
        }
    }

    override fun updateStatus(
        suggestionId: SuggestionId,
        status: SuggestionStatus,
    ) {
        database.suggestionQueries.updateSuggestionStatus(
            status = status.dbString,
            decided_at = System.currentTimeMillis(),
            id = suggestionId,
        )
    }

    override fun skip(suggestionId: SuggestionId) {
        database.suggestionQueries.skipSuggestion(
            skipped_at = System.currentTimeMillis(),
            id = suggestionId,
        )
    }

    override fun saveEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>) {
        replaceEditedChanges(suggestionId, editedChanges)
        database.suggestionQueries.touchSuggestion(suggestionId)
    }

    override fun clearEditedChanges(suggestionId: SuggestionId) {
        database.fieldValueQueries.deleteFieldsForSuggestionByContext(suggestionId, FieldContext.SUGG_EDITED.dbValue)
        database.suggestionQueries.touchSuggestion(suggestionId)
    }

    private fun mapWithFields(entities: List<DbSuggestion>): List<Suggestion> {
        if (entities.isEmpty()) return emptyList()

        val suggestionIds = entities.map { it.id }
        val noteIds = entities.map { it.note_id }

        // Get fields for all suggestions
        val allFields = database.fieldValueQueries.getFieldsForSuggestions(suggestionIds).executeAsList()
        val fieldsBySuggestionId = allFields.groupBy { it.suggestion_id }

        // Get model names from cached_note
        val modelNamesByNoteId = database.deckCacheQueries.getModelNamesForNotes(noteIds).executeAsList()
            .associate { it.id to it.model_name }

        return entities.map { entity ->
            val modelName = modelNamesByNoteId[entity.note_id] ?: ""
            entity.toDomain(fieldsBySuggestionId[entity.id] ?: emptyList(), modelName)
        }
    }

    private fun replaceEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>) {
        val owner = FieldOwner.Suggestion(suggestionId)
        database.fieldValueQueries.deleteFieldsForSuggestionByContext(suggestionId, FieldContext.SUGG_EDITED.dbValue)
        database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.SUGG_EDITED, editedChanges)
    }
}
