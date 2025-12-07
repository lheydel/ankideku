package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

object ComparisonOperators : List<ComparisonOperator> by listOf(
    ComparisonOperator("==", "=", "Equals", "Check if two values are equal"),
    ComparisonOperator("!=", "<>", "Not Equals", "Check if two values are different"),
    ComparisonOperator("<", "<", "Less Than", "Check if left is less than right"),
    ComparisonOperator("<=", "<=", "Less Than Or Equal", "Check if left is less than or equal to right"),
    ComparisonOperator(">", ">", "Greater Than", "Check if left is greater than right"),
    ComparisonOperator(">=", ">=", "Greater Than Or Equal", "Check if left is greater than or equal to right"),
)

/**
 * Parameterized comparison operator.
 *
 * @param key The operator key ("==", "!=", "<", "<=", ">", ">=")
 * @param sqlOperator The corresponding sql operator
 * @param displayName Human-readable name for UI
 * @param description Brief description of the operation
 */
class ComparisonOperator(
    override val key: String,
    val sqlOperator: String,
    displayName: String,
    description: String,
) : SelOperator {

    override val metadata = SelOperatorMetadata(
        displayName = displayName,
        category = SelOperatorCategory.Comparison,
        description = description,
        signature = SelOperatorSignature.binary(SelType.Any, SelType.Boolean),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)
        val left = evaluator.eval(args[0], context, jsonPath, 0)
        val right = evaluator.eval(args[1], context, jsonPath, 1)
        return SqlFragment("(${left.sql} $sqlOperator ${right.sql})", left.params + right.params)
    }
}
