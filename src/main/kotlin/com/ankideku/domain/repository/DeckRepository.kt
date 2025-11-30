package com.ankideku.domain.repository

import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for deck and note data.
 * Methods are blocking - caller is responsible for dispatcher management via TransactionService.
 */
interface DeckRepository {
    fun getAllDecks(): Flow<List<Deck>>
    fun getDeck(id: DeckId): Deck?
    fun saveDeck(deck: Deck)
    fun deleteDeck(deckId: DeckId)
    fun getNotesForDeck(deckId: DeckId): List<Note>
    fun saveNotes(notes: List<Note>)
    fun updateNoteFields(noteId: NoteId, fields: Map<String, String>)
}
