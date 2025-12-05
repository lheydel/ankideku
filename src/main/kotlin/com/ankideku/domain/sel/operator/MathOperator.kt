package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

/**
 * Base class for math operations.
 *
 * Handles SQL generation for arithmetic and aggregate operations.
 * Subclasses can override [toSql] for custom behavior.
 */
abstract class MathOperator : SelOperator {
    /** Default value prepended when fewer than minArguments are provided (e.g., 0 for unary minus) */
    protected open val defaultFirstArgument: Number? = null
    protected abstract val minArguments: Int
    protected open val maxArguments: Int = Int.MAX_VALUE

    /**
     * SQL operator to use between operands (e.g., "+", "-", "*", "/").
     * Override in subclasses that use simple binary operators.
     */
    protected open val sqlOperator: String? = null

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        val sqlOp = sqlOperator
            ?: throw SelOperatorException("Operator '$key' does not support SQL generation", jsonPath)

        if (args.size < minArguments) {
            throw SelOperatorException(
                "Not enough arguments for '$key'. Expected at least $minArguments, found ${args.size}.",
                jsonPath
            )
        }

        val fragments = evaluator.evalAll(args.toList(), context, jsonPath)

        // Handle default first argument for unary operations like unary minus
        val allFragments = if (fragments.size == minArguments - 1 && defaultFirstArgument != null) {
            listOf(SqlFragment("?", listOf(defaultFirstArgument))) + fragments
        } else {
            fragments
        }

        if (allFragments.isEmpty()) {
            return SqlFragment("NULL")
        }

        if (allFragments.size == 1) {
            return allFragments[0]
        }

        val combined = allFragments.joinToString(" $sqlOp ") { "(${it.sql})" }
        val params = allFragments.flatMap { it.params }
        return SqlFragment("($combined)", params)
    }

    companion object {
        /** Helper to create metadata for arithmetic operators */
        fun arithmeticMetadata(displayName: String, description: String, minArgs: Int, maxArgs: Int) =
            SelOperatorMetadata(
                displayName = displayName,
                category = SelOperatorCategory.Math,
                description = description,
                signature = SelOperatorSignature(
                    minArgs = minArgs,
                    maxArgs = maxArgs,
                    argTypes = listOf(SelType.Number),
                    returnType = SelType.Number,
                ),
            )

        /** Helper to create metadata for aggregate operators */
        fun aggregateMetadata(displayName: String, description: String, maxArgs: Int = 1) =
            SelOperatorMetadata(
                displayName = displayName,
                category = SelOperatorCategory.Aggregate,
                description = description,
                signature = SelOperatorSignature(
                    minArgs = 1,
                    maxArgs = maxArgs,
                    argTypes = listOf(SelType.Any),
                    returnType = SelType.Number,
                ),
            )
    }
}
