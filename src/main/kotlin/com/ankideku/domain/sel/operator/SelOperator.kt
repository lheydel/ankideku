package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelOperatorKey
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Interface for SEL operators.
 *
 * Each operator defines how to generate SQL for database queries.
 *
 * Operators receive unevaluated arguments and decide when/if to evaluate them,
 * enabling short-circuit evaluation for logical operators.
 *
 * Operators are registered in [SelOperatorRegistry] and looked up by their [key].
 */
interface SelOperator {
    /**
     * The operator key used in JSON (e.g., "==", "and", "field", "contains").
     */
    val key: SelOperatorKey

    /**
     * Generate SQL for this operator with the given arguments.
     *
     * @param evaluator The evaluator to use for evaluating sub-expressions
     * @param args The unevaluated arguments (SelArray of SelNode)
     * @param context The SQL context containing entity type, table alias, and scopes
     * @param jsonPath The current JSON path for error reporting
     * @return The SQL fragment with parameterized query and values
     */
    fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment
}
