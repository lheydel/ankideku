package com.ankideku.domain.repository

import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.model.SuggestionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI-generated suggestions.
 * Methods are blocking - caller is responsible for dispatcher management.
 */
interface SuggestionRepository {
    fun getForSession(sessionId: SessionId): Flow<List<Suggestion>>
    fun getPendingForSession(sessionId: SessionId): Flow<List<Suggestion>>
    fun getById(suggestionId: SuggestionId): Suggestion?
    fun save(suggestion: Suggestion)
    fun saveAll(suggestions: List<Suggestion>)
    fun updateStatus(suggestionId: SuggestionId, status: SuggestionStatus)
    fun saveEditedChanges(suggestionId: SuggestionId, editedChanges: Map<String, String>)
    fun clearEditedChanges(suggestionId: SuggestionId)
}
