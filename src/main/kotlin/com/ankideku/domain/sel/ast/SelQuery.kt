package com.ankideku.domain.sel.ast

import com.ankideku.domain.sel.model.EntityType
import kotlinx.serialization.Serializable

/**
 * Root query object for SEL.
 *
 * A complete query specifies:
 * - `target`: The entity type to query (Note, Suggestion, HistoryEntry, Session)
 * - `where`: The filter condition (a SelNode expression that evaluates to boolean)
 * - `alias`: Optional named scope for inner queries to reference via `ref`
 * - `result`: Optional expression defining what the subquery returns (null = SELECT 1 for exists)
 * - `orderBy`: Optional ordering clauses
 * - `limit`: Optional maximum number of results
 *
 * SelQuery implements SelNode so it can be used as a subquery.
 *
 * Example JSON:
 * ```json
 * {
 *   "target": "Note",
 *   "alias": "n",
 *   "where": { "isEmpty": { "field": "example" } },
 *   "orderBy": [{ "field": "mod", "direction": "Desc" }],
 *   "limit": 100
 * }
 * ```
 *
 * For subqueries with result:
 * ```json
 * {
 *   "query": {
 *     "target": "Suggestion",
 *     "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
 *     "result": { "prop": "status" },
 *     "orderBy": [{ "prop": "createdAt", "desc": true }],
 *     "limit": 1
 *   }
 * }
 * ```
 */
data class SelQuery(
    val target: EntityType,
    val where: SelNode,
    val alias: String? = null,
    val result: SelNode? = null,
    val orderBy: List<SelOrderClause>? = null,
    val limit: Int? = null
) : SelNode

/**
 * Defines ordering for query results.
 */
@Serializable
data class SelOrderClause(
    val field: String,
    val direction: SelOrderDirection = SelOrderDirection.Asc
)

/**
 * Ordering direction for query results.
 */
@Serializable
enum class SelOrderDirection {
    Asc,
    Desc
}
