package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

object ComparisonOperators : List<ComparisonOperator> by listOf(
    ComparisonOperator("==", "="),
    ComparisonOperator("!=", "<>"),
    ComparisonOperator("<", "<"),
    ComparisonOperator("<=", "<="),
    ComparisonOperator(">", ">"),
    ComparisonOperator(">=", ">="),
)

/**
 * Parameterized comparison operator.
 *
 * @param key The operator key ("==", "!=", "<", "<=", ">", ">=")
 * @param sqlOperator The corresponding sql operator
 */
class ComparisonOperator(
    override val key: String,
    val sqlOperator: String,
) : SelOperator {

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)
        val left = evaluator.eval(args[0], context, jsonPath, 0)
        val right = evaluator.eval(args[1], context, jsonPath, 1)
        return SqlFragment("(${left.sql} $sqlOperator ${right.sql})", left.params + right.params)
    }
}
