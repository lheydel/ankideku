package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Logical NOT operator.
 *
 * Usage: `{ "not": condition }`
 *
 * Returns the logical negation of the condition.
 */
object NotOperator : SelOperator {
    override val key = "not"

    override val metadata = SelOperatorMetadata(
        displayName = "Not",
        category = SelOperatorCategory.Logic,
        description = "Negate a condition",
        signature = SelOperatorSignature.unary(SelType.Boolean, SelType.Boolean),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)
        val inner = evaluator.eval(args[0], context, jsonPath, 0)
        return SqlFragment("NOT (${inner.sql})", inner.params)
    }
}
