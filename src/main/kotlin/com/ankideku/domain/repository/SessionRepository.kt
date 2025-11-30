package com.ankideku.domain.repository

import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.model.SuggestionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for sessions and suggestions.
 * Methods are blocking - caller is responsible for dispatcher management via TransactionService.
 */
interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    fun getSession(id: SessionId): Session?
    fun createSession(session: Session): SessionId
    fun updateState(sessionId: SessionId, state: SessionState)
    fun updateProgress(sessionId: SessionId, progress: SessionProgress)
    fun deleteSession(sessionId: SessionId)
    fun getSuggestions(sessionId: SessionId): Flow<List<Suggestion>>
    fun getPendingSuggestions(sessionId: SessionId): Flow<List<Suggestion>>
    fun getSuggestionById(suggestionId: SuggestionId): Suggestion?
    fun saveSuggestion(suggestion: Suggestion)
    fun saveSuggestions(suggestions: List<Suggestion>)
    fun updateSuggestionStatus(
        suggestionId: SuggestionId,
        status: SuggestionStatus,
        editedChanges: Map<String, String>? = null,
    )
    fun saveEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>)
    fun clearEditedChanges(suggestionId: SuggestionId)
}
