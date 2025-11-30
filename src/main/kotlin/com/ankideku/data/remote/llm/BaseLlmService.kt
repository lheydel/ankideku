package com.ankideku.data.remote.llm

import com.ankideku.domain.model.Note
import com.ankideku.util.TokenEstimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Base LLM Service
 * Abstract class implementing common logic for all LLM providers.
 * Subclasses only need to implement callLlm() and getHealth().
 */
abstract class BaseLlmService(
    private val promptBuilder: PromptBuilder = PromptBuilder(),
    private val responseParser: ResponseParser = ResponseParser(),
    private val tokenEstimator: TokenEstimator = TokenEstimator(),
    protected val maxRetries: Int = 2,
) : LlmService {

    /**
     * Analyze a batch of notes and return suggestions.
     * Handles prompt building, retry logic, parsing, and token counting.
     */
    override suspend fun analyzeBatch(
        notes: List<Note>,
        userPrompt: String,
        noteType: NoteTypeInfo,
    ): LlmResponse = withContext(Dispatchers.IO) {
        // Build prompt
        val fullPrompt = promptBuilder.buildPrompt(notes, userPrompt, noteType)
        val inputTokens = tokenEstimator.estimate(fullPrompt)
        val validNoteIds = notes.map { it.id }.toSet()

        // Try with retries
        var lastError: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val rawResponse = callLlm(fullPrompt)
                val suggestions = responseParser.parse(rawResponse, validNoteIds)
                val outputTokens = tokenEstimator.estimate(rawResponse)

                return@withContext LlmResponse(
                    suggestions = suggestions,
                    usage = TokenUsage(
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                    )
                )
            } catch (e: CancellationException) {
                throw e  // Don't retry on cancellation
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries) {
                    println("[${this@BaseLlmService::class.simpleName}] Error on attempt ${attempt + 1}/${maxRetries + 1}: ${e.message}")
                    println("[${this@BaseLlmService::class.simpleName}] Retrying...")
                }
            }
        }

        throw lastError ?: Exception("Failed to analyze batch after max retries")
    }

    /**
     * Provider-specific LLM call.
     * @param prompt The full prompt to send to the LLM
     * @return Raw response string from the LLM
     */
    protected abstract suspend fun callLlm(prompt: String): String
}
