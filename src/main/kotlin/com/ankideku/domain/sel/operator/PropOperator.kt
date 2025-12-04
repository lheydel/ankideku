package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelString
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment

/**
 * Property access operator.
 *
 * Accesses a top-level property (column) of the entity.
 * Uses EntityRegistry for property validation and column mapping.
 *
 * Usage:
 * - `{ "prop": "status" }` - Access "status" property
 * - `{ "prop": "noteId" }` - Access "noteId" property (maps to note_id column)
 */
object PropOperator : SelOperator {
    override val key = "prop"

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 1, key, jsonPath)

        val propName = (args[0] as? SelString)?.value
            ?: throw SelOperatorException("Property name must be a string literal", jsonPath)

        val prop = context.schema.getProperty(propName)
            ?: throw SelOperatorException(
                "Unknown property '$propName' for entity ${context.entityType}. " +
                    "Valid properties: ${context.schema.properties.map { it.selKey }}",
                jsonPath
            )
        return SqlFragment("${context.tableAlias}.${prop.sqlColumn}")
    }
}
