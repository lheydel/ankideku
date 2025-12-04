package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Null check operators (isNull, isNotNull).
 */
object NullCheckOperators : List<NullCheckOperator> by listOf(
    NullCheckOperator("isNull", expectNull = true),
    NullCheckOperator("isNotNull", expectNull = false),
)

/**
 * Parameterized null check operator.
 *
 * @param key The operator key ("isNull" or "isNotNull")
 * @param expectNull If true, returns true when value is null
 */
class NullCheckOperator(
    override val key: String,
    private val expectNull: Boolean,
) : SelOperator {

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)
        val inner = evaluator.eval(args[0], context, jsonPath, 0)
        val sqlOp = if (expectNull) "IS NULL" else "IS NOT NULL"
        return SqlFragment("(${inner.sql} $sqlOp)", inner.params)
    }
}
