package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.FieldOwner
import com.ankideku.data.mapper.insertFields
import com.ankideku.data.mapper.insertFieldsFromMap
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.model.SuggestionStatus
import com.ankideku.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ankideku.data.local.database.Suggestion as DbSuggestion

class SqlSessionRepository(
    private val database: AnkiDekuDb,
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return database.sessionQueries.getAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getSession(id: SessionId): Session? {
        return database.sessionQueries.getSessionById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun createSession(session: Session): SessionId {
        val now = System.currentTimeMillis()
        database.sessionQueries.insertSession(
            deck_id = session.deckId,
            deck_name = session.deckName,
            prompt = session.prompt,
            state = session.state.dbString,
            created_at = now,
            updated_at = now,
        )
        return database.sessionQueries.lastInsertedSessionId().executeAsOne()
    }

    override fun updateState(sessionId: SessionId, state: SessionState) {
        val (stateMessage, exitCode) = when (state) {
            is SessionState.Failed -> state.message to state.exitCode?.toLong()
            else -> null to null
        }

        database.sessionQueries.updateSessionState(
            state = state.dbString,
            state_message = stateMessage,
            exit_code = exitCode,
            updated_at = System.currentTimeMillis(),
            id = sessionId,
        )
    }

    override fun updateProgress(sessionId: SessionId, progress: SessionProgress) {
        database.sessionQueries.updateSessionProgress(
            progress_processed_cards = progress.processedCards.toLong(),
            progress_total_cards = progress.totalCards.toLong(),
            progress_processed_batches = progress.processedBatches.toLong(),
            progress_total_batches = progress.totalBatches.toLong(),
            progress_suggestions_count = progress.suggestionsCount.toLong(),
            progress_input_tokens = progress.inputTokens.toLong(),
            progress_output_tokens = progress.outputTokens.toLong(),
            progress_failed_batches = progress.failedBatches.toLong(),
            updated_at = System.currentTimeMillis(),
            id = sessionId,
        )
    }

    override fun deleteSession(sessionId: SessionId) {
        database.sessionQueries.deleteSession(sessionId)
    }

    override fun getSuggestions(sessionId: SessionId): Flow<List<Suggestion>> {
        return database.suggestionQueries.getSuggestionsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> mapSuggestionsWithFields(entities) }
    }

    override fun getPendingSuggestions(sessionId: SessionId): Flow<List<Suggestion>> {
        return database.suggestionQueries.getPendingSuggestionsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> mapSuggestionsWithFields(entities) }
    }

    override fun getSuggestionById(suggestionId: SuggestionId): Suggestion? {
        val entity = database.suggestionQueries.getSuggestionById(suggestionId).executeAsOneOrNull()
            ?: return null
        val fields = database.fieldValueQueries.getFieldsForSuggestions(listOf(suggestionId)).executeAsList()
        return entity.toDomain(fields)
    }

    private fun mapSuggestionsWithFields(entities: List<DbSuggestion>): List<Suggestion> {
        if (entities.isEmpty()) return emptyList()

        val suggestionIds = entities.map { it.id }
        val allFields = database.fieldValueQueries.getFieldsForSuggestions(suggestionIds).executeAsList()
        val fieldsBySuggestionId = allFields.groupBy { it.suggestion_id }

        return entities.map { it.toDomain(fieldsBySuggestionId[it.id] ?: emptyList()) }
    }

    override fun saveSuggestion(suggestion: Suggestion) {
        saveSuggestions(listOf(suggestion))
    }

    override fun saveSuggestions(suggestions: List<Suggestion>) {
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
                database.fieldValueQueries.insertFields(owner, FieldContext.Original, suggestion.originalFields)
                database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.Changes, suggestion.changes)
            }
        }
    }

    override fun updateSuggestionStatus(
        suggestionId: SuggestionId,
        status: SuggestionStatus,
        editedChanges: Map<String, String>?,
    ) {
        database.suggestionQueries.updateSuggestionStatus(
            status = status.dbString,
            decided_at = System.currentTimeMillis(),
            id = suggestionId,
        )
        if (editedChanges != null) {
            replaceEditedChanges(suggestionId, editedChanges)
        }
    }

    override fun saveEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>) {
        replaceEditedChanges(suggestionId, editedChanges)
        database.suggestionQueries.touchSuggestion(suggestionId)
    }

    override fun clearEditedChanges(suggestionId: SuggestionId) {
        database.fieldValueQueries.deleteFieldsForSuggestionByContext(suggestionId, FieldContext.Edited.dbValue)
        database.suggestionQueries.touchSuggestion(suggestionId)
    }

    private fun replaceEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>) {
        val owner = FieldOwner.Suggestion(suggestionId)
        database.fieldValueQueries.deleteFieldsForSuggestionByContext(suggestionId, FieldContext.Edited.dbValue)
        database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.Edited, editedChanges)
    }
}
