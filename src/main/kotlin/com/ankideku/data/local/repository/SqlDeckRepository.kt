package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.database.withTransaction
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.insertNoteFields
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteId
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDeckRepository(
    private val database: AnkiDekuDb,
) : DeckRepository {

    override fun getAllDecks(): Flow<List<Deck>> {
        return database.deckCacheQueries.getAllDecks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getDeck(name: String): Deck? = withContext(Dispatchers.IO) {
        database.deckCacheQueries.getDeckByName(name)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun saveDeck(deck: Deck) = database.withTransaction {
        val now = System.currentTimeMillis()
        database.deckCacheQueries.upsertDeck(
            anki_id = deck.id,
            name = deck.name,
            last_sync_timestamp = deck.lastSyncTimestamp,
            created_at = now,
            updated_at = now,
        )
    }

    override suspend fun deleteDeck(deckName: String) = database.withTransaction {
        database.deckCacheQueries.deleteDeckByName(deckName)
    }

    override suspend fun getNotesForDeck(deckName: String): List<Note> = withContext(Dispatchers.IO) {
        val noteEntities = database.deckCacheQueries.getNotesForDeck(deckName, deckName).executeAsList()
        if (noteEntities.isEmpty()) return@withContext emptyList()

        val noteIds = noteEntities.map { it.id }
        val allFields = database.fieldValueQueries.getFieldsForNotes(noteIds, FieldContext.Fields.dbValue).executeAsList()
        val fieldsByNoteId = allFields.groupBy { it.note_id }

        noteEntities.map { it.toDomain(fieldsByNoteId[it.id] ?: emptyList()) }
    }

    override suspend fun saveNotes(notes: List<Note>) = database.withTransaction {
        val now = System.currentTimeMillis()
        for (note in notes) {
            database.deckCacheQueries.upsertNote(
                id = note.id,
                deck_id = note.deckId,
                deck_name = note.deckName,
                model_name = note.modelName,
                tags = note.tags.toJson(),
                mod = note.mod,
                estimated_tokens = note.estimatedTokens?.toLong(),
                created_at = now,
                updated_at = now,
            )

            database.fieldValueQueries.deleteFieldsForNote(note.id)
            database.fieldValueQueries.insertNoteFields(note.id, note.fields)
        }
    }

    override suspend fun updateNoteFields(noteId: NoteId, fields: Map<String, String>) = database.withTransaction {
        fields.forEach { (name, value) ->
            database.fieldValueQueries.updateNoteField(
                field_value = value,
                note_id = noteId,
                context = FieldContext.Fields.dbValue,
                field_name = name,
            )
        }
    }
}
