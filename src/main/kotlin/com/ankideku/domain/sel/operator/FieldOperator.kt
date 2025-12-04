package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelString

/**
 * Field access operator.
 *
 * Accesses a field value from the entity using a scalar subquery.
 * Uses subqueries instead of JOINs to avoid row duplication.
 *
 * Usage:
 * - `{ "field": ["example", "changes"] }` - Access field "example" from "changes" context
 * - `{ "field": ["example", ["edited", "changes", "original"]] }` - Priority fallback
 *
 * Valid contexts by entity (see FieldValueMapper.kt):
 * - Notes: "fields"
 * - Suggestions: "original", "changes", "edited"
 * - HistoryEntry: "original", "aiChanges", "applied", "userEdits"
 */
object FieldOperator : SelOperator {
    override val key = "field"

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)

        val fieldName = (args[0] as? SelString)?.value
            ?: throw SelOperatorException("field name must be a string literal", jsonPath)
        val contextArg = args[1]

        return when (contextArg) {
            is SelString -> {
                // { "field": ["example", "changes"] } - single context
                buildFieldSubquery(fieldName, contextArg.value, context, jsonPath)
            }
            is SelArray -> {
                // { "field": ["example", ["editedChanges", "changes", "original"]] } - priority
                val contexts = contextArg.map { node ->
                    (node as? SelString)?.value
                        ?: throw SelOperatorException(
                            "Field context array must contain only strings",
                            jsonPath
                        )
                }
                if (contexts.isEmpty()) {
                    throw SelOperatorException(
                        "Field context array must not be empty",
                        jsonPath
                    )
                }
                buildFieldSubqueryWithPriority(fieldName, contexts, context, jsonPath)
            }
            else -> throw SelOperatorException(
                "Field context must be a string or array of strings",
                jsonPath
            )
        }
    }

    /**
     * Generate a scalar subquery to access a field value.
     *
     * Uses a subquery instead of JOIN to avoid row duplication.
     */
    private fun buildFieldSubquery(
        fieldName: String,
        contextSelKey: String,
        context: SelSqlContext,
        jsonPath: String
    ): SqlFragment {
        val schema = context.schema
        val fieldContext = schema.getFieldContext(contextSelKey)
            ?: throw SelOperatorException(
                "Field context '$contextSelKey' is not valid for entity ${context.entityType}. " +
                    "Valid contexts: ${schema.fieldContexts.map { it.selKey }}",
                jsonPath
            )

        val fkColumn = schema.fieldValueFkColumn
            ?: throw SelOperatorException(
                "${context.entityType} entity does not support field access",
                jsonPath
            )

        return SqlFragment(
            "(SELECT fv.field_value FROM field_value fv " +
                "WHERE fv.$fkColumn = ${context.tableAlias}.id " +
                "AND fv.context = ? AND fv.field_name = ?)",
            listOf(fieldContext.sqlValue, fieldName)
        )
    }

    /**
     * Generate a scalar subquery with priority fallback for multiple contexts.
     *
     * Returns the first non-null value from the ordered list of contexts.
     */
    private fun buildFieldSubqueryWithPriority(
        fieldName: String,
        contextSelKeys: List<String>,
        context: SelSqlContext,
        jsonPath: String
    ): SqlFragment {
        val schema = context.schema

        // Validate all contexts
        val fieldContexts = contextSelKeys.map { selKey ->
            schema.getFieldContext(selKey)
                ?: throw SelOperatorException(
                    "Field context '$selKey' is not valid for entity ${context.entityType}. " +
                        "Valid contexts: ${schema.fieldContexts.map { it.selKey }}",
                    jsonPath
                )
        }

        val fkColumn = schema.fieldValueFkColumn
            ?: throw SelOperatorException(
                "${context.entityType} entity does not support field access",
                jsonPath
            )

        val contextSqlValues = fieldContexts.map { it.sqlValue }

        // Build ORDER BY CASE for priority
        val orderByCase = contextSqlValues.mapIndexed { index, sqlValue ->
            "WHEN '$sqlValue' THEN ${index + 1}"
        }.joinToString(" ")

        // Build IN clause placeholders
        val inPlaceholders = contextSqlValues.joinToString(", ") { "?" }

        return SqlFragment(
            "(SELECT fv.field_value FROM field_value fv " +
                "WHERE fv.$fkColumn = ${context.tableAlias}.id " +
                "AND fv.field_name = ? " +
                "AND fv.context IN ($inPlaceholders) " +
                "ORDER BY CASE fv.context $orderByCase END " +
                "LIMIT 1)",
            listOf(fieldName) + contextSqlValues
        )
    }
}
