package com.ankideku.domain.repository

import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.model.SuggestionStatus
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    suspend fun getSession(id: SessionId): Session?
    suspend fun createSession(session: Session): SessionId  // Returns auto-generated ID
    suspend fun updateState(sessionId: SessionId, state: SessionState)
    suspend fun updateProgress(sessionId: SessionId, progress: SessionProgress)
    suspend fun deleteSession(sessionId: SessionId)
    fun getSuggestions(sessionId: SessionId): Flow<List<Suggestion>>
    fun getPendingSuggestions(sessionId: SessionId): Flow<List<Suggestion>>
    suspend fun saveSuggestion(suggestion: Suggestion)
    suspend fun saveSuggestions(suggestions: List<Suggestion>)
    suspend fun updateSuggestionStatus(
        suggestionId: SuggestionId,
        status: SuggestionStatus,
        editedChanges: Map<String, String>? = null,
    )
    suspend fun saveEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>)
    suspend fun clearEditedChanges(suggestionId: SuggestionId)
}
