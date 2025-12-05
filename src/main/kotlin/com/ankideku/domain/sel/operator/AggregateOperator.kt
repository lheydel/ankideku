package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

object AggregateOperators : List<AggregateOperator> by listOf(
    AggregateOperator("count", "COUNT", "Count", "Count the number of values", maxArguments = 1),
    AggregateOperator("avg", "AVG", "Average", "Calculate the average of values", maxArguments = 1),
    AggregateOperator("min", "MIN", "Minimum", "Get the minimum value"),
    AggregateOperator("max", "MAX", "Maximum", "Get the maximum value"),
)

/**
 * Aggregate operator for SQL aggregate functions.
 *
 * @param key The operator key (e.g., "avg", "min", "max")
 * @param sqlFunction The SQL function name to use
 * @param displayName Human-readable name for UI
 * @param description Brief description of the operation
 * @param maxArguments Maximum number of arguments (default unlimited)
 */
class AggregateOperator(
    override val key: String,
    private val sqlFunction: String,
    displayName: String,
    description: String,
    override val maxArguments: Int = Int.MAX_VALUE,
) : MathOperator() {
    override val minArguments: Int = 1

    override val metadata = MathOperator.aggregateMetadata(displayName, description, maxArguments)

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireMinArgs(args, minArguments, key, jsonPath)
        requireMaxArgs(args, maxArguments, key, jsonPath)

        val fragments = evaluator.evalAll(args.toList(), context, jsonPath)
        val allSql = fragments.joinToString(", ") { it.sql }
        val allParams = fragments.flatMap { it.params }
        return SqlFragment("$sqlFunction($allSql)", allParams)
    }
}
