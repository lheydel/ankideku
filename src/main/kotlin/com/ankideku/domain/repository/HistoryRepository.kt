package com.ankideku.domain.repository

import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteId
import com.ankideku.domain.model.SessionId

/**
 * Repository for history entries.
 * Methods are blocking - caller is responsible for dispatcher management via TransactionService.
 */
interface HistoryRepository {
    fun saveEntry(entry: HistoryEntry)
    fun getForSession(sessionId: SessionId): List<HistoryEntry>
    fun getForNote(noteId: NoteId): List<HistoryEntry>
    fun getForDeck(deckId: DeckId): List<HistoryEntry>
    fun getAll(limit: Int = 100): List<HistoryEntry>
    fun getById(id: Long): HistoryEntry?
    fun search(query: String, limit: Int = 100): List<HistoryEntry>
}
