package com.ankideku.domain.sel.model

/**
 * Result of SQL generation containing the SQL fragment and its parameters.
 */
data class SqlFragment(
    val sql: String,
    val params: List<Any?> = emptyList()
) {
    operator fun plus(other: SqlFragment): SqlFragment =
        SqlFragment(sql + other.sql, params + other.params)
}

/**
 * Complete SQL query with parameterized statement and values.
 */
data class SqlQuery(
    val sql: String,
    val params: List<Any?>
)
