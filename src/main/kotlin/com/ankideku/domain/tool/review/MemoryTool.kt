package com.ankideku.domain.tool.review

import com.ankideku.domain.tool.ToolParameter
import com.ankideku.domain.tool.ToolParameterType
import com.ankideku.domain.tool.ToolParametersSchema
import com.ankideku.domain.tool.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tool for the AI to store, retrieve, and manage persistent instructions.
 * Memory entries survive conversation resets.
 */
object MemoryTool : ReviewTool {

    override val name = "memory"

    override val description = """
        Store or manage persistent instructions that survive conversation resets.
        Use this proactively when the user gives you rules, preferences, or recurring instructions.

        Operations:
        - store: Save an instruction with a descriptive key
        - delete: Remove an instruction by key
        - list: Show all stored instructions
    """.trimIndent()

    override val parametersSchema = ToolParametersSchema(
        properties = mapOf(
            "operation" to ToolParameter(
                type = ToolParameterType.STRING,
                description = "The operation to perform: 'store', 'delete', or 'list'",
            ),
            "key" to ToolParameter(
                type = ToolParameterType.STRING,
                description = "A descriptive key for the instruction (e.g., 'kanji_rules', 'example_count'). Required for 'store' and 'delete' operations.",
            ),
            "value" to ToolParameter(
                type = ToolParameterType.STRING,
                description = "The instruction content to store. Required for 'store' operation.",
            ),
        ),
        required = listOf("operation"),
    )

    override suspend fun execute(params: JsonObject, context: ReviewToolContext): ToolResult {
        val operation = params["operation"]?.jsonPrimitive?.content
            ?: return ToolResult.Error("Missing required parameter: operation")

        return when (operation) {
            "store" -> executeStore(params, context)
            "delete" -> executeDelete(params, context)
            "list" -> executeList(context)
            else -> ToolResult.Error("Unknown operation: $operation. Use 'store', 'delete', or 'list'.")
        }
    }

    private suspend fun executeStore(params: JsonObject, context: ReviewToolContext): ToolResult {
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult.Error("Missing required parameter: key")
        val value = params["value"]?.jsonPrimitive?.content
            ?: return ToolResult.Error("Missing required parameter: value")

        if (key.isBlank()) {
            return ToolResult.Error("Key cannot be blank")
        }
        if (value.isBlank()) {
            return ToolResult.Error("Value cannot be blank")
        }

        return try {
            val isUpdate = context.memory.containsKey(key)
            context.onSaveMemory(key, value)
            if (isUpdate) {
                ToolResult.Success("Updated instruction '$key'")
            } else {
                ToolResult.Success("Stored new instruction '$key'")
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to store instruction: ${e.message}")
        }
    }

    private suspend fun executeDelete(params: JsonObject, context: ReviewToolContext): ToolResult {
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult.Error("Missing required parameter: key")

        if (!context.memory.containsKey(key)) {
            return ToolResult.Error("No instruction found with key '$key'")
        }

        return try {
            context.onDeleteMemory(key)
            ToolResult.Success("Deleted instruction '$key'")
        } catch (e: Exception) {
            ToolResult.Error("Failed to delete instruction: ${e.message}")
        }
    }

    private fun executeList(context: ReviewToolContext): ToolResult {
        if (context.memory.isEmpty()) {
            return ToolResult.Success("No instructions stored yet.")
        }

        val list = context.memory.entries.joinToString("\n") { (key, value) ->
            "- $key: $value"
        }
        return ToolResult.Success("Stored instructions:\n$list")
    }
}
