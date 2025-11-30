package com.ankideku.data.remote.llm

import com.ankideku.data.remote.llm.LlmJsonFields.CHANGES
import com.ankideku.data.remote.llm.LlmJsonFields.NOTE_ID
import com.ankideku.data.remote.llm.LlmJsonFields.REASONING
import com.ankideku.data.remote.llm.LlmJsonFields.SUGGESTIONS
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock LLM Service
 * Test implementation that parses prompts and generates random suggestions.
 */
class MockLlmService(
    private val responseDelayMs: Long = 500,
    private val suggestionRate: Double = 0.5,
    maxRetries: Int = 0,
) : BaseLlmService(maxRetries = maxRetries) {

    override suspend fun getHealth(): LlmHealthStatus {
        return LlmHealthStatus(
            available = true,
            info = "Mock LLM Service (Testing)"
        )
    }

    override suspend fun callLlm(prompt: String): String {
        // Simulate response delay
        delay(responseDelayMs)

        // Parse the prompt to extract noteIds and field names
        val (noteIds, fieldNames) = parsePrompt(prompt)

        // Generate suggestions based on suggestionRate
        val suggestions = buildList {
            for (noteId in noteIds) {
                if (Random.nextDouble() < suggestionRate) {
                    // Pick a random field to modify
                    val fieldName = fieldNames.randomOrNull()
                    if (fieldName != null) {
                        add(buildSuggestionJson(noteId, fieldName))
                    }
                }
            }
        }

        // Return as JSON object with suggestions array
        return """{"$SUGGESTIONS": [${suggestions.joinToString(", ")}]}"""
    }

    /**
     * Parse the prompt to extract noteIds and field names
     */
    private fun parsePrompt(prompt: String): Pair<List<Long>, List<String>> {
        val noteIds = mutableListOf<Long>()
        val fieldNames = mutableListOf<String>()

        // Extract noteIds from "Note N (ID: XXX):" pattern
        val noteIdRegex = Regex("""Note \d+ \(ID: (\d+)\):""")
        for (match in noteIdRegex.findAll(prompt)) {
            match.groupValues[1].toLongOrNull()?.let { noteIds.add(it) }
        }

        // Extract field names from "Available fields: [...]" pattern
        val fieldsRegex = Regex("""Available fields:\s*\[([^\]]+)]""")
        fieldsRegex.find(prompt)?.let { match ->
            val fieldsStr = match.groupValues[1]
            val fieldRegex = Regex(""""([^"]+)"""")
            for (fieldMatch in fieldRegex.findAll(fieldsStr)) {
                fieldNames.add(fieldMatch.groupValues[1])
            }
        }

        return Pair(noteIds, fieldNames)
    }

    /**
     * Build a JSON suggestion object
     */
    private fun buildSuggestionJson(noteId: Long, fieldName: String): String {
        val randomValue = generateRandomValue(fieldName)
        // Escape quotes in values for valid JSON
        val escapedValue = randomValue.replace("\"", "\\\"")
        return """{"$NOTE_ID": $noteId, "$CHANGES": {"$fieldName": "$escapedValue"}, "$REASONING": "Mock: updated $fieldName field"}"""
    }

    /**
     * Generate a random value for a field
     */
    private fun generateRandomValue(fieldName: String): String {
        val randomId = Random.nextLong().toString(36).take(8)
        return "Mock $fieldName - $randomId"
    }
}
