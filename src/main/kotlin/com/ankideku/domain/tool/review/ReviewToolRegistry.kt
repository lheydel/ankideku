package com.ankideku.domain.tool.review

private val tools = mutableMapOf<String, ReviewTool>()

/**
 * Registry of available tools for review sessions.
 */
object ReviewToolRegistry : Map<String, ReviewTool> by tools {

    /**
     * Register a tool. If a tool with the same name exists, it will be replaced.
     */
    fun register(tool: ReviewTool) {
        tools[tool.name] = tool
    }

    /**
     * Generate the system prompt section describing available tools.
     * This is included in the AI's system prompt.
     */
    fun generateToolsPrompt(): String = buildString {
        appendLine("You have access to the following actions. Output them as JSON blocks in your response:")
        appendLine()
        for (tool in tools.values) {
            appendLine("### ${tool.name}")
            appendLine(tool.description)
            appendLine()
            appendLine("Parameters:")
            for ((paramName, param) in tool.parametersSchema.properties) {
                val required = if (paramName in tool.parametersSchema.required) " (required)" else ""
                appendLine("- $paramName (${param.type.name.lowercase()})$required: ${param.description}")
            }
            appendLine()
            appendLine("Example:")
            appendLine("```json")
            appendLine("""{"action": "${tool.name}", ...parameters...}""")
            appendLine("```")
            appendLine()
        }
    }

    init {
        register(MakeReviewSuggestionTool)
        register(MemoryTool)
    }
}
