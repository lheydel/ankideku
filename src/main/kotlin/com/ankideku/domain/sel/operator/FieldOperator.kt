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
 * - `{ "field": ["*", "context"] }` - All fields in context (concatenated, for search)
 * - `{ "field": ["*", "*"] }` - All fields in all contexts (concatenated, for search)
 * - `{ "field": ["fieldName", "*"] }` - Specific field across all contexts
 *
 * Valid contexts by entity (see FieldValueMapper.kt):
 * - Notes: "fields"
 * - Suggestions: "original", "changes", "edited"
 * - HistoryEntry: "original", "aiChanges", "applied", "userEdits"
 */
object FieldOperator : SelOperator {
    override val key = "field"
    private const val WILDCARD = "*"

    override val metadata = SelOperatorMetadata(
        displayName = "Field",
        category = SelOperatorCategory.Internal,
        description = "Access a field value from the entity",
        signature = SelOperatorSignature(
            minArgs = 2,
            maxArgs = 2,
            argTypes = listOf(SelType.String, SelType.String),
            returnType = SelType.String,
        ),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)

        val fieldName = (args[0] as? SelString)?.value
            ?: throw SelOperatorException("field name must be a string literal", jsonPath)
        val contextArg = args[1]

        return when (contextArg) {
            is SelString -> {
                val contextSelKey = contextArg.value
                if (fieldName == WILDCARD || contextSelKey == WILDCARD) {
                    // Wildcard mode - use GROUP_CONCAT for searching
                    buildWildcardSubquery(
                        fieldName = fieldName.takeUnless { it == WILDCARD },
                        contextSelKey = contextSelKey.takeUnless { it == WILDCARD },
                        context = context,
                        jsonPath = jsonPath,
                    )
                } else {
                    // { "field": ["example", "changes"] } - single context
                    buildFieldSubquery(fieldName, contextSelKey, context, jsonPath)
                }
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
                if (fieldName == WILDCARD) {
                    // Wildcard field with multiple contexts
                    buildWildcardSubquery(
                        fieldName = null,
                        contextSelKey = null,
                        contextSelKeys = contexts,
                        context = context,
                        jsonPath = jsonPath,
                    )
                } else {
                    buildFieldSubqueryWithPriority(fieldName, contexts, context, jsonPath)
                }
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

    /**
     * Generate a wildcard subquery using GROUP_CONCAT for searching across multiple fields/contexts.
     *
     * This allows patterns like:
     * - `{ "field": ["*", "*"] }` - All fields in all contexts
     * - `{ "field": ["*", "original"] }` - All fields in a specific context
     * - `{ "field": ["Front", "*"] }` - Specific field across all contexts
     * - `{ "field": ["*", ["original", "changes"]] }` - All fields in multiple contexts
     *
     * Returns a concatenation of all matching field values, suitable for LIKE/contains searches.
     */
    private fun buildWildcardSubquery(
        fieldName: String?,
        contextSelKey: String?,
        contextSelKeys: List<String>? = null,
        context: SelSqlContext,
        jsonPath: String,
    ): SqlFragment {
        val schema = context.schema
        val fkColumn = schema.fieldValueFkColumn
            ?: throw SelOperatorException(
                "${context.entityType} entity does not support field access",
                jsonPath
            )

        val conditions = mutableListOf("fv.$fkColumn = ${context.tableAlias}.id")
        val params = mutableListOf<Any>()

        // Add field name filter if specified
        if (fieldName != null) {
            conditions.add("fv.field_name = ?")
            params.add(fieldName)
        }

        // Add context filter(s) if specified
        when {
            contextSelKey != null -> {
                val fieldContext = schema.getFieldContext(contextSelKey)
                    ?: throw SelOperatorException(
                        "Field context '$contextSelKey' is not valid for entity ${context.entityType}. " +
                            "Valid contexts: ${schema.fieldContexts.map { it.selKey }}",
                        jsonPath
                    )
                conditions.add("fv.context = ?")
                params.add(fieldContext.sqlValue)
            }
            contextSelKeys != null -> {
                val fieldContexts = contextSelKeys.map { selKey ->
                    schema.getFieldContext(selKey)
                        ?: throw SelOperatorException(
                            "Field context '$selKey' is not valid for entity ${context.entityType}. " +
                                "Valid contexts: ${schema.fieldContexts.map { it.selKey }}",
                            jsonPath
                        )
                }
                val placeholders = fieldContexts.joinToString(", ") { "?" }
                conditions.add("fv.context IN ($placeholders)")
                params.addAll(fieldContexts.map { it.sqlValue })
            }
            // If neither specified (both "*"), no context filter - search all contexts
        }

        val whereClause = conditions.joinToString(" AND ")

        return SqlFragment(
            "(SELECT GROUP_CONCAT(fv.field_value, ' ') FROM field_value fv WHERE $whereClause)",
            params
        )
    }
}
