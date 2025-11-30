package com.ankideku.domain.repository

import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun getAllDecks(): Flow<List<Deck>>
    suspend fun getDeck(name: String): Deck?
    suspend fun saveDeck(deck: Deck)
    suspend fun deleteDeck(deckName: String)
    suspend fun getNotesForDeck(deckName: String): List<Note>
    suspend fun saveNotes(notes: List<Note>)
    suspend fun updateNoteFields(noteId: NoteId, fields: Map<String, String>)
}
