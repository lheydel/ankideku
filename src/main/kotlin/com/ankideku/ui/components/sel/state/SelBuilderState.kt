package com.ankideku.ui.components.sel.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ankideku.domain.sel.ast.*
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.ScopeType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.util.json
import kotlinx.serialization.json.*

/**
 * Scope value with optional lock state.
 *
 * @param value The scope value (e.g., deck ID, session ID)
 * @param displayLabel Human-readable label for the value (e.g., deck name)
 * @param locked If true, this scope was set by the caller and cannot be changed
 */
data class ScopeValue(
    val value: Any,
    val displayLabel: String,
    val locked: Boolean = false,
)

/**
 * Breadcrumb entry for subquery navigation.
 */
data class BreadcrumbEntry(
    val label: String,
    val subquery: SubqueryState?,
)

/**
 * Main state holder for the SEL query builder UI.
 */
@Stable
class SelBuilderState(
    initialTarget: EntityType = EntityType.Note,
    initialScopes: Map<String, ScopeValue> = emptyMap(),
    initialAlias: String = "root",
) {
    var target by mutableStateOf(initialTarget)
    var alias by mutableStateOf(initialAlias)
    var rootGroup by mutableStateOf(ConditionGroupState())
    var orderBy by mutableStateOf<List<SelOrderClause>>(emptyList())
    var limit by mutableStateOf<Int?>(null)

    /** Scope values keyed by scope key (e.g., "deck" -> ScopeValue(123, "My Deck", locked=true)) */
    var scopes by mutableStateOf(initialScopes)

    /** Navigation stack for subquery editing. Empty = editing root query. */
    var navigationStack by mutableStateOf<List<SubqueryState>>(emptyList())

    /** The currently active subquery being edited, or null if editing root */
    val currentSubquery: SubqueryState? get() = navigationStack.lastOrNull()

    /** The current condition group being edited (root or subquery's) */
    val currentGroup: ConditionGroupState get() = currentSubquery?.rootGroup ?: rootGroup

    /** The current entity type being queried */
    val currentTarget: EntityType get() = currentSubquery?.target ?: target

    /**
     * Parent entity types available for property references in current context.
     * Returns list of (alias, EntityType) pairs for each level above current.
     * Empty when at root level. First entry is immediate parent, etc.
     * Uses actual aliases from the query/subquery chain for ref operator compatibility.
     */
    val parentScopes: List<Pair<String, EntityType>> get() = buildList {
        if (navigationStack.isEmpty()) return@buildList

        // Build scopes from root to current (excluding current)
        // Each level uses the actual alias defined on that query/subquery
        add(alias to target) // Root query
        navigationStack.dropLast(1).forEach { sq ->
            add(sq.alias to sq.target)
        }
        // Reverse so immediate parent is first
        reverse()
    }

    /** Breadcrumb path for navigation UI */
    val breadcrumbs: List<BreadcrumbEntry> get() = buildList {
        // Root level with alias
        add(BreadcrumbEntry("$alias: ${target.name}", null))
        // Each subquery level with its alias
        navigationStack.forEach { sq ->
            add(BreadcrumbEntry("${sq.alias}: ${sq.toPreviewText()}", sq))
        }
    }

    /** Navigate into a subquery for editing */
    fun navigateToSubquery(subquery: SubqueryState) {
        navigationStack = navigationStack + subquery
    }

    /** Navigate back to a specific breadcrumb level (0 = root) */
    fun navigateTo(index: Int) {
        navigationStack = if (index <= 0) emptyList() else navigationStack.take(index)
    }

    /** Navigate back one level */
    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    /**
     * Build the complete SelQuery from current UI state.
     * Scope values are added as equality conditions to the where clause.
     */
    fun toSelQuery(): SelQuery {
        val schema = SelEntityRegistry[target]
        val whereNode = rootGroup.toSelNode(alias)

        // Build scope conditions
        val scopeConditions = scopes.mapNotNull { (scopeKey, scopeValue) ->
            val scope = schema.scopes.find { it.key == scopeKey } ?: return@mapNotNull null

            when (scope.type) {
                ScopeType.Deck -> {
                    // Deck scope includes subdecks: deckName == "Parent" OR startsWith(deckName, "Parent::")
                    val deckName = scopeValue.displayLabel
                    val deckNameProp = SelOperation("prop", SelArray(listOf(SelString("deckName"))))
                    val exactMatch = SelOperation("==", SelArray(listOf(deckNameProp, SelString(deckName))))
                    val subdeckMatch = SelOperation("startsWith", SelArray(listOf(
                        deckNameProp,
                        SelString("$deckName::")
                    )))
                    SelOperation("or", SelArray(listOf(exactMatch, subdeckMatch)))
                }
                ScopeType.Session -> {
                    // Session scope uses direct ID matching
                    SelOperation("==", SelArray(listOf(
                        SelOperation("prop", SelArray(listOf(SelString(scope.propertyKey)))),
                        when (val v = scopeValue.value) {
                            is String -> SelString(v)
                            is Number -> SelNumber(v)
                            is Boolean -> if (v) SelBoolean.TRUE else SelBoolean.FALSE
                            else -> SelString(v.toString())
                        }
                    )))
                }
            }
        }

        // Combine scope conditions with user conditions
        val finalWhere = when {
            scopeConditions.isEmpty() -> whereNode
            whereNode == SelBoolean.TRUE -> {
                if (scopeConditions.size == 1) scopeConditions.first()
                else SelOperation("and", SelArray(scopeConditions))
            }
            else -> SelOperation("and", SelArray(scopeConditions + whereNode))
        }

        return SelQuery(
            target = target,
            where = finalWhere,
            alias = alias,
            orderBy = orderBy.takeIf { it.isNotEmpty() },
            limit = limit,
        )
    }

    /**
     * Build the JSON representation of the current query.
     */
    fun toJson(): String = toSelQuery().toJson()

    /**
     * Generate a natural language preview of the query.
     */
    fun toPreviewText(): String {
        val schema = SelEntityRegistry[target]
        val scopeText = scopes.entries.joinToString(" ") { (key, value) ->
            val scope = schema.scopes.find { it.key == key }
            "[${scope?.displayName ?: key}: ${value.displayLabel}]"
        }
        val scopePrefix = if (scopeText.isNotEmpty()) "$scopeText " else ""
        val limitText = limit?.let { " LIMIT $it" } ?: ""
        return "${target.name} ${scopePrefix}where ${rootGroup.toPreviewText()}$limitText"
    }

    /**
     * Reset to default state (preserves locked scopes).
     */
    fun reset() {
        rootGroup = ConditionGroupState()
        orderBy = emptyList()
        limit = null
        // Keep locked scopes, clear unlocked ones
        scopes = scopes.filterValues { it.locked }
    }

    /**
     * Create a copy of this state.
     */
    fun copy(): SelBuilderState = SelBuilderState(
        initialTarget = target,
        initialScopes = scopes,
    ).also { copy ->
        copy.alias = alias
        copy.rootGroup = rootGroup.copy()
        copy.orderBy = orderBy
        copy.limit = limit
    }

    /**
     * Load state from a JSON query string.
     * Parses the query and populates the builder state.
     *
     * Currently loads: target, alias, orderBy, limit, and basic conditions.
     * Complex nested expressions are converted to a default empty state.
     */
    fun loadFromJson(queryJson: String) {
        try {
            val root = json.parseToJsonElement(queryJson)
            if (root !is JsonObject) return

            // Load target
            root["target"]?.jsonPrimitive?.contentOrNull?.let { targetStr ->
                target = try {
                    EntityType.valueOf(targetStr)
                } catch (_: Exception) {
                    target
                }
            }

            // Load alias
            alias = root["alias"]?.jsonPrimitive?.contentOrNull ?: "root"

            // Load limit
            limit = root["limit"]?.jsonPrimitive?.intOrNull

            // Load orderBy
            root["orderBy"]?.jsonArray?.let { orderByArray ->
                orderBy = orderByArray.mapNotNull { clause ->
                    val clauseObj = clause as? JsonObject ?: return@mapNotNull null
                    val field = clauseObj["field"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val direction = clauseObj["direction"]?.jsonPrimitive?.contentOrNull?.let {
                        try { SelOrderDirection.valueOf(it) } catch (_: Exception) { SelOrderDirection.Asc }
                    } ?: SelOrderDirection.Asc
                    SelOrderClause(field, direction)
                }
            }

            // Load where clause - attempt to convert to UI state
            root["where"]?.let { whereElement ->
                rootGroup = parseWhereToGroup(whereElement) ?: ConditionGroupState()
            }

            // Clear navigation stack (we're loading a root query)
            navigationStack = emptyList()

        } catch (_: Exception) {
            // If parsing fails, just reset to default
            reset()
        }
    }

    /**
     * Attempt to parse a where clause JSON element into a ConditionGroupState.
     * Returns null if the structure is too complex to represent.
     */
    private fun parseWhereToGroup(element: JsonElement): ConditionGroupState? {
        if (element !is JsonObject) return null
        if (element.keys.size != 1) return null

        val operator = element.keys.first()
        val args = element[operator]

        // Check if it's an AND/OR group
        if (operator == "and" || operator == "or") {
            val argsArray = args as? JsonArray ?: return null
            val items = argsArray.mapNotNull { parseWhereItem(it) }
            if (items.isEmpty()) return null

            return ConditionGroupState(initialJoinOperator = operator).also { group ->
                group.items = items
            }
        }

        // Single condition - wrap in a group
        val condition = parseCondition(element) ?: return null
        return ConditionGroupState().also { group ->
            group.items = listOf(GroupItem.Condition(condition))
        }
    }

    /**
     * Parse a where item - could be a condition or nested group.
     */
    private fun parseWhereItem(element: JsonElement): GroupItem? {
        if (element !is JsonObject) return null
        if (element.keys.size != 1) return null

        val operator = element.keys.first()

        // Check if it's a nested AND/OR group
        if (operator == "and" || operator == "or") {
            val group = parseWhereToGroup(element) ?: return null
            return GroupItem.NestedGroup(group)
        }

        // Otherwise it's a condition
        val condition = parseCondition(element) ?: return null
        return GroupItem.Condition(condition)
    }

    /**
     * Parse a single condition from JSON.
     */
    private fun parseCondition(obj: JsonObject): ConditionState? {
        if (obj.keys.size != 1) return null
        val operator = obj.keys.first()
        val args = obj[operator]

        // Skip and/or (they're groups, not conditions)
        if (operator == "and" || operator == "or") return null

        // Handle exists/not(exists) - these are subquery conditions that serialize
        // as the subquery result type, not as an operator with operands
        if (operator == "exists") {
            val subqueryOperand = parseExistsSubquery(args, negated = false)
            if (subqueryOperand != null) {
                // Create a condition with "==" true, where the operand is the subquery
                // The subquery's resultType handles the exists wrapper
                return ConditionState(initialOperator = "==").also { cond ->
                    cond.operands = listOf(
                        subqueryOperand,
                        OperandState(initialType = OperandType.Value, initialValue = "true")
                    )
                }
            }
        }

        if (operator == "not") {
            // Check if it's not(exists(...))
            val argsArray = args as? JsonArray
            val innerElement = argsArray?.getOrNull(0) as? JsonObject
            if (innerElement?.keys?.firstOrNull() == "exists") {
                val existsArgs = innerElement["exists"]
                val subqueryOperand = parseExistsSubquery(existsArgs, negated = true)
                if (subqueryOperand != null) {
                    return ConditionState(initialOperator = "==").also { cond ->
                        cond.operands = listOf(
                            subqueryOperand,
                            OperandState(initialType = OperandType.Value, initialValue = "true")
                        )
                    }
                }
            }
        }

        val condition = ConditionState(initialOperator = operator)

        // Parse arguments
        val argsList = when (args) {
            is JsonArray -> args.mapNotNull { parseOperand(it) }
            else -> listOfNotNull(parseOperand(args))
        }

        if (argsList.isEmpty()) return null
        condition.operands = argsList

        return condition
    }

    /**
     * Parse an operand from JSON.
     */
    private fun parseOperand(element: JsonElement?): OperandState? {
        if (element == null) return null

        return when (element) {
            is JsonPrimitive -> {
                // Literal value
                OperandState(
                    initialType = OperandType.Value,
                    initialValue = element.contentOrNull ?: element.toString(),
                )
            }
            is JsonObject -> {
                if (element.keys.size != 1) return null
                val op = element.keys.first()
                val args = element[op]

                when (op) {
                    "field" -> parseFieldOperand(args)
                    "prop" -> parsePropOperand(args)
                    "ref" -> parseRefOperand(args)
                    "exists" -> parseExistsSubquery(args, negated = false)
                    "not" -> parseNotOperand(args)
                    "query" -> parseQuerySubquery(args)
                    else -> {
                        // Expression operand
                        val exprArgs = when (args) {
                            is JsonArray -> args.mapNotNull { parseOperand(it) }
                            else -> listOfNotNull(parseOperand(args))
                        }
                        OperandState(initialType = OperandType.Expression).also { operand ->
                            operand.expression = ExpressionState(initialOperator = op).also { expr ->
                                expr.operands = exprArgs
                            }
                        }
                    }
                }
            }
            else -> null
        }
    }

    private fun parseFieldOperand(args: JsonElement?): OperandState? {
        return when (args) {
            is JsonPrimitive -> {
                OperandState(
                    initialType = OperandType.Field,
                    initialFieldName = args.contentOrNull ?: "",
                )
            }
            is JsonArray -> {
                val name = args.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: ""
                val context = args.getOrNull(1)?.jsonPrimitive?.contentOrNull
                OperandState(
                    initialType = OperandType.Field,
                    initialFieldName = name,
                    initialFieldContext = context,
                )
            }
            else -> null
        }
    }

    private fun parsePropOperand(args: JsonElement?): OperandState? {
        val propName = when (args) {
            is JsonPrimitive -> args.contentOrNull
            is JsonArray -> args.getOrNull(0)?.jsonPrimitive?.contentOrNull
            else -> null
        } ?: return null

        return OperandState(
            initialType = OperandType.Property,
            initialPropertyName = propName,
        )
    }

    private fun parseRefOperand(args: JsonElement?): OperandState? {
        val argsArray = args as? JsonArray ?: return null
        val scope = argsArray.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: return null
        val propName = argsArray.getOrNull(1)?.jsonPrimitive?.contentOrNull ?: return null

        return OperandState(
            initialType = OperandType.Property,
            initialPropertyName = propName,
            initialPropertyScope = scope,
        )
    }

    /**
     * Parse not(...) - check if it's not(exists(...)) for NotExists subquery,
     * otherwise treat as regular expression.
     */
    private fun parseNotOperand(args: JsonElement?): OperandState? {
        val argsArray = args as? JsonArray ?: return null
        val innerElement = argsArray.getOrNull(0) as? JsonObject ?: return null

        // Check if it's not(exists(...))
        if (innerElement.keys.firstOrNull() == "exists") {
            val existsArgs = innerElement["exists"]
            return parseExistsSubquery(existsArgs, negated = true)
        }

        // Otherwise treat as regular not expression
        val exprArgs = argsArray.mapNotNull { parseOperand(it) }
        return OperandState(initialType = OperandType.Expression).also { operand ->
            operand.expression = ExpressionState(initialOperator = "not").also { expr ->
                expr.operands = exprArgs
            }
        }
    }

    /**
     * Parse exists(query(...)) into a Subquery operand.
     *
     * JSON structure: { "exists": { "query": { "target": ..., "where": ... } } }
     * So args is { "query": {...} }
     */
    private fun parseExistsSubquery(args: JsonElement?, negated: Boolean): OperandState? {
        // args can be either:
        // - JsonObject: { "query": {...} } (raw JSON structure)
        // - JsonArray: [{ "query": {...} }] (SelArray structure)
        val queryWrapper = when (args) {
            is JsonObject -> args
            is JsonArray -> args.getOrNull(0) as? JsonObject ?: return null
            else -> return null
        }

        // Should have "query" key
        if (queryWrapper.keys.firstOrNull() != "query") return null

        // The query value can be:
        // - JsonObject: the query directly (raw JSON)
        // - JsonArray: [queryObj] (SelArray structure)
        val queryObj = when (val queryValue = queryWrapper["query"]) {
            is JsonObject -> queryValue
            is JsonArray -> queryValue.getOrNull(0) as? JsonObject ?: return null
            else -> return null
        }

        val subquery = parseSubqueryObject(queryObj) ?: return null
        subquery.resultType = if (negated) SubqueryResultType.NotExists else SubqueryResultType.Exists

        return OperandState(initialType = OperandType.Subquery).also { operand ->
            operand.subquery = subquery
        }
    }

    /**
     * Parse query(...) directly - for ScalarProperty or Count result types.
     *
     * JSON structure: { "query": { "target": ..., "where": ..., "result": ... } }
     */
    private fun parseQuerySubquery(args: JsonElement?): OperandState? {
        // args can be either:
        // - JsonObject: the query directly (raw JSON)
        // - JsonArray: [queryObj] (SelArray structure)
        val queryObj = when (args) {
            is JsonObject -> args
            is JsonArray -> args.getOrNull(0) as? JsonObject ?: return null
            else -> return null
        }

        val subquery = parseSubqueryObject(queryObj) ?: return null

        return OperandState(initialType = OperandType.Subquery).also { operand ->
            operand.subquery = subquery
        }
    }

    /**
     * Parse the inner query object into a SubqueryState.
     */
    private fun parseSubqueryObject(queryObj: JsonObject): SubqueryState? {
        // Parse target
        val targetStr = queryObj["target"]?.jsonPrimitive?.contentOrNull ?: return null
        val target = try {
            EntityType.valueOf(targetStr)
        } catch (_: Exception) {
            return null
        }

        // Parse alias
        val alias = queryObj["alias"]?.jsonPrimitive?.contentOrNull

        val subquery = SubqueryState(
            initialTarget = target,
            initialAlias = alias,
        )

        // Parse limit
        subquery.limit = queryObj["limit"]?.jsonPrimitive?.intOrNull

        // Parse orderBy
        queryObj["orderBy"]?.jsonArray?.let { orderByArray ->
            subquery.orderBy = orderByArray.mapNotNull { clause ->
                val clauseObj = clause as? JsonObject ?: return@mapNotNull null
                val field = clauseObj["field"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val direction = clauseObj["direction"]?.jsonPrimitive?.contentOrNull?.let {
                    try { SelOrderDirection.valueOf(it) } catch (_: Exception) { SelOrderDirection.Asc }
                } ?: SelOrderDirection.Asc
                SelOrderClause(field, direction)
            }
        }

        // Parse result to determine resultType
        queryObj["result"]?.let { resultElement ->
            if (resultElement is JsonObject && resultElement.keys.size == 1) {
                val resultOp = resultElement.keys.first()
                when (resultOp) {
                    "count" -> {
                        subquery.resultType = SubqueryResultType.Count
                    }
                    "prop" -> {
                        subquery.resultType = SubqueryResultType.ScalarProperty
                        val propArgs = resultElement["prop"]
                        subquery.resultProperty = when (propArgs) {
                            is JsonPrimitive -> propArgs.contentOrNull ?: ""
                            is JsonArray -> propArgs.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: ""
                            else -> ""
                        }
                    }
                }
            }
        }

        // Parse where clause
        queryObj["where"]?.let { whereElement ->
            subquery.rootGroup = parseWhereToGroup(whereElement) ?: ConditionGroupState()
        }

        return subquery
    }
}
