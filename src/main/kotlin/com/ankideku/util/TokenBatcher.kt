package com.ankideku.util

import com.ankideku.data.remote.llm.NoteTypeInfo
import com.ankideku.data.remote.llm.PromptBuilder
import com.ankideku.domain.model.Note

/**
 * Creates batches of notes based on token count rather than fixed size.
 * Ensures each batch fits within the token limit for LLM context windows.
 */
class TokenBatcher(
    private val maxInputTokens: Int = DEFAULT_MAX_INPUT_TOKENS,
) {
    companion object {
        const val DEFAULT_MAX_INPUT_TOKENS = 8_000
    }

    /**
     * Create token-based batches of notes.
     * @param notes Notes to batch
     * @param userPrompt User's instruction for the AI
     * @param noteType Information about the note type
     * @return List of batches, where each batch fits within the token limit
     */
    fun createBatches(
        notes: List<Note>,
        userPrompt: String,
        noteType: NoteTypeInfo,
    ): List<List<Note>> {
        if (notes.isEmpty()) return emptyList()

        val batches = mutableListOf<List<Note>>()
        val baseTokens = calculateBasePromptTokens(userPrompt, noteType)
        val availableTokens = maxInputTokens - baseTokens

        if (availableTokens <= 0) {
            println("[TokenBatcher] Base prompt exceeds token limit, using single-note batches")
            return notes.map { listOf(it) }
        }

        var currentBatch = mutableListOf<Note>()
        var currentBatchTokens = 0

        for (note in notes) {
            val noteTokens = estimateNoteTokens(note)

            // Single note exceeds limit - give it its own batch (LLM will try)
            if (noteTokens > availableTokens) {
                if (currentBatch.isNotEmpty()) {
                    batches.add(currentBatch.toList())
                    currentBatch = mutableListOf()
                    currentBatchTokens = 0
                }
                batches.add(listOf(note))
                println("[TokenBatcher] Note ${note.id} exceeds token limit ($noteTokens > $availableTokens)")
                continue
            }

            // Check if adding this note would exceed the limit
            if (currentBatchTokens + noteTokens > availableTokens) {
                batches.add(currentBatch.toList())
                currentBatch = mutableListOf(note)
                currentBatchTokens = noteTokens
            } else {
                currentBatch.add(note)
                currentBatchTokens += noteTokens
            }
        }

        // Don't forget the last batch
        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch.toList())
        }

        // Log batch statistics
        val batchSizes = batches.map { it.size }
        val avgBatchSize = batchSizes.average()
        println(
            "[TokenBatcher] Created ${batches.size} batches from ${notes.size} notes " +
                "(avg ${String.format("%.1f", avgBatchSize)} notes/batch, base tokens: $baseTokens, max: $maxInputTokens)"
        )

        return batches
    }

    /**
     * Calculate the base prompt tokens (system prompt + batch header).
     * Uses PromptBuilder to match actual prompts sent to LLM.
     */
    private fun calculateBasePromptTokens(userPrompt: String, noteType: NoteTypeInfo): Int {
        val systemPrompt = PromptBuilder.buildSystemPrompt()
        val headerPrompt = PromptBuilder.buildBatchHeader(userPrompt, noteType)
        return TokenEstimator.estimate(systemPrompt) + TokenEstimator.estimate(headerPrompt)
    }

    /**
     * Estimate tokens for a single note using actual prompt formatting.
     */
    private fun estimateNoteTokens(note: Note): Int {
        val formatted = PromptBuilder.formatNote(note, 0)
        return TokenEstimator.estimate(formatted)
    }
}
