package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelOrderDirection
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.schema.SelEntityRegistry

/**
 * Subquery operator.
 *
 * Wraps a SelQuery to be used as a subquery expression.
 * The subquery can optionally return a specific value via the `result` field.
 *
 * Usage:
 * - Without result: `{ "query": { "target": "...", "where": ... } }` → `(SELECT 1 FROM ...)` (for EXISTS)
 * - With result: `{ "query": { "target": "...", "where": ..., "result": {...} } }` → `(SELECT {result} FROM ...)`
 *
 * Example - scalar subquery returning a property:
 * ```json
 * { "==": [
 *   { "query": {
 *     "target": "Suggestion",
 *     "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
 *     "result": { "prop": "status" },
 *     "orderBy": [{ "prop": "createdAt", "desc": true }],
 *     "limit": 1
 *   }},
 *   "accepted"
 * ]}
 * ```
 *
 * Note: The query operator expects a SelQuery as its argument, which must be
 * parsed by SelParser.parseQueryNode() and converted to a SelOperation wrapping the query.
 */
object QueryOperator : SelOperator {
    override val key = "query"

    override val metadata = SelOperatorMetadata(
        displayName = "Subquery",
        category = SelOperatorCategory.Internal,
        description = "Execute a nested subquery",
        signature = SelOperatorSignature.unary(SelType.Any, SelType.Any),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)

        val queryNode = args[0]
        if (queryNode !is SelQuery) {
            throw SelOperatorException(
                "query operator expects a SelQuery, got ${queryNode::class.simpleName}",
                jsonPath
            )
        }

        return buildSubquery(evaluator, queryNode, context, jsonPath)
    }

    /**
     * Build a scalar subquery from a SelQuery.
     */
    internal fun buildSubquery(evaluator: SelSqlEvaluator, query: SelQuery, parentContext: SelSqlContext, jsonPath: String): SqlFragment {
        val schema = SelEntityRegistry[query.target]
        val childContext = parentContext.childContext(query, jsonPath)

        // Evaluate the WHERE clause
        val whereFragment = evaluator.toSql(query.where, childContext, "$jsonPath.where")

        // Determine what to SELECT
        // Default to "1" for EXISTS subqueries; use "result" for scalar subqueries
        val selectFragment = if (query.result != null) {
            evaluator.toSql(query.result, childContext, "$jsonPath.result")
        } else {
            SqlFragment("1")
        }

        // Build ORDER BY if present
        val orderBySql = query.orderBy?.let { clauses ->
            " ORDER BY " + clauses.joinToString(", ") { clause ->
                // Resolve the field/property for ordering
                val prop = schema.getProperty(clause.field)
                    ?: throw SelOperatorException(
                        "Unknown property '${clause.field}' for entity ${query.target} in orderBy",
                        jsonPath
                    )
                val direction = if (clause.direction == SelOrderDirection.Desc) "DESC" else "ASC"
                "${schema.sqlAlias}.${prop.sqlColumn} $direction"
            }
        } ?: ""

        // Build LIMIT if present
        val limitFragment = query.limit?.let {
            SqlFragment(" LIMIT ?", listOf(it))
        } ?: SqlFragment("")

        val sql = "(SELECT ${selectFragment.sql} FROM ${schema.sqlTable} ${schema.sqlAlias} " +
            "WHERE ${whereFragment.sql}$orderBySql${limitFragment.sql})"

        return SqlFragment(sql, selectFragment.params + whereFragment.params + limitFragment.params)
    }
}
