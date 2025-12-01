package com.ankideku.util

import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.NoteTypeConfig

/**
 * Gets the display value for a note based on the field display configuration.
 * Falls back to the first field (by order) if no configuration exists for the model.
 *
 * @param modelName The note type/model name
 * @param fields The note's fields
 * @param fieldDisplayConfig Map of model name to preferred field name
 * @param maxLength Maximum length of returned string (will be truncated if longer)
 * @param stripHtml Whether to strip HTML tags from the value
 * @return pair of field name and its display value
 */
fun getDisplayField(
    fields: Map<String, NoteField>,
    noteTypeConfig: NoteTypeConfig?,
    maxLength: Int = 100,
    stripHtml: Boolean = true,
): Pair<String, String> {
    val configuredFieldName = noteTypeConfig?.defaultDisplayField

    val fieldName = if (configuredFieldName != null && fields.containsKey(configuredFieldName)) {
        configuredFieldName
    } else {
        // Fallback to first field by order
        fields.values.minByOrNull { it.order }?.name ?: "Unknown Field"
    }

    val fieldValue = fields[fieldName]?.value ?: ""

    var result = fieldValue
    if (stripHtml) {
        result = result.replace(Regex("<[^>]*>"), "")
    }

    return fieldName to result.trim().take(maxLength)
}
