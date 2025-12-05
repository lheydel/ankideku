package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * String length operator.
 *
 * Usage: `{ "len": value }`
 *
 * Returns the length of a string.
 */
object LenOperator : SelOperator {
    override val key = "len"

    override val metadata = SelOperatorMetadata(
        displayName = "Length",
        category = SelOperatorCategory.String,
        description = "Get the length of a string",
        signature = SelOperatorSignature.unary(SelType.String, SelType.Number),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)
        val inner = evaluator.eval(args[0], context, jsonPath, 0)
        // Use COALESCE to handle NULL values (return 0 for NULL)
        return SqlFragment("COALESCE(LENGTH(${inner.sql}), 0)", inner.params)
    }
}
