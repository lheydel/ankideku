package com.ankideku.domain.usecase.review

import com.ankideku.data.remote.llm.ConversationHandle
import com.ankideku.data.remote.llm.ConversationResponse
import com.ankideku.data.remote.llm.LlmConfig
import com.ankideku.data.remote.llm.LlmServiceFactory
import com.ankideku.data.remote.llm.ParsedActionCall
import com.ankideku.domain.model.ActionCall
import com.ankideku.domain.model.ReviewContextConfig
import com.ankideku.domain.model.ReviewMessage
import com.ankideku.domain.model.ReviewMessageRole
import com.ankideku.domain.model.ReviewSession
import com.ankideku.domain.model.ReviewSessionId
import com.ankideku.domain.model.ReviewSuggestion
import com.ankideku.domain.model.ReviewSuggestionId
import com.ankideku.domain.model.ReviewSuggestionStatus
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.repository.ReviewSessionRepository
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.domain.tool.ToolResult
import com.ankideku.domain.tool.review.ReviewToolContext
import com.ankideku.domain.tool.review.ReviewToolRegistry
import com.ankideku.util.onIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * Orchestrates review sessions - interactive AI chat for reviewing suggestions.
 */
class ReviewSessionOrchestrator(
    private val reviewSessionRepository: ReviewSessionRepository,
    private val suggestionRepository: SuggestionRepository,
    private val settingsRepository: SettingsRepository,
) {
    private var activeConversation: ConversationHandle? = null
    private var activeReviewSessionId: ReviewSessionId? = null
    private var activeSessionId: SessionId? = null

    // Cached suggestions - observed from flow
    private var cachedSuggestions: List<Suggestion> = emptyList()
    private var suggestionsObserverScope: CoroutineScope? = null

    private fun startObservingSuggestions(sessionId: SessionId) {
        // Cancel any existing observer
        suggestionsObserverScope?.cancel()

        // Create new scope and start observing
        suggestionsObserverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope ->
            scope.launch {
                suggestionRepository.getForSession(sessionId).collect { suggestions ->
                    cachedSuggestions = suggestions
                }
            }
        }
    }

    private fun stopObservingSuggestions() {
        suggestionsObserverScope?.cancel()
        suggestionsObserverScope = null
        cachedSuggestions = emptyList()
    }

    /**
     * Start or resume a review session for a batch session.
     */
    suspend fun startReviewSession(sessionId: SessionId): ReviewSessionResult {
        println("[ReviewSessionOrchestrator] startReviewSession called for sessionId: $sessionId")
        // Check if review session already exists
        val existing = onIO { reviewSessionRepository.getForSession(sessionId) }
        println("[ReviewSessionOrchestrator] Existing review session: $existing")
        if (existing != null) {
            return resumeReviewSession(existing)
        }

        // Create new review session
        val provider = onIO { settingsRepository.getSettings().llmProvider }
        val now = System.currentTimeMillis()

        val reviewSession = ReviewSession(
            sessionId = sessionId,
            llmProvider = provider.name,
            startedAt = now,
            lastActivityAt = now,
            contextConfig = ReviewContextConfig(),
        )

        val reviewSessionId = onIO { reviewSessionRepository.create(reviewSession) }

        // Start conversation
        val systemPrompt = buildSystemPrompt(reviewSessionId)
        val llmService = LlmServiceFactory.getInstance(LlmConfig(provider))
        activeConversation = llmService.startConversation(systemPrompt)
        activeReviewSessionId = reviewSessionId
        activeSessionId = sessionId

        // Start observing suggestions
        startObservingSuggestions(sessionId)

        return ReviewSessionResult.Started(reviewSessionId)
    }

    /**
     * Resume an existing review session.
     */
    private suspend fun resumeReviewSession(reviewSession: ReviewSession): ReviewSessionResult {
        println("[ReviewSessionOrchestrator] resumeReviewSession called for: ${reviewSession.id}")
        val provider = onIO { settingsRepository.getSettings().llmProvider }
        println("[ReviewSessionOrchestrator] LLM provider: $provider")

        // Build system prompt with memory
        val systemPrompt = buildSystemPrompt(reviewSession.id)

        // Start new conversation (Claude CLI doesn't persist sessions)
        println("[ReviewSessionOrchestrator] Starting conversation...")
        val llmService = LlmServiceFactory.getInstance(LlmConfig(provider))
        activeConversation = llmService.startConversation(systemPrompt)
        println("[ReviewSessionOrchestrator] Conversation started: ${activeConversation?.id}")
        activeReviewSessionId = reviewSession.id
        activeSessionId = reviewSession.sessionId

        // Start observing suggestions
        startObservingSuggestions(reviewSession.sessionId)

        // Get last messages to include as context
        val config = reviewSession.contextConfig ?: ReviewContextConfig()
        val lastMessages = onIO { reviewSessionRepository.getLastMessages(reviewSession.id, config.messageHistoryLimit) }
        println("[ReviewSessionOrchestrator] Last messages count: ${lastMessages.size}")

        // Replay history to restore conversation context
        if (lastMessages.isNotEmpty()) {
            println("[ReviewSessionOrchestrator] Replaying history (${lastMessages.size} messages)...")
            val historyPrompt = buildHistoryPrompt(lastMessages)
            activeConversation?.sendMessage(historyPrompt)
            println("[ReviewSessionOrchestrator] History replay complete")
        }

        println("[ReviewSessionOrchestrator] Returning Resumed result")
        return ReviewSessionResult.Resumed(reviewSession.id, lastMessages.size)
    }

    /**
     * Send a message in the review session.
     */
    suspend fun sendMessage(
        content: String,
        currentSuggestionId: SuggestionId? = null,
        includeCurrentSuggestion: Boolean = false,
    ): ReviewMessageResult {
        val reviewSessionId = activeReviewSessionId
            ?: return ReviewMessageResult.Error("No active review session")
        val conversation = activeConversation
            ?: return ReviewMessageResult.Error("No active conversation")

        // Build message content
        val fullContent = if (includeCurrentSuggestion && currentSuggestionId != null) {
            val suggestion = onIO { suggestionRepository.getById(currentSuggestionId) }
            if (suggestion != null) {
                buildMessageWithSuggestionContext(content, suggestion)
            } else {
                content
            }
        } else {
            content
        }

        // Save user message
        val userMessage = ReviewMessage(
            reviewSessionId = reviewSessionId,
            role = ReviewMessageRole.User,
            content = fullContent,
            createdAt = System.currentTimeMillis(),
        )
        val userMessageId = onIO { reviewSessionRepository.addMessage(userMessage) }

        // Send to LLM
        val response = conversation.sendMessage(fullContent)

        // Process response (may have tool calls)
        val processedResponse = processResponse(reviewSessionId, currentSuggestionId, response)

        // Update activity timestamp
        onIO { reviewSessionRepository.updateActivity(reviewSessionId, System.currentTimeMillis()) }

        return ReviewMessageResult.Success(
            userMessageId = userMessageId,
            assistantMessage = processedResponse.assistantMessage,
            reviewSuggestions = processedResponse.reviewSuggestions,
        )
    }

    /**
     * Process LLM response, execute tool calls, save messages.
     */
    private suspend fun processResponse(
        reviewSessionId: ReviewSessionId,
        currentSuggestionId: SuggestionId?,
        response: ConversationResponse,
    ): ProcessedResponse {
        val reviewSuggestions = mutableListOf<ReviewSuggestion>()

        // Save assistant message
        val actionCalls = response.actionCalls.map { parsed ->
            ActionCall(
                id = UUID.randomUUID().toString(),
                action = parsed.action,
                params = parsed.params.toStringMap(),
            )
        }

        val assistantMessage = ReviewMessage(
            reviewSessionId = reviewSessionId,
            role = ReviewMessageRole.Assistant,
            content = response.content ?: "",
            actionCalls = actionCalls.ifEmpty { null },
            createdAt = System.currentTimeMillis(),
        )
        val assistantMessageId = onIO { reviewSessionRepository.addMessage(assistantMessage) }

        // Execute tool calls
        if (response.actionCalls.isNotEmpty()) {
            val memory = onIO { reviewSessionRepository.getMemory(reviewSessionId) }

            val sessionId = activeSessionId
                ?: throw IllegalStateException("No active session")

            val context = ReviewToolContext(
                reviewSessionId = reviewSessionId,
                sessionId = sessionId,
                currentSuggestionId = currentSuggestionId,
                allSuggestions = cachedSuggestions,
                memory = memory,
                onSaveMemory = { key, value ->
                    onIO { reviewSessionRepository.saveMemory(reviewSessionId, key, value) }
                },
                onDeleteMemory = { key ->
                    onIO { reviewSessionRepository.deleteMemory(reviewSessionId, key) }
                },
                onCreateReviewSuggestion = { suggId, changes, reasoning ->
                    val reviewSuggestion = ReviewSuggestion(
                        reviewSessionId = reviewSessionId,
                        messageId = assistantMessageId,
                        suggestionId = suggId,
                        proposedChanges = changes,
                        proposedReasoning = reasoning,
                    )
                    val id = onIO { reviewSessionRepository.createReviewSuggestion(reviewSuggestion) }
                    reviewSuggestions.add(reviewSuggestion.copy(id = id))
                },
            )

            for (actionCall in response.actionCalls) {
                val result = executeTool(actionCall, context)

                // Save tool result message
                val resultMessage = ReviewMessage(
                    reviewSessionId = reviewSessionId,
                    role = ReviewMessageRole.ActionResult,
                    content = when (result) {
                        is ToolResult.Success -> result.message
                        is ToolResult.Error -> "Error: ${result.message}"
                    },
                    actionCallId = actionCalls.find { it.action == actionCall.action }?.id,
                    createdAt = System.currentTimeMillis(),
                )
                onIO { reviewSessionRepository.addMessage(resultMessage) }
            }
        }

        return ProcessedResponse(
            assistantMessage = assistantMessage.copy(id = assistantMessageId),
            reviewSuggestions = reviewSuggestions,
        )
    }

    private suspend fun executeTool(actionCall: ParsedActionCall, context: ReviewToolContext): ToolResult {
        val tool = ReviewToolRegistry[actionCall.action]
            ?: return ToolResult.Error("Unknown action: ${actionCall.action}")

        return try {
            tool.execute(actionCall.params, context)
        } catch (e: Exception) {
            ToolResult.Error("Tool execution failed: ${e.message}")
        }
    }

    /**
     * Apply a review suggestion (stages it for user confirmation).
     */
    suspend fun applyReviewSuggestion(reviewSuggestionId: ReviewSuggestionId): ApplyResult {
        val reviewSuggestion = onIO { reviewSessionRepository.getReviewSuggestionById(reviewSuggestionId) }
            ?: return ApplyResult.Error("Review suggestion not found")

        // Update status
        onIO {
            reviewSessionRepository.updateReviewSuggestionStatus(
                reviewSuggestionId = reviewSuggestionId,
                status = ReviewSuggestionStatus.Applied,
                appliedAt = System.currentTimeMillis(),
            )
        }

        return ApplyResult.Success(reviewSuggestion.suggestionId, reviewSuggestion.proposedChanges)
    }

    /**
     * Dismiss a review suggestion.
     */
    suspend fun dismissReviewSuggestion(reviewSuggestionId: ReviewSuggestionId) {
        onIO {
            reviewSessionRepository.updateReviewSuggestionStatus(
                reviewSuggestionId = reviewSuggestionId,
                status = ReviewSuggestionStatus.Dismissed,
            )
        }
    }

    /**
     * Reset the conversation (clears messages but keeps memory).
     */
    suspend fun resetConversation(): ReviewSessionResult {
        val reviewSessionId = activeReviewSessionId
            ?: return ReviewSessionResult.Error("No active review session")
        val conversation = activeConversation
            ?: return ReviewSessionResult.Error("No active conversation")

        // Reset conversation context (sends /clear to Claude CLI)
        conversation.reset()

        // Clear messages in database
        onIO { reviewSessionRepository.clearMessages(reviewSessionId) }

        // Update activity
        onIO { reviewSessionRepository.updateActivity(reviewSessionId, System.currentTimeMillis()) }

        return ReviewSessionResult.Reset(reviewSessionId)
    }

    /**
     * Update review session configuration.
     */
    suspend fun updateConfig(config: ReviewContextConfig) {
        val reviewSessionId = activeReviewSessionId ?: return
        onIO {
            reviewSessionRepository.updateConfig(reviewSessionId, config, System.currentTimeMillis())
        }
    }

    /**
     * End the review session.
     */
    suspend fun endReviewSession() {
        stopObservingSuggestions()
        activeConversation?.close()
        activeConversation = null
        activeReviewSessionId = null
        activeSessionId = null
    }

    /**
     * Get memory entries for display.
     */
    suspend fun getMemory(): Map<String, String> {
        val reviewSessionId = activeReviewSessionId ?: return emptyMap()
        return onIO { reviewSessionRepository.getMemory(reviewSessionId) }
    }

    /**
     * Delete a memory entry.
     */
    suspend fun deleteMemory(key: String) {
        val reviewSessionId = activeReviewSessionId ?: return
        onIO { reviewSessionRepository.deleteMemory(reviewSessionId, key) }
    }

    /**
     * Get current context config.
     */
    suspend fun getContextConfig(): ReviewContextConfig? {
        val reviewSessionId = activeReviewSessionId ?: return null
        return onIO { reviewSessionRepository.getById(reviewSessionId)?.contextConfig }
    }

    /**
     * Get pending review suggestions.
     */
    suspend fun getPendingReviewSuggestions(): List<ReviewSuggestion> {
        val reviewSessionId = activeReviewSessionId ?: return emptyList()
        return onIO { reviewSessionRepository.getPendingReviewSuggestions(reviewSessionId) }
    }

    // --- Helpers ---

    private suspend fun buildSystemPrompt(reviewSessionId: ReviewSessionId): String = buildString {
        appendLine("You are an AI assistant helping review and improve Anki flashcard suggestions.")
        appendLine("The goal of an Anki flashcard is to be the best educational tool possible.")
        appendLine()
        appendLine("Your role is to:")
        appendLine("- Review flashcard content and AI-generated suggestions when the user asks")
        appendLine("- Suggest improvements using the make_suggestion action")
        appendLine("- Remember important instructions using the memory action")
        appendLine()
        appendLine("IMPORTANT: When reviewing AI-generated suggestions that include reasoning, never trust the reasoning blindly.")
        appendLine("Be critical and objective - the original AI may have hallucinated or made errors.")
        appendLine("Always verify that suggestions actually improve the educational value of the flashcard and match the instructions of the user.")
        appendLine()

        // Include tools documentation
        appendLine(ReviewToolRegistry.generateToolsPrompt())

        // Include memory instructions
        appendLine(MEMORY_INSTRUCTIONS)

        // Include current memory entries
        val memory = onIO { reviewSessionRepository.getMemory(reviewSessionId) }
        if (memory.isNotEmpty()) {
            appendLine()
            appendLine("Your stored instructions:")
            for ((key, value) in memory) {
                appendLine("- $key: $value")
            }
        }
    }

    private fun buildHistoryPrompt(messages: List<ReviewMessage>): String = buildString {
        appendLine("Here is our previous conversation for context:")
        appendLine()
        for (message in messages) {
            val roleLabel = when (message.role) {
                ReviewMessageRole.User -> "User"
                ReviewMessageRole.Assistant -> "Assistant"
                ReviewMessageRole.ActionResult -> "Action Result"
            }
            appendLine("$roleLabel: ${message.content}")
        }
        appendLine()
        appendLine("Continue from where we left off.")
    }

    private fun buildMessageWithSuggestionContext(userMessage: String, suggestion: Suggestion): String = buildString {
        appendLine("I'm looking at suggestion #${suggestion.id} for note ${suggestion.noteId}.")
        appendLine()
        appendLine("Original fields:")
        for ((name, field) in suggestion.originalFields) {
            appendLine("- $name: ${field.value}")
        }
        appendLine()
        appendLine("Suggested changes:")
        for ((name, value) in suggestion.changes) {
            appendLine("- $name: $value")
        }
        appendLine()
        appendLine("AI reasoning: ${suggestion.reasoning}")
        appendLine()
        appendLine("User message: $userMessage")
    }

    companion object {
        private val MEMORY_INSTRUCTIONS = """
            ## Memory Tool

            You have access to a Memory tool for storing persistent instructions.

            WHEN TO USE IT PROACTIVELY:
            - When the user gives you a rule or preference (e.g., "always do X", "never do Y", "I prefer Z")
            - When the user corrects your behavior or gives feedback about how you should work
            - When the user describes patterns or conventions to follow
            - When the user asks you to "remember" or "memorize" something
            - IMMEDIATELY after receiving such instructions - don't wait to be reminded

            BE PROACTIVE: If you catch yourself thinking "I should remember this for next time", use the memory tool RIGHT AWAY in that same response.

            HOW TO USE IT:
            - Use descriptive keys that indicate the context (e.g., "kanji_rules", "example_quality")
            - Update existing keys if the user refines an instruction
            - Delete keys if the user says to stop doing something

            WHY IT MATTERS:
            - Memory persists across conversation resets
            - When the conversation gets long and context drifts, the user may reset the chat
            - Your stored instructions will be restored, so you won't lose important context
            - Using memory in the SAME response ensures nothing is forgotten
        """.trimIndent()
    }
}

private data class ProcessedResponse(
    val assistantMessage: ReviewMessage,
    val reviewSuggestions: List<ReviewSuggestion>,
)

private fun JsonObject.toStringMap(): Map<String, String> =
    entries.associate { (k, v) -> k to v.toString().trim('"') }

// --- Result Types ---

sealed class ReviewSessionResult {
    data class Started(val reviewSessionId: ReviewSessionId) : ReviewSessionResult()
    data class Resumed(val reviewSessionId: ReviewSessionId, val messageCount: Int) : ReviewSessionResult()
    data class Reset(val reviewSessionId: ReviewSessionId) : ReviewSessionResult()
    data class Error(val message: String) : ReviewSessionResult()
}

sealed class ReviewMessageResult {
    data class Success(
        val userMessageId: Long,
        val assistantMessage: ReviewMessage,
        val reviewSuggestions: List<ReviewSuggestion>,
    ) : ReviewMessageResult()
    data class Error(val message: String) : ReviewMessageResult()
}

sealed class ApplyResult {
    data class Success(val suggestionId: SuggestionId, val changes: Map<String, String>) : ApplyResult()
    data class Error(val message: String) : ApplyResult()
}
