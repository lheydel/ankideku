package com.ankideku.domain.model

import kotlinx.serialization.Serializable

/**
 * Review session for interactive AI chat after batch suggestion generation.
 * One review session per batch session.
 */
@Serializable
data class ReviewSession(
    val id: ReviewSessionId = 0,
    val sessionId: SessionId,
    val llmProvider: String,
    val startedAt: Long,
    val lastActivityAt: Long,
    val contextConfig: ReviewContextConfig? = null,
)

/**
 * Configuration for review session context.
 */
@Serializable
data class ReviewContextConfig(
    val fieldsToInclude: List<String>? = null,  // null = all fields
    val messageHistoryLimit: Int = 10,
    val customSystemPrompt: String? = null,
)

/**
 * A message in the review session conversation.
 */
@Serializable
data class ReviewMessage(
    val id: ReviewMessageId = 0,
    val reviewSessionId: ReviewSessionId,
    val role: ReviewMessageRole,
    val content: String,
    val actionCalls: List<ActionCall>? = null,  // For assistant messages
    val actionCallId: String? = null,  // For action_result messages
    val createdAt: Long,
)

@Serializable
enum class ReviewMessageRole(val dbString: String) {
    User("user"),
    Assistant("assistant"),
    ActionResult("action_result");

    companion object {
        fun fromDbString(value: String): ReviewMessageRole = entries.first { it.dbString == value }
    }
}

/**
 * An action call from the AI (structured output parsed from response).
 */
@Serializable
data class ActionCall(
    val id: String,
    val action: String,
    val params: Map<String, String>,  // Simplified for now, could be JsonObject
)

/**
 * A memory entry (persistent instruction) in the review session.
 */
@Serializable
data class ReviewMemory(
    val id: Long = 0,
    val reviewSessionId: ReviewSessionId,
    val key: String,
    val value: String,
    val createdAt: Long,
)

/**
 * An AI suggestion from the review chat.
 */
@Serializable
data class ReviewSuggestion(
    val id: ReviewSuggestionId = 0,
    val reviewSessionId: ReviewSessionId,
    val messageId: ReviewMessageId,
    val suggestionId: SuggestionId,  // The original suggestion being modified
    val proposedChanges: Map<String, String>,  // From field_value table
    val proposedReasoning: String?,
    val status: ReviewSuggestionStatus = ReviewSuggestionStatus.Pending,
    val appliedAt: Long? = null,
)

@Serializable
enum class ReviewSuggestionStatus(val dbString: String) {
    Pending("pending"),
    Applied("applied"),
    Dismissed("dismissed");

    companion object {
        fun fromDbString(value: String): ReviewSuggestionStatus = entries.first { it.dbString == value }
    }
}
