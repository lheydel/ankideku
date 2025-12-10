package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.FieldOwner
import com.ankideku.data.mapper.insertFieldsFromMap
import com.ankideku.data.mapper.toDomain
import com.ankideku.data.mapper.toStringMap
import com.ankideku.domain.model.ReviewContextConfig
import com.ankideku.domain.model.ReviewMessage
import com.ankideku.domain.model.ReviewMessageId
import com.ankideku.domain.model.ReviewSession
import com.ankideku.domain.model.ReviewSessionId
import com.ankideku.domain.model.ReviewSuggestion
import com.ankideku.domain.model.ReviewSuggestionId
import com.ankideku.domain.model.ReviewSuggestionStatus
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.repository.ReviewSessionRepository
import com.ankideku.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlReviewSessionRepository(
    private val database: AnkiDekuDb,
) : ReviewSessionRepository {

    private val reviewSessionQueries get() = database.reviewSessionQueries
    private val fieldValueQueries get() = database.fieldValueQueries

    // --- Review Session ---

    override fun getForSession(sessionId: SessionId): ReviewSession? {
        return reviewSessionQueries.getReviewSessionForSession(sessionId)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun getById(reviewSessionId: ReviewSessionId): ReviewSession? {
        return reviewSessionQueries.getReviewSessionById(reviewSessionId)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun create(reviewSession: ReviewSession): ReviewSessionId {
        var id: ReviewSessionId = 0
        database.transaction {
            reviewSessionQueries.insertReviewSession(
                session_id = reviewSession.sessionId,
                llm_provider = reviewSession.llmProvider,
                started_at = reviewSession.startedAt,
                last_activity_at = reviewSession.lastActivityAt,
                context_config = reviewSession.contextConfig?.toJson(),
            )
            id = reviewSessionQueries.lastInsertedReviewSessionId().executeAsOne()
        }
        return id
    }

    override fun updateActivity(reviewSessionId: ReviewSessionId, timestamp: Long) {
        reviewSessionQueries.updateReviewSessionActivity(timestamp, reviewSessionId)
    }

    override fun updateConfig(reviewSessionId: ReviewSessionId, config: ReviewContextConfig?, timestamp: Long) {
        reviewSessionQueries.updateReviewSessionConfig(
            context_config = config?.toJson(),
            last_activity_at = timestamp,
            id = reviewSessionId,
        )
    }

    override fun delete(reviewSessionId: ReviewSessionId) {
        reviewSessionQueries.deleteReviewSession(reviewSessionId)
    }

    // --- Messages ---

    override fun getMessages(reviewSessionId: ReviewSessionId): Flow<List<ReviewMessage>> {
        return reviewSessionQueries.getMessagesForReviewSession(reviewSessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getLastMessages(reviewSessionId: ReviewSessionId, limit: Int): List<ReviewMessage> {
        return reviewSessionQueries.getLastNMessagesForReviewSession(reviewSessionId, limit.toLong())
            .executeAsList()
            .map { it.toDomain() }
            .reversed() // Query returns DESC, we want ASC
    }

    override fun getMessageById(messageId: ReviewMessageId): ReviewMessage? {
        return reviewSessionQueries.getMessageById(messageId)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun addMessage(message: ReviewMessage): ReviewMessageId {
        var id: ReviewMessageId = 0
        database.transaction {
            reviewSessionQueries.insertReviewMessage(
                review_session_id = message.reviewSessionId,
                role = message.role.dbString,
                content = message.content,
                action_calls = message.actionCalls?.toJson(),
                action_call_id = message.actionCallId,
                created_at = message.createdAt,
            )
            id = reviewSessionQueries.lastInsertedReviewMessageId().executeAsOne()
        }
        return id
    }

    override fun clearMessages(reviewSessionId: ReviewSessionId) {
        reviewSessionQueries.deleteMessagesForReviewSession(reviewSessionId)
    }

    // --- Memory ---

    override fun getMemory(reviewSessionId: ReviewSessionId): Map<String, String> {
        return reviewSessionQueries.getMemoryForReviewSession(reviewSessionId)
            .executeAsList()
            .associate { it.key to it.value_ }
    }

    override fun getMemoryEntry(reviewSessionId: ReviewSessionId, key: String): String? {
        return reviewSessionQueries.getMemoryByKey(reviewSessionId, key)
            .executeAsOneOrNull()
            ?.value_
    }

    override fun saveMemory(reviewSessionId: ReviewSessionId, key: String, value: String) {
        reviewSessionQueries.upsertMemory(
            review_session_id = reviewSessionId,
            key = key,
            value_ = value,
            created_at = System.currentTimeMillis(),
        )
    }

    override fun deleteMemory(reviewSessionId: ReviewSessionId, key: String) {
        reviewSessionQueries.deleteMemoryByKey(reviewSessionId, key)
    }

    override fun clearMemory(reviewSessionId: ReviewSessionId) {
        reviewSessionQueries.deleteAllMemoryForReviewSession(reviewSessionId)
    }

    override fun countMemory(reviewSessionId: ReviewSessionId): Int {
        return reviewSessionQueries.countMemoryForReviewSession(reviewSessionId)
            .executeAsOne()
            .toInt()
    }

    // --- Review Suggestions ---

    override fun getReviewSuggestions(reviewSessionId: ReviewSessionId): Flow<List<ReviewSuggestion>> {
        return reviewSessionQueries.getReviewSuggestionsForSession(reviewSessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                val ids = entities.map { it.id }
                val fieldsByReviewSuggestion = loadFieldsForReviewSuggestions(ids)
                entities.map { entity ->
                    entity.toDomain(fieldsByReviewSuggestion[entity.id] ?: emptyMap())
                }
            }
    }

    override fun getPendingReviewSuggestions(reviewSessionId: ReviewSessionId): List<ReviewSuggestion> {
        val entities = reviewSessionQueries.getPendingReviewSuggestionsForSession(reviewSessionId).executeAsList()
        val ids = entities.map { it.id }
        val fieldsByReviewSuggestion = loadFieldsForReviewSuggestions(ids)
        return entities.map { entity ->
            entity.toDomain(fieldsByReviewSuggestion[entity.id] ?: emptyMap())
        }
    }

    override fun getReviewSuggestionsForSuggestion(suggestionId: SuggestionId): List<ReviewSuggestion> {
        val entities = reviewSessionQueries.getReviewSuggestionsForSuggestion(suggestionId).executeAsList()
        val ids = entities.map { it.id }
        val fieldsByReviewSuggestion = loadFieldsForReviewSuggestions(ids)
        return entities.map { entity ->
            entity.toDomain(fieldsByReviewSuggestion[entity.id] ?: emptyMap())
        }
    }

    override fun getReviewSuggestionById(reviewSuggestionId: ReviewSuggestionId): ReviewSuggestion? {
        val entity = reviewSessionQueries.getReviewSuggestionById(reviewSuggestionId).executeAsOneOrNull()
            ?: return null
        val fields = fieldValueQueries.getFieldsForReviewSuggestionByContext(
            reviewSuggestionId,
            FieldContext.REVIEW_SUGG_CHANGES.dbValue,
        ).executeAsList().toStringMap()
        return entity.toDomain(fields)
    }

    override fun createReviewSuggestion(reviewSuggestion: ReviewSuggestion): ReviewSuggestionId {
        var id: ReviewSuggestionId = 0
        database.transaction {
            reviewSessionQueries.insertReviewSuggestion(
                review_session_id = reviewSuggestion.reviewSessionId,
                message_id = reviewSuggestion.messageId,
                suggestion_id = reviewSuggestion.suggestionId,
                proposed_reasoning = reviewSuggestion.proposedReasoning,
                status = reviewSuggestion.status.dbString,
            )
            id = reviewSessionQueries.lastInsertedReviewSuggestionId().executeAsOne()

            // Store proposed changes in field_value
            fieldValueQueries.insertFieldsFromMap(
                owner = FieldOwner.ReviewSuggestion(id),
                context = FieldContext.REVIEW_SUGG_CHANGES,
                fields = reviewSuggestion.proposedChanges,
            )
        }
        return id
    }

    override fun updateReviewSuggestionStatus(
        reviewSuggestionId: ReviewSuggestionId,
        status: ReviewSuggestionStatus,
        appliedAt: Long?,
    ) {
        reviewSessionQueries.updateReviewSuggestionStatus(
            status = status.dbString,
            applied_at = appliedAt,
            id = reviewSuggestionId,
        )
    }

    override fun dismissPendingForSuggestion(suggestionId: SuggestionId) {
        reviewSessionQueries.dismissPendingReviewSuggestionsForSuggestion(suggestionId)
    }

    // --- Helpers ---

    private fun loadFieldsForReviewSuggestions(ids: List<ReviewSuggestionId>): Map<ReviewSuggestionId, Map<String, String>> {
        if (ids.isEmpty()) return emptyMap()

        return fieldValueQueries.getFieldsForReviewSuggestions(ids)
            .executeAsList()
            .filter { it.context == FieldContext.REVIEW_SUGG_CHANGES.dbValue }
            .groupBy { it.review_suggestion_id!! }
            .mapValues { (_, fields) -> fields.toStringMap() }
    }
}
