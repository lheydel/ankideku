package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray

/**
 * Logical operators (and, or).
 */
object LogicOperators : List<LogicOperator> by listOf(
    LogicOperator("and", "AND", "And", "All conditions must be true"),
    LogicOperator("or", "OR", "Or", "At least one condition must be true"),
)

/**
 * Parameterized logical operator (and/or).
 *
 * @param key The operator key ("and" or "or")
 * @param sqlOperator The corresponding sql operator
 * @param displayName Human-readable name for UI
 * @param description Brief description of the operation
 */
class LogicOperator(
    override val key: String,
    val sqlOperator: String,
    displayName: String,
    description: String,
) : SelOperator {

    override val metadata = SelOperatorMetadata(
        displayName = displayName,
        category = SelOperatorCategory.Logic,
        description = description,
        signature = SelOperatorSignature.variadic(SelType.Boolean, SelType.Boolean),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireMinArgs(args, 1, key, jsonPath)

        val fragments = evaluator.evalAll(args.toList(), context, jsonPath)
        val combined = fragments.joinToString(" $sqlOperator ") { it.sql }
        val params = fragments.flatMap { it.params }

        return SqlFragment("($combined)", params)
    }
}
