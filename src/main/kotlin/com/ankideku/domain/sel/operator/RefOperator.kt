package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.evaluator.SelSqlContext
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.domain.sel.model.SqlFragment
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelString
import com.ankideku.domain.sel.schema.SelEntityRegistry

/**
 * Scope reference operator.
 *
 * References a property from a named ancestor scope.
 * Used within subqueries to reference parent query's entity properties.
 *
 * Usage: `{ "ref": ["n", "id"] }` → `n.id`
 *
 * Example:
 * ```json
 * {
 *   "target": "Note",
 *   "alias": "n",
 *   "where": { "exists": { "query": {
 *     "target": "Suggestion",
 *     "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] }
 *   }}}
 * }
 * ```
 *
 * This resolves to SQL:
 * ```sql
 * SELECT n.* FROM cached_note n WHERE EXISTS (
 *   SELECT 1 FROM suggestion s WHERE s.note_id = n.id
 * )
 * ```
 */
object RefOperator : SelOperator {
    override val key = "ref"

    override val metadata = SelOperatorMetadata(
        displayName = "Reference",
        category = SelOperatorCategory.Internal,
        description = "Reference a property from a parent scope",
        signature = SelOperatorSignature.binary(SelType.String, SelType.Any),
    )

    override fun toSql(evaluator: SelSqlEvaluator, args: SelArray, context: SelSqlContext, jsonPath: String): SqlFragment {
        requireArgs(args, 2, key, jsonPath)

        val scopeName = (args[0] as? SelString)?.value
            ?: throw SelOperatorException("ref scope must be a string literal", jsonPath)

        val property = (args[1] as? SelString)?.value
            ?: throw SelOperatorException("ref property must be a string literal", jsonPath)

        val scopeInfo = context.getScope(scopeName)
            ?: throw SelOperatorException(
                "Unknown scope '$scopeName'. Available scopes: ${context.scopeNames}",
                jsonPath
            )
        val scopeSchema = SelEntityRegistry[scopeInfo.entityType]
        val prop = scopeSchema.getProperty(property)
            ?: throw SelOperatorException(
                "Unknown property '$property' for entity ${scopeInfo.entityType} in scope '$scopeName'",
                jsonPath
            )
        return SqlFragment("${scopeInfo.tableAlias}.${prop.sqlColumn}")
    }
}
