package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelOperation
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Exists operator.
 *
 * Checks if a subquery returns any rows.
 *
 * Usage: `{ "exists": { "query": { "target": "...", "where": ... } } }`
 *
 * Example:
 * ```json
 * {
 *   "target": "Note",
 *   "alias": "n",
 *   "where": { "exists": { "query": {
 *     "target": "Suggestion",
 *     "where": { "and": [
 *       { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
 *       { "==": [{ "prop": "status" }, "accepted"] }
 *     ]}
 *   }}}
 * }
 * ```
 *
 * Generates:
 * ```sql
 * SELECT n.* FROM cached_note n WHERE EXISTS (
 *   SELECT 1 FROM suggestion s WHERE s.note_id = n.id AND s.status = 'accepted'
 * )
 * ```
 *
 * Combine with `not` for non-existence:
 * `{ "not": { "exists": { "query": {...} } } }`
 */
object ExistsOperator : SelOperator {
    override val key = "exists"

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)

        val innerArg = args[0]

        // The argument should be a query operation: { "query": {...} }
        val queryFragment = when (innerArg) {
            is SelOperation -> {
                if (innerArg.operator != QueryOperator.key) {
                    throw SelOperatorException(
                        "exists expects { \"query\": {...} }, got operator '${innerArg.operator}'",
                        jsonPath
                    )
                }
                evaluator.eval(innerArg, context, jsonPath, 0)
            }
            is SelQuery -> {
                // Direct SelQuery (when parsed directly)
                QueryOperator.buildSubquery(evaluator, innerArg, context, "$jsonPath[0]")
            }
            else -> throw SelOperatorException(
                "exists expects { \"query\": {...} } or a SelQuery, got ${innerArg::class.simpleName}",
                jsonPath
            )
        }

        return SqlFragment("EXISTS ${queryFragment.sql}", queryFragment.params)
    }
}
