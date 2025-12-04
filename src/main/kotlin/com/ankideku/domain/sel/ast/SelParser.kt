package com.ankideku.domain.sel.ast

import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.QueryOperator
import com.ankideku.util.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Parser that converts JSON strings to SEL AST nodes.
 *
 * SEL (Structured Expression Language) uses a JsonLogic-inspired format where
 * every operation is represented as a single-key JSON object:
 * ```json
 * { "operator": [arg1, arg2, ...] }
 * ```
 *
 * Usage:
 * ```kotlin
 * val node = SelParser.parse("""{ "==": [{ "field": "kanji" }, "日本"] }""")
 * val query = SelParser.parseQuery("""{ "target": "Note", "where": { "isEmpty": { "field": "example" } } }""")
 * ```
 */
object SelParser {
    /**
     * Parse a JSON string into a SelNode AST.
     *
     * @param jsonString The JSON string to parse
     * @return The parsed SelNode
     * @throws SelParseException if the JSON is invalid or doesn't conform to SEL format
     */
    fun parse(jsonString: String): SelNode {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            throw SelParseException("Failed to parse SEL expression: ${e.message}", "$", e)
        }
        return parseElement(root, "$")
    }

    /**
     * Parse a JSON string into a complete SelQuery.
     *
     * Expected format:
     * ```json
     * {
     *   "target": "Note" | "Suggestion" | "HistoryEntry" | "Session",
     *   "where": { ... SEL expression ... },
     *   "alias": "n",  // optional - names the scope for ref access
     *   "result": { "prop": "status" },  // optional - what subquery returns
     *   "orderBy": [{ "field": "...", "direction": "Asc" | "Desc" }],  // optional
     *   "limit": 100  // optional
     * }
     * ```
     *
     * @param jsonString The JSON string to parse
     * @return The parsed SelQuery
     * @throws SelParseException if the JSON is invalid or doesn't conform to query format
     */
    fun parseQuery(jsonString: String): SelQuery {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            throw SelParseException("Failed to parse SEL query: ${e.message}", "$", e)
        }

        if (root !is JsonObject) {
            throw SelParseException("Query must be a JSON object", "$")
        }

        return parseQueryObject(root, "$")
    }

    /**
     * Parse a JsonObject that represents a query.
     * Used for parsing both top-level queries and nested subqueries.
     */
    private fun parseQueryObject(obj: JsonObject, jsonPath: String): SelQuery {
        // Parse target (required)
        val targetElement = obj["target"]
            ?: throw SelParseException("Query must have 'target' field", jsonPath)
        val targetStr = (targetElement as? JsonPrimitive)?.contentOrNull
            ?: throw SelParseException("'target' must be a string", "$jsonPath.target")
        val target = try {
            EntityType.valueOf(targetStr)
        } catch (_: IllegalArgumentException) {
            throw SelParseException(
                "Invalid target: '$targetStr'. Must be one of: ${EntityType.entries.joinToString()}",
                "$jsonPath.target"
            )
        }

        // Parse where (required)
        val whereElement = obj["where"]
            ?: throw SelParseException("Query must have 'where' field", jsonPath)
        val where = parseElement(whereElement, "$jsonPath.where")

        // Parse alias (optional)
        val alias = obj["alias"]?.let { aliasElement ->
            (aliasElement as? JsonPrimitive)?.contentOrNull
                ?: throw SelParseException("'alias' must be a string", "$jsonPath.alias")
        }

        // Parse result (optional) - what the subquery returns
        val result = obj["result"]?.let { resultElement ->
            parseElement(resultElement, "$jsonPath.result")
        }

        // Parse orderBy (optional)
        val orderBy = obj["orderBy"]?.let { orderByElement ->
            val orderByArray = when (orderByElement) {
                is JsonArray -> orderByElement
                else -> JsonArray(listOf(orderByElement))
            }

            orderByArray.mapIndexed { index, element ->
                parseOrderClause(element, "$jsonPath.orderBy[$index]")
            }
        }

        // Parse limit (optional)
        val limit = obj["limit"]?.let { limitElement ->
            val primitive = limitElement as? JsonPrimitive
            primitive?.intOrNull
                ?: throw SelParseException("'limit' must be an integer", "$jsonPath.limit")
        }

        return SelQuery(target, where, alias, result, orderBy, limit)
    }

    private fun parseOrderClause(element: JsonElement, jsonPath: String): SelOrderClause {
        if (element !is JsonObject) {
            throw SelParseException("Order clause must be an object", jsonPath)
        }

        // Support both "field" and "prop" for the property name
        val field = (element["field"] as? JsonPrimitive)?.contentOrNull
            ?: (element["prop"] as? JsonPrimitive)?.contentOrNull
            ?: throw SelParseException("Order clause must have 'field' or 'prop' string", jsonPath)

        // Support both "direction" (Asc/Desc) and "desc" (boolean)
        val direction = when {
            element["desc"]?.let { (it as? JsonPrimitive)?.booleanOrNull } == true -> SelOrderDirection.Desc
            element["direction"] != null -> {
                val dirElement = element["direction"]
                val dirStr = (dirElement as? JsonPrimitive)?.contentOrNull
                    ?: throw SelParseException("'direction' must be a string", "$jsonPath.direction")
                try {
                    SelOrderDirection.valueOf(dirStr)
                } catch (_: IllegalArgumentException) {
                    throw SelParseException(
                        "Invalid direction: '$dirStr'. Must be 'Asc' or 'Desc'",
                        "$jsonPath.direction"
                    )
                }
            }
            else -> SelOrderDirection.Asc
        }

        return SelOrderClause(field, direction)
    }

    /**
     * Parse a JsonElement into a SelNode.
     */
    private fun parseElement(element: JsonElement, jsonPath: String = "$"): SelNode {
        return when (element) {
            is JsonNull -> SelNull
            is JsonPrimitive -> parsePrimitive(element, jsonPath)
            is JsonArray -> parseArray(element, jsonPath)
            is JsonObject -> parseObject(element, jsonPath)
        }
    }

    private fun parsePrimitive(primitive: JsonPrimitive, jsonPath: String): SelPrimitive<*> {
        return when {
            primitive.isString -> SelString(primitive.content)
            primitive == JsonNull -> SelNull
            primitive.doubleOrNull != null -> {
                // Prefer Long for whole numbers, Double for decimals
                val longVal = primitive.longOrNull
                val doubleVal = primitive.doubleOrNull!!
                if (longVal != null && longVal.toDouble() == doubleVal) {
                    SelNumber(longVal)
                } else {
                    SelNumber(doubleVal)
                }
            }
            primitive.booleanOrNull != null -> {
                if (primitive.boolean) SelBoolean.TRUE else SelBoolean.FALSE
            }
            else -> throw SelParseException("Unsupported primitive type: ${primitive.content}", jsonPath)
        }
    }

    private fun parseArray(array: JsonArray, jsonPath: String): SelArray {
        val elements = array.mapIndexed { index, element ->
            parseElement(element, "$jsonPath[$index]")
        }
        return SelArray(elements)
    }

    private fun parseObject(obj: JsonObject, jsonPath: String): SelNode {
        if (obj.keys.size != 1) {
            throw SelParseException(
                "Operation objects must have exactly one key (the operator), found ${obj.keys.size}: ${obj.keys}",
                jsonPath
            )
        }

        val operator = obj.keys.first()
        val argsElement = obj[operator] ?: JsonNull

        // Special case: "query" operator - parse the value as a query
        if (operator == QueryOperator.key) {
            val queryArg = when (argsElement) {
                is JsonObject -> parseQueryObject(argsElement, "$jsonPath.query")
                else -> throw SelParseException(
                    "query operator expects a query object",
                    "$jsonPath.query"
                )
            }
            return SelOperation(operator, SelArray(listOf(queryArg)))
        }

        // Convert arguments to SelArray
        // If the argument is already an array, parse it as such
        // If it's a single value, wrap it in a single-element array
        val arguments = when (val parsed = parseElement(argsElement, "$jsonPath.$operator")) {
            is SelArray -> parsed
            else -> SelArray(listOf(parsed))
        }

        return SelOperation(operator, arguments)
    }
}
