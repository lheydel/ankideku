package com.ankideku.util

import com.ankideku.domain.model.NoteField

/**
 * Gets the display value for a note based on the field display configuration.
 * Falls back to the first field (by order) if no configuration exists for the model.
 *
 * @param modelName The note type/model name
 * @param fields The note's fields
 * @param fieldDisplayConfig Map of model name to preferred field name
 * @param maxLength Maximum length of returned string (will be truncated if longer)
 * @param stripHtml Whether to strip HTML tags from the value
 * @return The display value for the note
 */
fun getDisplayField(
    modelName: String,
    fields: Map<String, NoteField>,
    fieldDisplayConfig: Map<String, String>,
    maxLength: Int = 100,
    stripHtml: Boolean = true,
): String {
    val configuredFieldName = fieldDisplayConfig[modelName]

    val fieldValue = if (configuredFieldName != null && fields.containsKey(configuredFieldName)) {
        fields[configuredFieldName]?.value
    } else {
        // Fallback to first field by order
        fields.values.minByOrNull { it.order }?.value
    }

    if (fieldValue.isNullOrBlank()) return "No content"

    var result = fieldValue
    if (stripHtml) {
        result = result.replace(Regex("<[^>]*>"), "")
    }

    return result.take(maxLength).trim().ifBlank { "No content" }
}
