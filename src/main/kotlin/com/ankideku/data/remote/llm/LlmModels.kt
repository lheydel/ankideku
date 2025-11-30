package com.ankideku.data.remote.llm

import kotlinx.serialization.Serializable

/**
 * JSON field names for LLM response schema.
 * Used across PromptBuilder, ResponseParser, and MockLlmService.
 */
object LlmJsonFields {
    const val SUGGESTIONS = "suggestions"
    const val NOTE_ID = "noteId"
    const val CHANGES = "changes"
    const val REASONING = "reasoning"
}

/**
 * Suggestion returned from LLM
 */
@Serializable
data class LlmSuggestion(
    val noteId: Long,
    val changes: Map<String, String>,
    val reasoning: String,
)

/**
 * Response from LLM batch analysis
 */
@Serializable
data class LlmResponse(
    val suggestions: List<LlmSuggestion>,
    val usage: TokenUsage = TokenUsage(),
)

/**
 * Token usage for a batch
 */
@Serializable
data class TokenUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

/**
 * Health status of LLM provider
 */
data class LlmHealthStatus(
    val available: Boolean,
    val error: String? = null,
    val info: String? = null,
)

/**
 * Note type info for prompt building
 */
data class NoteTypeInfo(
    val name: String,
    val fields: List<String>,
)

/**
 * Supported LLM providers
 */
@Serializable
enum class LlmProvider {
    CLAUDE_CODE,
    MOCK,
}

/**
 * LLM configuration
 */
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.MOCK,
)

val DEFAULT_LLM_CONFIG = LlmConfig()
