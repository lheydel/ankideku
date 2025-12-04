package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Empty check operators (isEmpty, isNotEmpty).
 */
object EmptyCheckOperators : List<EmptyCheckOperator> by listOf(
    EmptyCheckOperator("isEmpty", expectEmpty = true),
    EmptyCheckOperator("isNotEmpty", expectEmpty = false),
)

/**
 * Parameterized empty check operator.
 *
 * @param key The operator key ("isEmpty" or "isNotEmpty")
 * @param expectEmpty If true, returns true when value is empty
 */
class EmptyCheckOperator(
    override val key: String,
    private val expectEmpty: Boolean,
) : SelOperator {

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)
        val inner = evaluator.eval(args[0], context, jsonPath, 0)
        // isEmpty: (value IS NULL OR value = '')
        // isNotEmpty: (value IS NOT NULL AND value != '')
        return if (expectEmpty) {
            SqlFragment("(${inner.sql} IS NULL OR ${inner.sql} = '')", inner.params + inner.params)
        } else {
            SqlFragment("(${inner.sql} IS NOT NULL AND ${inner.sql} != '')", inner.params + inner.params)
        }
    }
}
