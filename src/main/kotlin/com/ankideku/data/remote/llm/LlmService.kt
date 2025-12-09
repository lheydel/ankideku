package com.ankideku.data.remote.llm

import com.ankideku.domain.model.Note

/**
 * Provider-agnostic interface for LLM interactions.
 * Implementations can use Claude Code CLI, API calls, local models, etc.
 */
interface LlmService {
    /**
     * Analyze a batch of notes and return suggestions.
     * @param notes Notes to analyze
     * @param userPrompt User's instruction for improvements
     * @param noteType Information about the note type (available fields)
     */
    suspend fun analyzeBatch(
        notes: List<Note>,
        userPrompt: String,
        noteType: NoteTypeInfo,
    ): LlmResponse

    /**
     * Check if the LLM provider is available and configured.
     */
    suspend fun getHealth(): LlmHealthStatus

    /**
     * Start a new conversation with the LLM.
     * @param systemPrompt System prompt defining behavior and available actions
     * @return A handle for sending messages to the conversation
     */
    suspend fun startConversation(systemPrompt: String): ConversationHandle
}
