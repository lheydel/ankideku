package com.ankideku.domain.tool.review

import com.ankideku.domain.tool.ToolParameter
import com.ankideku.domain.tool.ToolParameterType
import com.ankideku.domain.tool.ToolParametersSchema
import com.ankideku.domain.tool.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Tool for the AI to suggest changes to one or more notes.
 * Creates review_suggestion entries that the user can apply.
 */
object MakeReviewSuggestionTool : ReviewTool {

    override val name = "make_suggestion"

    override val description = """
        Suggest changes to one or more flashcard notes. Each suggestion will be shown to the user
        who can choose to apply or dismiss it. Use this when you want to propose specific field
        changes based on your review.
    """.trimIndent()

    override val parametersSchema = ToolParametersSchema(
        properties = mapOf(
            "suggestions" to ToolParameter(
                type = ToolParameterType.ARRAY,
                description = "List of suggestions to make",
                items = ToolParameter(
                    type = ToolParameterType.OBJECT,
                    description = "A single suggestion for a note",
                    properties = mapOf(
                        "suggestionId" to ToolParameter(
                            type = ToolParameterType.NUMBER,
                            description = "The ID of the original suggestion to modify",
                        ),
                        "changes" to ToolParameter(
                            type = ToolParameterType.OBJECT,
                            description = "Map of field names to new values",
                        ),
                        "reasoning" to ToolParameter(
                            type = ToolParameterType.STRING,
                            description = "Optional explanation for why you're suggesting these changes",
                        ),
                    ),
                ),
            ),
        ),
        required = listOf("suggestions"),
    )

    override suspend fun execute(params: JsonObject, context: ReviewToolContext): ToolResult {
        val suggestionsArray = params["suggestions"]?.jsonArray
            ?: return ToolResult.Error("Missing required parameter: suggestions")

        if (suggestionsArray.isEmpty()) {
            return ToolResult.Error("No suggestions provided")
        }

        val validSuggestionIds = context.allSuggestions.map { it.id }.toSet()
        var successCount = 0
        val errors = mutableListOf<String>()

        for (suggestionJson in suggestionsArray) {
            try {
                val obj = suggestionJson.jsonObject
                val suggestionId = obj["suggestionId"]?.jsonPrimitive?.longOrNull
                    ?: run {
                        errors.add("Missing suggestionId in suggestion")
                        continue
                    }

                if (suggestionId !in validSuggestionIds) {
                    errors.add("Invalid suggestionId: $suggestionId")
                    continue
                }

                val changesObj = obj["changes"]?.jsonObject
                    ?: run {
                        errors.add("Missing changes for suggestionId $suggestionId")
                        continue
                    }

                val changes = changesObj.mapValues { (_, value) ->
                    value.jsonPrimitive.content
                }

                if (changes.isEmpty()) {
                    errors.add("Empty changes for suggestionId $suggestionId")
                    continue
                }

                val reasoning = obj["reasoning"]?.jsonPrimitive?.content

                context.onCreateReviewSuggestion(suggestionId, changes, reasoning)
                successCount++
            } catch (e: Exception) {
                errors.add("Failed to process suggestion: ${e.message}")
            }
        }

        return if (successCount > 0) {
            val message = buildString {
                append("Created $successCount suggestion(s)")
                if (errors.isNotEmpty()) {
                    append(". Errors: ${errors.joinToString("; ")}")
                }
            }
            ToolResult.Success(message)
        } else {
            ToolResult.Error("Failed to create any suggestions: ${errors.joinToString("; ")}")
        }
    }
}
