package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Null check operators (isNull, isNotNull).
 */
object NullCheckOperators : List<NullCheckOperator> by listOf(
    NullCheckOperator("isNull", expectNull = true, "Is Null", "Check if value is null"),
    NullCheckOperator("isNotNull", expectNull = false, "Is Not Null", "Check if value is not null"),
)

/**
 * Parameterized null check operator.
 *
 * @param key The operator key ("isNull" or "isNotNull")
 * @param expectNull If true, returns true when value is null
 * @param displayName Human-readable name for UI
 * @param description Brief description of the operation
 */
class NullCheckOperator(
    override val key: String,
    private val expectNull: Boolean,
    displayName: String,
    description: String,
) : SelOperator {

    override val metadata = SelOperatorMetadata(
        displayName = displayName,
        category = SelOperatorCategory.Predicate,
        description = description,
        signature = SelOperatorSignature.unary(SelType.Any, SelType.Boolean),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)
        val inner = evaluator.eval(args[0], context, jsonPath, 0)
        val sqlOp = if (expectNull) "IS NULL" else "IS NOT NULL"
        return SqlFragment("(${inner.sql} $sqlOp)", inner.params)
    }
}
