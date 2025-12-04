package com.ankideku.domain.sel.ast

/**
 * Base sealed interface for all SEL (Structured Expression Language) AST nodes.
 *
 * SEL uses a JSON-based format where every operation is an object with exactly
 * one key (the operator) and its arguments as the value.
 */
sealed interface SelNode
