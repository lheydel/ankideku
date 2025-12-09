package com.ankideku.domain.tool

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Marker interface for tool contexts.
 * Each context type provides the data needed for tools to execute in a specific domain.
 */
interface ToolContext

/**
 * Generic interface for AI tools that can be executed in a given context.
 * Tools are invoked via structured output parsing from LLM responses.
 */
interface AiTool<C : ToolContext> {
    /** Unique identifier for this tool (used in action JSON) */
    val name: String

    /** Human-readable description of what this tool does (included in system prompt) */
    val description: String

    /** JSON schema describing the parameters this tool accepts */
    val parametersSchema: ToolParametersSchema

    /**
     * Execute the tool with the given parameters in the provided context.
     * @param params The parsed parameters from the action JSON
     * @param context The execution context providing access to domain data
     * @return Result of the tool execution
     */
    suspend fun execute(params: JsonObject, context: C): ToolResult
}

/**
 * Schema definition for tool parameters.
 * Used to generate the system prompt documentation for the AI.
 */
@Serializable
data class ToolParametersSchema(
    val properties: Map<String, ToolParameter>,
    val required: List<String> = emptyList(),
)

@Serializable
enum class ToolParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY,
    OBJECT,
}

@Serializable
data class ToolParameter(
    val type: ToolParameterType,
    val description: String,
    val items: ToolParameter? = null,  // For array types
    val properties: Map<String, ToolParameter>? = null,  // For object types
)

/**
 * Result of a tool execution.
 */
sealed class ToolResult {
    /** Tool executed successfully */
    data class Success(val message: String) : ToolResult()

    /** Tool execution failed */
    data class Error(val message: String) : ToolResult()
}
