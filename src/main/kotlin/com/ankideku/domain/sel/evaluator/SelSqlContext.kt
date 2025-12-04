package com.ankideku.domain.sel.evaluator

import com.ankideku.domain.sel.SelException
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.domain.sel.schema.EntitySchema
import kotlinx.serialization.json.Json

/**
 * Information about a named scope from an ancestor query.
 */
data class ScopeInfo(
    val entityType: EntityType,
    val tableAlias: String,
)

/**
 * Context for SQL generation.
 *
 * Tracks the current entity type, table alias, and named scopes from ancestor queries.
 *
 * @param entityType The target entity type (Note, Suggestion, HistoryEntry, Session)
 * @param tableAlias The SQL alias for the current entity's table
 * @param scopes Named scopes from ancestor queries (alias -> ScopeInfo)
 */
class SelSqlContext(
    val entityType: EntityType,
    val tableAlias: String,
    private val scopes: Map<String, ScopeInfo> = emptyMap(),
) {
    val schema: EntitySchema get() = SelEntityRegistry[entityType]
    val scopeNames: List<String> get() = scopes.keys.toList()

    /**
     * Create a child context for a subquery.
     *
     * If the subquery has an alias, the current context is registered as a named scope
     * that the subquery can reference via `ref`.
     *
     * @param subquery The subquery being entered
     * @return A new context for evaluating the subquery's WHERE clause
     */
    fun childContext(subquery: SelQuery, jsonPath: String): SelSqlContext {
        val childSchema = SelEntityRegistry[subquery.target]
        val newScopes = if (subquery.alias != null) {
            if (subquery.alias in scopes) {
                throw SelException("Duplicate alias '${subquery.alias}' - already used by an ancestor query", "$jsonPath.alias")
            }
            scopes + (subquery.alias to ScopeInfo(entityType, tableAlias))
        } else {
            scopes
        }
        return SelSqlContext(
            entityType = subquery.target,
            tableAlias = childSchema.sqlAlias,
            scopes = newScopes,
        )
    }

    /**
     * Get scope info by name.
     */
    fun getScope(name: String): ScopeInfo? = scopes[name]
}
