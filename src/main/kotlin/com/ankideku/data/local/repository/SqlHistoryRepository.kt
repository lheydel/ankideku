package com.ankideku.data.local.repository

import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.database.History_entry
import com.ankideku.data.mapper.*
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteId
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.repository.HistoryRepository

class SqlHistoryRepository(
    private val database: AnkiDekuDb,
) : HistoryRepository {

    override fun saveEntry(entry: HistoryEntry) = database.transaction {
        database.historyQueries.insertHistoryEntry(
            session_id = entry.sessionId,
            note_id = entry.noteId,
            deck_id = entry.deckId,
            deck_name = entry.deckName,
            action = entry.action.dbString,
            reasoning = entry.reasoning,
            timestamp = entry.timestamp,
        )
        val historyId = database.historyQueries.lastInsertedHistoryId().executeAsOne()
        val owner = FieldOwner.History(historyId)

        database.fieldValueQueries.insertFields(owner, FieldContext.Original, entry.originalFields)
        database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.AiChanges, entry.aiChanges)
        entry.appliedChanges?.let {
            database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.Applied, it)
        }
        entry.userEdits?.let {
            database.fieldValueQueries.insertFieldsFromMap(owner, FieldContext.UserEdits, it)
        }
    }

    override fun getForSession(sessionId: SessionId): List<HistoryEntry> {
        val entities = database.historyQueries.getHistoryForSession(sessionId).executeAsList()
        return mapWithFields(entities)
    }

    override fun getForNote(noteId: NoteId): List<HistoryEntry> {
        val entities = database.historyQueries.getHistoryForNote(noteId).executeAsList()
        return mapWithFields(entities)
    }

    override fun getForDeck(deckId: DeckId): List<HistoryEntry> {
        val entities = database.historyQueries.getHistoryForDeck(deckId).executeAsList()
        return mapWithFields(entities)
    }

    override fun getAll(limit: Int): List<HistoryEntry> {
        val entities = database.historyQueries.getAllHistory(limit.toLong()).executeAsList()
        return mapWithFields(entities)
    }

    private fun mapWithFields(entities: List<History_entry>): List<HistoryEntry> {
        if (entities.isEmpty()) return emptyList()

        val historyIds = entities.map { it.id }
        val noteIds = entities.map { it.note_id }

        val allFields = database.fieldValueQueries.getFieldsForHistoryEntries(historyIds).executeAsList()
        val fieldsByHistoryId = allFields.groupBy { it.history_id }

        // Get model names from cached_note
        val modelNamesByNoteId = database.deckCacheQueries.getModelNamesForNotes(noteIds).executeAsList()
            .associate { it.id to it.model_name }

        return entities.map { entity ->
            val modelName = modelNamesByNoteId[entity.note_id] ?: ""
            entity.toDomain(fieldsByHistoryId[entity.id] ?: emptyList(), modelName)
        }
    }

    override fun getById(id: Long): HistoryEntry? {
        val entity = database.historyQueries.getHistoryById(id).executeAsOneOrNull() ?: return null
        val fieldValues = database.fieldValueQueries.getFieldsForHistory(entity.id).executeAsList()
        val modelName = database.deckCacheQueries.getModelNamesForNotes(listOf(entity.note_id)).executeAsList()
            .firstOrNull()?.model_name ?: ""
        return entity.toDomain(fieldValues, modelName)
    }

    override fun search(query: String, limit: Int): List<HistoryEntry> {
        if (query.isBlank()) return emptyList()
        val entities = database.historyQueries.searchHistory(query, limit.toLong()).executeAsList()
        return mapWithFields(entities)
    }
}
