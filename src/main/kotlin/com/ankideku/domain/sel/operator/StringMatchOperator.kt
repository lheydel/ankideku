package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

/**
 * String matching operators (contains, startsWith, endsWith).
 */
object StringMatchOperators : List<StringMatchOperator> by listOf(
    StringMatchOperator("contains", StringMatchOperator.containsPattern),
    StringMatchOperator("startsWith", StringMatchOperator.startsWithPattern),
    StringMatchOperator("endsWith", StringMatchOperator.endsWithPattern),
)

/**
 * Parameterized string matching operator.
 *
 * @param key The operator key ("contains", "startsWith", "endsWith")
 * @param sqlPattern Function to create SQL LIKE pattern from needle
 */
class StringMatchOperator(
    override val key: String,
    private val sqlPattern: (SqlFragment) -> SqlFragment,
) : SelOperator {

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)
        val haystack = evaluator.eval(args[0], context, jsonPath, 0)
        val needle = evaluator.eval(args[1], context, jsonPath, 1)

        // Build LIKE pattern based on the match type
        val pattern = sqlPattern(needle)
        return SqlFragment(
            "(${haystack.sql} LIKE ${pattern.sql})",
            haystack.params + pattern.params
        )
    }

    companion object {
        /** Creates a contains pattern: '%' || needle || '%' */
        val containsPattern: (SqlFragment) -> SqlFragment = { needle ->
            SqlFragment("'%' || ${needle.sql} || '%'", needle.params)
        }

        /** Creates a startsWith pattern: needle || '%' */
        val startsWithPattern: (SqlFragment) -> SqlFragment = { needle ->
            SqlFragment("${needle.sql} || '%'", needle.params)
        }

        /** Creates an endsWith pattern: '%' || needle */
        val endsWithPattern: (SqlFragment) -> SqlFragment = { needle ->
            SqlFragment("'%' || ${needle.sql}", needle.params)
        }
    }
}
