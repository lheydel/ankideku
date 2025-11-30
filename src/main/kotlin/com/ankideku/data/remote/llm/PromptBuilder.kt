package com.ankideku.data.remote.llm

import com.ankideku.data.remote.llm.LlmJsonFields.CHANGES
import com.ankideku.data.remote.llm.LlmJsonFields.NOTE_ID
import com.ankideku.data.remote.llm.LlmJsonFields.REASONING
import com.ankideku.data.remote.llm.LlmJsonFields.SUGGESTIONS
import com.ankideku.domain.model.Note

/**
 * Builds prompts for LLM batch analysis.
 * Formats card data efficiently to save tokens.
 */
object PromptBuilder {

    /**
     * Build the complete prompt for a batch of notes
     */
    fun buildPrompt(
        notes: List<Note>,
        userPrompt: String,
        noteType: NoteTypeInfo,
    ): String {
        val systemPrompt = buildSystemPrompt()
        val batchPrompt = buildBatchPrompt(notes, userPrompt, noteType)
        return "$systemPrompt\n\n---\n\n$batchPrompt"
    }

    /**
     * Build the system prompt (fixed overhead)
     */
    fun buildSystemPrompt(): String = """
You are an expert at analyzing Anki flashcard notes and suggesting improvements.

Your task: Analyze notes and suggest field modifications based on user instructions.

Output format: JSON object with suggestions array.
Schema:
{
  "$SUGGESTIONS": [
    {
      "$NOTE_ID": number,
      "$CHANGES": { "FieldName": "new value" },
      "$REASONING": "brief explanation"
    }
  ]
}

Rules:
- Only suggest changes for notes that need improvement
- Skip notes that are already good
- Return empty suggestions array if no notes need changes: { "$SUGGESTIONS": [] }
- Keep reasoning concise (saves cost)
- Return ONLY valid JSON, no other text
- CRITICAL: Your response will be parsed programmatically. Invalid JSON will cause errors. Ensure proper formatting.
- Field names in "$CHANGES" must exactly match the available fields listed
- $NOTE_ID must match one of the provided note IDs
""".trimIndent()

    /**
     * Build the batch prompt header (without notes).
     * Used for token estimation of fixed overhead per batch.
     */
    fun buildBatchHeader(userPrompt: String, noteType: NoteTypeInfo): String = """
Note type: "${noteType.name}"
Available fields: [${noteType.fields.joinToString(", ") { "\"$it\"" }}]

User request: "$userPrompt"

Notes to analyze:

Analyze these notes and suggest improvements following the user's request.
Return ONLY the JSON output, no other text.
""".trimIndent()

    private fun buildBatchPrompt(
        notes: List<Note>,
        userPrompt: String,
        noteType: NoteTypeInfo,
    ): String {
        val formattedNotes = notes.mapIndexed { idx, note ->
            formatNote(note, idx)
        }.joinToString("\n\n")

        return """
Note type: "${noteType.name}"
Available fields: [${noteType.fields.joinToString(", ") { "\"$it\"" }}]

User request: "$userPrompt"

Notes to analyze:
$formattedNotes

Analyze these notes and suggest improvements following the user's request.
Return ONLY the JSON output, no other text.
""".trimIndent()
    }

    /**
     * Format a single note for the prompt.
     * Omits empty fields to save tokens.
     */
    fun formatNote(note: Note, index: Int): String = buildString {
        appendLine("Note ${index + 1} (ID: ${note.id}):")

        // Add non-empty fields only
        for ((fieldName, field) in note.fields) {
            val value = field.value.trim()
            if (value.isNotEmpty()) {
                // Indent field content and handle multiline values
                val formattedValue = if (value.contains('\n')) {
                    "\n    ${value.replace("\n", "\n    ")}"
                } else {
                    value
                }
                appendLine("  $fieldName: $formattedValue")
            }
        }
    }.trimEnd()
}
