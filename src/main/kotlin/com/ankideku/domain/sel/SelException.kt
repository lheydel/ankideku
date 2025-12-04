package com.ankideku.domain.sel

/**
 * Base exception for all SEL-related errors.
 *
 * @param message Human-readable error description
 * @param jsonPath JSON path where the error occurred (e.g., "$.where.and[0]")
 * @param cause Optional underlying exception
 */
open class SelException(
    message: String,
    val jsonPath: String,
    cause: Throwable? = null
) : Exception("$message (Path: $jsonPath)", cause)

/**
 * Exception thrown during SEL operator execution (both in-memory evaluation and SQL generation).
 */
class SelOperatorException(
    message: String,
    jsonPath: String = "$",
    cause: Throwable? = null
) : SelException(message, jsonPath, cause)
