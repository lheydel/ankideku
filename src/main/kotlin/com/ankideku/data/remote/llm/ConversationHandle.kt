package com.ankideku.data.remote.llm

import kotlinx.serialization.json.JsonObject

/**
 * Handle for an ongoing conversation with an LLM.
 * Represents a persistent connection that maintains conversation context.
 */
interface ConversationHandle {
    /** Unique identifier for this conversation */
    val id: String

    /**
     * Send a message and receive a response.
     * The conversation context is maintained internally.
     * @param content The message content to send
     * @return Response from the LLM including any action calls
     */
    suspend fun sendMessage(content: String): ConversationResponse

    /**
     * Close the conversation and release resources.
     * After calling this, the handle should not be used.
     */
    suspend fun close()
}

/**
 * Response from a conversation message.
 */
data class ConversationResponse(
    /** Text content of the response (may be null if only actions) */
    val content: String?,
    /** Parsed action calls from the response */
    val actionCalls: List<ParsedActionCall>,
    /** Token usage for this message exchange */
    val usage: TokenUsage,
)

/**
 * A parsed action call from the LLM response.
 */
data class ParsedActionCall(
    /** Name of the action (e.g., "make_suggestion", "memory") */
    val action: String,
    /** Raw JSON parameters for the action */
    val params: JsonObject,
)
