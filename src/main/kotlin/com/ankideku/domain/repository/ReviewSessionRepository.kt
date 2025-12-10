package com.ankideku.domain.repository

import com.ankideku.domain.model.ReviewContextConfig
import com.ankideku.domain.model.ReviewMemory
import com.ankideku.domain.model.ReviewMessage
import com.ankideku.domain.model.ReviewMessageId
import com.ankideku.domain.model.ReviewSession
import com.ankideku.domain.model.ReviewSessionId
import com.ankideku.domain.model.ReviewSuggestion
import com.ankideku.domain.model.ReviewSuggestionId
import com.ankideku.domain.model.ReviewSuggestionStatus
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SuggestionId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for review sessions (interactive AI chat).
 * Methods are blocking - caller is responsible for dispatcher management.
 */
interface ReviewSessionRepository {

    // --- Review Session ---

    fun getForSession(sessionId: SessionId): ReviewSession?
    fun getById(reviewSessionId: ReviewSessionId): ReviewSession?
    fun create(reviewSession: ReviewSession): ReviewSessionId
    fun updateActivity(reviewSessionId: ReviewSessionId, timestamp: Long)
    fun updateConfig(reviewSessionId: ReviewSessionId, config: ReviewContextConfig?, timestamp: Long)
    fun delete(reviewSessionId: ReviewSessionId)

    // --- Messages ---

    fun getMessages(reviewSessionId: ReviewSessionId): Flow<List<ReviewMessage>>
    fun getLastMessages(reviewSessionId: ReviewSessionId, limit: Int): List<ReviewMessage>
    fun getMessageById(messageId: ReviewMessageId): ReviewMessage?
    fun addMessage(message: ReviewMessage): ReviewMessageId
    fun clearMessages(reviewSessionId: ReviewSessionId)

    // --- Memory ---

    fun getMemory(reviewSessionId: ReviewSessionId): Map<String, String>
    fun getMemoryEntry(reviewSessionId: ReviewSessionId, key: String): String?
    fun saveMemory(reviewSessionId: ReviewSessionId, key: String, value: String)
    fun deleteMemory(reviewSessionId: ReviewSessionId, key: String)
    fun clearMemory(reviewSessionId: ReviewSessionId)
    fun countMemory(reviewSessionId: ReviewSessionId): Int

    // --- Review Suggestions ---

    fun getReviewSuggestions(reviewSessionId: ReviewSessionId): Flow<List<ReviewSuggestion>>
    fun getPendingReviewSuggestions(reviewSessionId: ReviewSessionId): List<ReviewSuggestion>
    fun getReviewSuggestionsForSuggestion(suggestionId: SuggestionId): List<ReviewSuggestion>
    fun getReviewSuggestionById(reviewSuggestionId: ReviewSuggestionId): ReviewSuggestion?
    fun createReviewSuggestion(reviewSuggestion: ReviewSuggestion): ReviewSuggestionId
    fun updateReviewSuggestionStatus(
        reviewSuggestionId: ReviewSuggestionId,
        status: ReviewSuggestionStatus,
        appliedAt: Long? = null,
    )
    fun dismissPendingForSuggestion(suggestionId: SuggestionId)
}
