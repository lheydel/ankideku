package com.ankideku.data.remote.llm

import com.ankideku.data.remote.llm.LlmJsonFields.CHANGES
import com.ankideku.data.remote.llm.LlmJsonFields.NOTE_ID
import com.ankideku.data.remote.llm.LlmJsonFields.REASONING
import com.ankideku.data.remote.llm.LlmJsonFields.SUGGESTIONS
import com.ankideku.util.json
import kotlinx.serialization.json.*

/**
 * Parses raw LLM response into validated suggestions.
 * Handles cases where LLM includes extra text around JSON.
 */
class ResponseParser {

    /**
     * Parse raw LLM response into validated suggestions
     * @param rawResponse Raw string response from LLM
     * @param validNoteIds Set of noteIds that were in this batch (for validation)
     */
    fun parse(rawResponse: String, validNoteIds: Set<Long>): List<LlmSuggestion> {
        val jsonObject = extractJson(rawResponse)
        return validateSuggestions(jsonObject, validNoteIds)
    }

    /**
     * Extract JSON object from raw text
     * Handles cases where LLM includes extra text around JSON
     */
    private fun extractJson(text: String): JsonObject {
        val trimmed = text.trim()

        // Try parsing as-is first
        runCatching {
            return json.parseToJsonElement(trimmed).jsonObject
        }

        // Try to find JSON object in the text
        val jsonMatch = Regex("""\{[\s\S]*\}""").find(trimmed)
        if (jsonMatch != null) {
            runCatching {
                return json.parseToJsonElement(jsonMatch.value).jsonObject
            }
        }

        // Try to find JSON between code fences
        val codeFenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(trimmed)
        if (codeFenceMatch != null) {
            runCatching {
                return json.parseToJsonElement(codeFenceMatch.groupValues[1].trim()).jsonObject
            }
        }

        throw IllegalArgumentException(
            "Failed to parse JSON from LLM response: ${trimmed.take(200)}..."
        )
    }

    /**
     * Validate and filter suggestions
     */
    private fun validateSuggestions(
        response: JsonObject,
        validNoteIds: Set<Long>,
    ): List<LlmSuggestion> {
        val suggestionsArray = response[SUGGESTIONS]?.jsonArrayOrNull
            ?: return emptyList()  // Empty response is valid (no suggestions needed)

        return suggestionsArray.mapNotNull { element ->
            val obj = element.jsonObjectOrNull ?: return@mapNotNull null

            // Parse and validate noteId
            val noteId = when (val rawId = obj[NOTE_ID]) {
                is JsonPrimitive -> when {
                    rawId.isString -> rawId.content.toLongOrNull()
                    else -> rawId.longOrNull
                }
                else -> null
            } ?: return@mapNotNull null

            if (noteId !in validNoteIds) {
                return@mapNotNull null
            }

            // Validate changes
            val changesObj = obj[CHANGES]?.jsonObjectOrNull
                ?: return@mapNotNull null

            val changes = mutableMapOf<String, String>()
            for ((field, value) in changesObj) {
                val strValue = when (value) {
                    is JsonPrimitive -> value.contentOrNull ?: value.toString()
                    else -> value.toString()
                }
                changes[field] = strValue
            }

            if (changes.isEmpty()) {
                return@mapNotNull null
            }

            // Validate reasoning (optional but encouraged)
            val reasoning = obj[REASONING]?.jsonPrimitive?.contentOrNull
                ?: "No reasoning provided"

            LlmSuggestion(
                noteId = noteId,
                changes = changes,
                reasoning = reasoning,
            )
        }
    }
}

private val JsonElement.jsonObjectOrNull: JsonObject?
    get() = (this as? JsonObject)

private val JsonElement.jsonArrayOrNull: JsonArray?
    get() = (this as? JsonArray)
