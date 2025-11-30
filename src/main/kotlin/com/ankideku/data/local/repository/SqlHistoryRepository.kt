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

    override fun saveEntry(entry: HistoryEntry) {
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
        val allFields = database.fieldValueQueries.getFieldsForHistoryEntries(historyIds).executeAsList()
        val fieldsByHistoryId = allFields.groupBy { it.history_id }

        return entities.map { it.toDomain(fieldsByHistoryId[it.id] ?: emptyList()) }
    }

    override fun getById(id: Long): HistoryEntry? {
        val entity = database.historyQueries.getHistoryById(id).executeAsOneOrNull() ?: return null
        val fieldValues = database.fieldValueQueries.getFieldsForHistory(entity.id).executeAsList()
        return entity.toDomain(fieldValues)
    }

    override fun search(query: String, limit: Int): List<HistoryEntry> {
        if (query.isBlank()) return emptyList()
        val entities = database.historyQueries.searchHistory(query, limit.toLong()).executeAsList()
        return mapWithFields(entities)
    }
}
