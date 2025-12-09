package com.ankideku.data.remote.llm

import com.ankideku.util.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses action calls from LLM response text.
 * Actions are JSON objects with an "action" field, embedded in the response.
 */
object ActionParser {

    private val codeFenceRegex = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""")
    private val inlineArrayRegex = Regex("""\[[\s\S]*?]""")
    private val inlineObjectRegex = Regex("""\{[^{}]*"action"[^{}]*\}""")

    private val extractors = listOf<Pair<Regex, (MatchResult) -> String>>(
        codeFenceRegex to { it.groupValues[1] },
        inlineArrayRegex to { it.value },
        inlineObjectRegex to { it.value },
    )

    /**
     * Parse response text and extract action calls.
     * @param responseText Raw response from LLM
     * @return Pair of (text content without action blocks, list of parsed actions)
     */
    fun parse(responseText: String): ActionParseResult {
        val (text, actions) = extractors.fold(responseText to emptyList<ParsedActionCall>()) { (text, actions), (regex, extractor) ->
            val (newText, newActions) = extractActions(text, regex, extractor)
            newText to (actions + newActions)
        }

        // Clean up the text content
        val cleanedText = text
            .lines()
            .filter { it.isNotBlank() || it.isEmpty() }
            .joinToString("\n")
            .trim()

        return ActionParseResult(
            textContent = cleanedText.ifBlank { null },
            actionCalls = actions,
        )
    }

    /**
     * Generic extraction: find matches, parse JSON content, remove successful matches from text.
     */
    private fun extractActions(
        text: String,
        regex: Regex,
        contentExtractor: (MatchResult) -> String = { it.value },
    ): Pair<String, List<ParsedActionCall>> {
        var remaining = text
        val actions = mutableListOf<ParsedActionCall>()

        for (match in regex.findAll(text)) {
            val content = contentExtractor(match).trim()
            val parsed = parseJsonContent(content)
            if (parsed.isNotEmpty()) {
                actions.addAll(parsed)
                remaining = remaining.replace(match.value, " ")
            }
        }

        return remaining to actions
    }

    /**
     * Parse JSON content that may be an array, single object, or multiple objects.
     */
    private fun parseJsonContent(content: String): List<ParsedActionCall> {
        // Try as array
        runCatching {
            return json.parseToJsonElement(content).jsonArray.mapNotNull {
                (it as? JsonObject)?.toActionOrNull()
            }
        }

        // Try as single object
        runCatching {
            json.parseToJsonElement(content).jsonObject.toActionOrNull()?.let { return listOf(it) }
        }

        // Fall back to finding multiple objects via regex
        return Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""").findAll(content).mapNotNull { match ->
            runCatching { json.parseToJsonElement(match.value).jsonObject.toActionOrNull() }.getOrNull()
        }.toList()
    }

    private fun JsonObject.toActionOrNull(): ParsedActionCall? {
        val action = this["action"]?.jsonPrimitive?.content ?: return null
        return ParsedActionCall(action, JsonObject(filterKeys { it != "action" }))
    }
}

/**
 * Result of parsing action calls from response text.
 */
data class ActionParseResult(
    /** Remaining text content after removing action blocks */
    val textContent: String?,
    /** List of parsed action calls */
    val actionCalls: List<ParsedActionCall>,
)
