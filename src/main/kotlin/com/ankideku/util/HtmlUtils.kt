package com.ankideku.util

/**
 * Converts basic HTML (as used by Anki) to plain text for display.
 * Handles common tags and entities.
 */
fun String.htmlToPlainText(): String {
    return this
        // Convert block-level tags to newlines
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?div[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?p[^>]*>", RegexOption.IGNORE_CASE), "\n")
        // Remove other HTML tags
        .replace(Regex("<[^>]+>"), "")
        // Decode common HTML entities
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        // Collapse multiple consecutive newlines to max 2
        .replace(Regex("\n{3,}"), "\n\n")
        // Trim leading/trailing whitespace from each line
        .lines()
        .joinToString("\n") { it.trim() }
        .trim()
}
