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

    /** Get the actual note count and token estimate from cached notes for a deck (including sub-decks) */
    fun getDeckStats(deckName: String): DeckStats

    /** Get note count and token estimate for a deck only (excluding sub-decks) */
    fun getDirectDeckStats(deckId: DeckId): DeckStats
}

data class DeckStats(val noteCount: Int, val tokenEstimate: Int)
