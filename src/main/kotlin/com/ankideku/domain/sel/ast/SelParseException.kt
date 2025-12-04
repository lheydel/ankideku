package com.ankideku.domain.sel.ast

import com.ankideku.domain.sel.SelException

/**
 * Exception thrown during SEL parsing (JSON → AST conversion).
 */
class SelParseException(
    message: String,
    jsonPath: String,
    cause: Throwable? = null
) : SelException(message, jsonPath, cause)
