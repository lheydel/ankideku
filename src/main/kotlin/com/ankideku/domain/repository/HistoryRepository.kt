package com.ankideku.domain.repository

import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteId
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.DeckId

interface HistoryRepository {
    suspend fun saveEntry(entry: HistoryEntry)
    suspend fun getForSession(sessionId: SessionId): List<HistoryEntry>
    suspend fun getForNote(noteId: NoteId): List<HistoryEntry>
    suspend fun getForDeck(deckId: DeckId): List<HistoryEntry>
    suspend fun getAll(limit: Int = 100): List<HistoryEntry>
    suspend fun getById(id: Long): HistoryEntry?
}
