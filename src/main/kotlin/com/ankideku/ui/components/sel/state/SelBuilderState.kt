package com.ankideku.ui.components.sel.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ankideku.domain.sel.ast.*
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.ScopeType
import com.ankideku.domain.sel.schema.SelEntityRegistry

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
}
