package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.insertNoteFields
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteId
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.DeckStats
import com.ankideku.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDeckRepository(
    private val database: AnkiDekuDb,
) : DeckRepository {

    override fun getAllDecks(): Flow<List<Deck>> {
        return database.deckCacheQueries.getAllDecks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getDeck(id: DeckId): Deck? {
        return database.deckCacheQueries.getDeckById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun saveDeck(deck: Deck) {
        val now = System.currentTimeMillis()
        database.deckCacheQueries.upsertDeck(
            anki_id = deck.id,
            name = deck.name,
            parent_id = deck.parentId,
            last_sync_timestamp = deck.lastSyncTimestamp,
            note_count = deck.noteCount.toLong(),
            token_estimate = deck.tokenEstimate.toLong(),
            created_at = now,
            updated_at = now,
        )
    }

    override fun deleteDeck(deckId: DeckId) {
        database.deckCacheQueries.deleteDeck(deckId)
    }

    override fun getNotesForDeck(deckId: DeckId): List<Note> {
        // Look up deck name for hierarchical query (includes sub-decks)
        val deck = database.deckCacheQueries.getDeckById(deckId).executeAsOneOrNull()
            ?: return emptyList()
        val deckName = deck.name

        val noteEntities = database.deckCacheQueries.getNotesForDeck(deckName, deckName).executeAsList()
        if (noteEntities.isEmpty()) return emptyList()

        val noteIds = noteEntities.map { it.id }
        val allFields = database.fieldValueQueries.getFieldsForNotes(noteIds, FieldContext.Fields.dbValue).executeAsList()
        val fieldsByNoteId = allFields.groupBy { it.note_id }

        return noteEntities.map { it.toDomain(fieldsByNoteId[it.id] ?: emptyList()) }
    }

    override fun saveNotes(notes: List<Note>) {
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

    override fun updateNoteFields(noteId: NoteId, fields: Map<String, String>) {
        fields.forEach { (name, value) ->
            database.fieldValueQueries.updateNoteField(
                field_value = value,
                note_id = noteId,
                context = FieldContext.Fields.dbValue,
                field_name = name,
            )
        }
    }

    override fun getDeckStats(deckName: String): DeckStats {
        val noteCount = database.deckCacheQueries
            .countNotesForDeckByName(deckName, deckName)
            .executeAsOne()
            .toInt()
        val tokenEstimate = database.deckCacheQueries
            .sumTokensForDeckByName(deckName, deckName)
            .executeAsOne()
            .toInt()
        return DeckStats(noteCount, tokenEstimate)
    }

    override fun getDirectDeckStats(deckId: DeckId): DeckStats {
        val noteCount = database.deckCacheQueries
            .countNotesForDeckDirect(deckId)
            .executeAsOne()
            .toInt()
        val tokenEstimate = database.deckCacheQueries
            .sumTokensForDeckDirect(deckId)
            .executeAsOne()
            .toInt()
        return DeckStats(noteCount, tokenEstimate)
    }
}
