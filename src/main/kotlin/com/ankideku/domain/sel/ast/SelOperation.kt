package com.ankideku.domain.sel.ast

/**
 * Type alias for operator keys (e.g., "==", "and", "field", "contains").
 */
typealias SelOperatorKey = String

/**
 * Represents an operation in the SEL AST.
 *
 * An operation consists of an operator key and its arguments.
 * In JSON, this is represented as a single-key object:
 * ```json
 * { "operator": [arg1, arg2, ...] }
 * // or for single argument:
 * { "operator": arg }
 * ```
 *
 * Examples:
 * - `{ "==": [{ "field": "kanji" }, "日本"] }` → EqOperator with 2 args
 * - `{ "isEmpty": { "field": "example" } }` → IsEmptyOperator with 1 arg
 * - `{ "and": [...conditions] }` → AndOperator with variadic args
 */
data class SelOperation(
    val operator: SelOperatorKey,
    val arguments: SelArray
) : SelNode {
    override fun toJson(): String = when (arguments.size) {
        1 -> "{\"$operator\":${arguments[0].toJson()}}"
        else -> "{\"$operator\":${arguments.toJson()}}"
    }
}
