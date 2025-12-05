package com.ankideku.domain.sel.operator

/**
 * Value types in SEL expressions.
 * Used to define what types operators accept and return.
 */
enum class SelType {
    /** String value */
    String,

    /** Numeric value (integer or decimal) */
    Number,

    /** Boolean value (true/false) */
    Boolean,

    /** Any type - accepts/returns any value */
    Any,
}

/**
 * Categories of operators for UI grouping.
 */
enum class SelOperatorCategory(val displayName: String) {
    /** Comparison operators (==, !=, <, <=, >, >=) */
    Comparison("Comparison"),

    /** String operations (contains, startsWith, endsWith, len) */
    String("String"),

    /** Logical operations (and, or, not) */
    Logic("Logic"),

    /** Predicate checks (isEmpty, isNotEmpty, isNull, isNotNull) */
    Predicate("Predicate"),

    /** Mathematical operations (+, -, *, /, %) */
    Math("Math"),

    /** Aggregate functions (count, avg, min, max) */
    Aggregate("Aggregate"),

    /** Internal operators not shown in UI (field, prop, ref, query, exists) */
    Internal("Internal"),
}

/**
 * Metadata for an operator's signature.
 * Defines argument count, types, and return type.
 */
data class SelOperatorSignature(
    /** Minimum number of arguments */
    val minArgs: Int,

    /** Maximum number of arguments (Int.MAX_VALUE for unlimited) */
    val maxArgs: Int = minArgs,

    /** Types accepted for each argument position, or single type for all positions */
    val argTypes: List<SelType>,

    /** Return type of the operator */
    val returnType: SelType,
) {
    val argCount: IntRange get() = minArgs..maxArgs

    /**
     * Get the expected type for a specific argument position.
     * If argTypes has fewer entries than the position, uses the last type (for variadic operators).
     */
    fun argTypeAt(index: Int): SelType =
        argTypes.getOrElse(index) { argTypes.lastOrNull() ?: SelType.Any }

    companion object {
        /** Signature for binary operators (exactly 2 args) */
        fun binary(argType: SelType, returnType: SelType) = SelOperatorSignature(
            minArgs = 2,
            maxArgs = 2,
            argTypes = listOf(argType, argType),
            returnType = returnType,
        )

        /** Signature for unary operators (exactly 1 arg) */
        fun unary(argType: SelType, returnType: SelType) = SelOperatorSignature(
            minArgs = 1,
            maxArgs = 1,
            argTypes = listOf(argType),
            returnType = returnType,
        )

        /** Signature for variadic operators (2+ args of same type) */
        fun variadic(argType: SelType, returnType: SelType, minArgs: Int = 2) = SelOperatorSignature(
            minArgs = minArgs,
            maxArgs = Int.MAX_VALUE,
            argTypes = listOf(argType),
            returnType = returnType,
        )
    }
}

/**
 * Complete metadata for an operator.
 * Contains display information, categorization, and type signature.
 */
data class SelOperatorMetadata(
    /** Human-readable display name (e.g., "Add" for "+") */
    val displayName: String,

    /** Category for UI grouping */
    val category: SelOperatorCategory,

    /** Brief description of what the operator does */
    val description: String,

    /** Type signature (arguments and return type) */
    val signature: SelOperatorSignature,
)
