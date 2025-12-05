package com.ankideku.domain.sel

import com.ankideku.domain.sel.ast.SelQuery

/**
 * Service for executing SEL (Structured Expression Language) queries.
 *
 * Provides a unified interface for:
 * - Parsing SEL JSON strings into query AST
 * - Executing queries against the database
 * - Converting results to typed domain models
 *
 * Usage:
 * ```kotlin
 * val service: SelService = ...
 *
 * // Parse and execute a query
 * val query = service.parseQuery("""{ "target": "Note", "where": { "isEmpty": { "field": "example" } } }""")
 * val result = service.execute(query)
 *
 * when (result) {
 *     is SelResult.Notes -> result.items.forEach { note -> ... }
 *     is SelResult.Suggestions -> result.items.forEach { suggestion -> ... }
 *     ...
 * }
 *
 * // Or use the shorthand
 * val result = service.executeJson("""{ "target": "Note", "where": ... }""")
 * ```
 */
interface SelService {
    /**
     * Parse a JSON string into a SelQuery AST.
     *
     * @param json The SEL query as JSON string
     * @return Parsed query object
     * @throws SelParseException if the JSON is invalid or doesn't conform to SEL format
     */
    fun parseQuery(json: String): SelQuery

    /**
     * Execute a parsed SEL query and return typed results.
     *
     * @param query The parsed query to execute
     * @return Typed result based on query target (Notes, Suggestions, etc.)
     * @throws SelExecutionException if query execution fails
     */
    fun execute(query: SelQuery): SelResult

    /**
     * Parse and execute a SEL query from JSON string.
     *
     * Shorthand for `execute(parseQuery(json))`.
     *
     * @param json The SEL query as JSON string
     * @return Typed result based on query target
     * @throws SelParseException if parsing fails
     * @throws SelExecutionException if execution fails
     */
    fun executeJson(json: String): SelResult = execute(parseQuery(json))
}

/**
 * Exception thrown when SEL query execution fails.
 */
class SelExecutionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
