package com.ankideku.domain.sel.evaluator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelBoolean
import com.ankideku.domain.sel.ast.SelNode
import com.ankideku.domain.sel.ast.SelNull
import com.ankideku.domain.sel.ast.SelNumber
import com.ankideku.domain.sel.ast.SelOperation
import com.ankideku.domain.sel.ast.SelOrderDirection
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.ast.SelString
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.model.SqlQuery
import com.ankideku.domain.sel.operator.QueryOperator
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import com.ankideku.domain.sel.schema.SelEntityRegistry

/**
 * SQL evaluator for SEL expressions.
 *
 * Converts a SelQuery AST into a parameterized SQL query that can be executed
 * against the database. Uses scalar subqueries for field access (no JOINs)
 * to avoid row duplication.
 *
 * Usage:
 * ```kotlin
 * val query = SelQuery(
 *     target = EntityType.Note,
 *     where = SelParser.parse("""{ "isEmpty": { "field": "example" } }""")
 * )
 * val sqlQuery = SelSqlEvaluator.buildQuery(query)
 * // sqlQuery.sql = "SELECT n.* FROM cached_note n WHERE ..."
 * // sqlQuery.params = [...]
 * ```
 */
object SelSqlEvaluator {

    /**
     * Build a complete SQL query from a SelQuery.
     *
     * @param query The SEL query to convert
     * @return A SqlQuery with parameterized SQL and values
     */
    fun buildQuery(query: SelQuery): SqlQuery {
        val schema = SelEntityRegistry.get(query.target)
        val context = createContext(query)

        val whereFragment = toSql(query.where, context, "$.where")

        // Build ORDER BY if present
        val orderBySql = query.orderBy?.let { clauses ->
            " ORDER BY " + clauses.joinToString(", ") { clause ->
                val prop = schema.getProperty(clause.field)
                    ?: throw SelOperatorException(
                        "Unknown property '${clause.field}' for entity ${query.target} in orderBy",
                        "$.orderBy"
                    )
                val direction = if (clause.direction == SelOrderDirection.Desc) "DESC" else "ASC"
                "${schema.sqlAlias}.${prop.sqlColumn} $direction"
            }
        } ?: ""

        // Build LIMIT if present
        val limitSql = query.limit?.let { " LIMIT ?" } ?: ""
        val limitParams = query.limit?.let { listOf(it) } ?: emptyList()

        val sql = "SELECT ${schema.sqlAlias}.* FROM ${schema.sqlTable} ${schema.sqlAlias} " +
            "WHERE ${whereFragment.sql}$orderBySql$limitSql"

        return SqlQuery(
            sql = sql,
            params = whereFragment.params + limitParams
        )
    }

    /**
     * Convert a SelNode to SQL.
     *
     * @param node The AST node to convert
     * @param context The SQL context for tracking scopes
     * @param jsonPath The current JSON path for error reporting
     * @return SQL fragment with parameterized query and values
     */
    fun toSql(node: SelNode, context: SelSqlContext, jsonPath: String): SqlFragment {
        return when (node) {
            is SelString -> SqlFragment("?", listOf(node.value))
            is SelNumber -> SqlFragment("?", listOf(node.value))
            is SelBoolean -> SqlFragment("?", listOf(if (node.value) 1 else 0))
            is SelNull -> SqlFragment("NULL")
            is SelArray -> throw SelOperatorException("Array not supported in SQL context", jsonPath)
            is SelQuery -> QueryOperator.buildSubquery(this, node, context, jsonPath)
            is SelOperation -> toSqlOperation(node, context, jsonPath)
        }
    }

    /**
     * Evaluate a single argument node at a specific index.
     */
    fun eval(node: SelNode, context: SelSqlContext, jsonPath: String, index: Int): SqlFragment {
        return toSql(node, context, "$jsonPath[$index]")
    }

    /**
     * Evaluate all nodes and return their SQL fragments.
     */
    fun evalAll(nodes: List<SelNode>, context: SelSqlContext, jsonPath: String): List<SqlFragment> {
        return nodes.mapIndexed { i, node -> eval(node, context, jsonPath, i) }
    }

    private fun toSqlOperation(
        operation: SelOperation,
        context: SelSqlContext,
        jsonPath: String
    ): SqlFragment {
        val operatorPath = "$jsonPath.${operation.operator}"
        val operator = SelOperatorRegistry[operation.operator]
            ?: throw SelOperatorException("Unknown operator: '${operation.operator}'", operatorPath)

        return operator.toSql(this, operation.arguments, context, operatorPath)
    }

    private fun createContext(query: SelQuery): SelSqlContext {
        val schema = SelEntityRegistry.get(query.target)

        // If the query has an alias, register it as a scope
        val initialScopes = if (query.alias != null) {
            mapOf(query.alias to ScopeInfo(query.target, schema.sqlAlias))
        } else {
            emptyMap()
        }

        return SelSqlContext(
            entityType = query.target,
            tableAlias = schema.sqlAlias,
            scopes = initialScopes,
        )
    }
}
