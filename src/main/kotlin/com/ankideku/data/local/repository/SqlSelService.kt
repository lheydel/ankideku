package com.ankideku.data.local.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.ankideku.data.local.database.Cached_note
import com.ankideku.data.local.database.Field_value
import com.ankideku.data.local.database.History_entry
import com.ankideku.data.local.database.Suggestion as DbSuggestion
import com.ankideku.data.local.database.Session as DbSession
import com.ankideku.data.mapper.FieldContext
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.sel.SelExecutionException
import com.ankideku.domain.sel.SelResult
import com.ankideku.domain.sel.SelService
import com.ankideku.domain.sel.ast.SelParser
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.model.SqlQuery

/**
 * SQL-backed implementation of SelService.
 *
 * Executes SEL queries by:
 * 1. Parsing JSON → AST (via SelParser)
 * 2. Converting AST → SQL (via SelSqlEvaluator)
 * 3. Executing raw SQL against the database
 * 4. Mapping results to domain models with their field values
 *
 * Uses SQLDelight-generated data classes and existing toDomain mappers.
 */
class SqlSelService(
    private val driver: SqlDriver,
) : SelService {

    override fun parseQuery(json: String): SelQuery = SelParser.parseQuery(json)

    override fun execute(query: SelQuery): SelResult {
        val sqlQuery = SelSqlEvaluator.buildQuery(query)

        return try {
            when (query.target) {
                EntityType.Note -> executeNoteQuery(sqlQuery)
                EntityType.Suggestion -> executeSuggestionQuery(sqlQuery)
                EntityType.HistoryEntry -> executeHistoryQuery(sqlQuery)
                EntityType.Session -> executeSessionQuery(sqlQuery)
            }
        } catch (e: Exception) {
            throw SelExecutionException("Failed to execute SEL query: ${e.message}", e)
        }
    }

    // ==================== Query Execution ====================

    private fun executeNoteQuery(sqlQuery: SqlQuery): SelResult.Notes {
        val rows = executeQuery(sqlQuery, ::mapCachedNote)
        if (rows.isEmpty()) return SelResult.Notes(emptyList())

        val fieldsById = fetchFields(rows.map { it.id }, "note_id", FieldContext.NOTE_FIELDS.dbValue)

        return SelResult.Notes(rows.map { row ->
            row.toDomain(fieldsById[row.id] ?: emptyList())
        })
    }

    private fun executeSuggestionQuery(sqlQuery: SqlQuery): SelResult.Suggestions {
        val rows = executeQuery(sqlQuery, ::mapSuggestion)
        if (rows.isEmpty()) return SelResult.Suggestions(emptyList())

        val fieldsById = fetchFields(rows.map { it.id }, "suggestion_id")
        val modelNames = fetchModelNames(rows.map { it.note_id })

        return SelResult.Suggestions(rows.map { row ->
            row.toDomain(fieldsById[row.id] ?: emptyList(), modelNames[row.note_id] ?: "")
        })
    }

    private fun executeHistoryQuery(sqlQuery: SqlQuery): SelResult.HistoryEntries {
        val rows = executeQuery(sqlQuery, ::mapHistoryEntry)
        if (rows.isEmpty()) return SelResult.HistoryEntries(emptyList())

        val fieldsById = fetchFields(rows.map { it.id }, "history_id")
        val modelNames = fetchModelNames(rows.map { it.note_id })

        return SelResult.HistoryEntries(rows.map { row ->
            row.toDomain(fieldsById[row.id] ?: emptyList(), modelNames[row.note_id] ?: "")
        })
    }

    private fun executeSessionQuery(sqlQuery: SqlQuery): SelResult.Sessions {
        val rows = executeQuery(sqlQuery, ::mapSession)
        return SelResult.Sessions(rows.map { it.toDomain() })
    }

    // ==================== Cursor → SQLDelight Type Mappers ====================

    private fun mapCachedNote(c: SqlCursor) = Cached_note(
        id = c.getLong(0)!!,
        deck_id = c.getLong(1)!!,
        deck_name = c.getString(2)!!,
        model_name = c.getString(3)!!,
        tags = c.getString(4)!!,
        mod = c.getLong(5)!!,
        estimated_tokens = c.getLong(6),
        created_at = c.getLong(7)!!,
        updated_at = c.getLong(8)!!,
    )

    private fun mapSuggestion(c: SqlCursor) = DbSuggestion(
        id = c.getLong(0)!!,
        session_id = c.getLong(1)!!,
        note_id = c.getLong(2)!!,
        reasoning = c.getString(3)!!,
        status = c.getString(4)!!,
        created_at = c.getLong(5)!!,
        decided_at = c.getLong(6),
        skipped_at = c.getLong(7),
    )

    private fun mapHistoryEntry(c: SqlCursor) = History_entry(
        id = c.getLong(0)!!,
        session_id = c.getLong(1)!!,
        note_id = c.getLong(2)!!,
        deck_id = c.getLong(3)!!,
        deck_name = c.getString(4)!!,
        action = c.getString(5)!!,
        reasoning = c.getString(6),
        timestamp = c.getLong(7)!!,
    )

    private fun mapSession(c: SqlCursor) = DbSession(
        id = c.getLong(0)!!,
        deck_id = c.getLong(1)!!,
        deck_name = c.getString(2)!!,
        prompt = c.getString(3)!!,
        state = c.getString(4)!!,
        state_message = c.getString(5),
        exit_code = c.getLong(6),
        progress_processed_cards = c.getLong(7)!!,
        progress_total_cards = c.getLong(8)!!,
        progress_processed_batches = c.getLong(9)!!,
        progress_total_batches = c.getLong(10)!!,
        progress_suggestions_count = c.getLong(11)!!,
        progress_input_tokens = c.getLong(12)!!,
        progress_output_tokens = c.getLong(13)!!,
        progress_failed_batches = c.getLong(14)!!,
        created_at = c.getLong(15)!!,
        updated_at = c.getLong(16)!!,
    )

    // ==================== Field Value Fetching ====================

    private fun fetchFields(
        ids: List<Long>,
        fkColumn: String,
        contextFilter: String? = null,
    ): Map<Long, List<Field_value>> {
        if (ids.isEmpty()) return emptyMap()

        val contextClause = contextFilter?.let { " AND context = ?" } ?: ""
        val sql = """
            SELECT id, note_id, suggestion_id, history_id, context, field_name, field_value, field_order
            FROM field_value
            WHERE $fkColumn IN (${ids.placeholders()})$contextClause
        """.trimIndent()

        val params = if (contextFilter != null) ids + contextFilter else ids

        return executeQueryWithParams(sql, params) { c ->
            Field_value(
                id = c.getLong(0)!!,
                note_id = c.getLong(1),
                suggestion_id = c.getLong(2),
                history_id = c.getLong(3),
                context = c.getString(4)!!,
                field_name = c.getString(5)!!,
                field_value = c.getString(6)!!,
                field_order = c.getLong(7)!!,
            )
        }.groupBy { it.note_id ?: it.suggestion_id ?: it.history_id!! }
    }

    private fun fetchModelNames(noteIds: List<Long>): Map<Long, String> {
        if (noteIds.isEmpty()) return emptyMap()
        val sql = "SELECT id, model_name FROM cached_note WHERE id IN (${noteIds.placeholders()})"
        return executeQueryWithParams(sql, noteIds) { c -> c.getLong(0)!! to c.getString(1)!! }.toMap()
    }

    // ==================== Helpers ====================

    private fun List<Long>.placeholders() = joinToString(",") { "?" }

    // ==================== Raw SQL Execution ====================

    private fun <T> executeQuery(sqlQuery: SqlQuery, mapper: (SqlCursor) -> T): List<T> =
        executeQueryWithParams(sqlQuery.sql, sqlQuery.params, mapper)

    private fun <T> executeQueryWithParams(sql: String, params: List<Any?>, mapper: (SqlCursor) -> T): List<T> {
        val result = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val results = mutableListOf<T>()
                while (cursor.next().value) {
                    results.add(mapper(cursor))
                }
                QueryResult.Value(results)
            },
            parameters = params.size,
            binders = { bindParams(params) }
        )
        return result.value
    }

    private fun SqlPreparedStatement.bindParams(params: List<Any?>) {
        params.forEachIndexed { index, value ->
            when (value) {
                null -> bindString(index, null)
                is String -> bindString(index, value)
                is Long -> bindLong(index, value)
                is Int -> bindLong(index, value.toLong())
                is Double -> bindDouble(index, value)
                is Boolean -> bindLong(index, if (value) 1L else 0L)
                is Number -> bindDouble(index, value.toDouble())
                else -> bindString(index, value.toString())
            }
        }
    }

}
