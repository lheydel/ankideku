package com.ankideku.ui.components.sel.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ankideku.domain.sel.ast.*
import com.ankideku.domain.sel.operator.SelOperatorMetadata
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import java.util.UUID

/**
 * Represents a single condition in the UI builder.
 */
@Stable
class ConditionState(
    initialOperator: String = "==",
) {
    val id: String = UUID.randomUUID().toString()
    var operator by mutableStateOf(initialOperator)
    var operands by mutableStateOf(listOf(OperandState(), OperandState()))

    val operatorMetadata: SelOperatorMetadata? get() = SelOperatorRegistry.getMetadata(operator)

    fun setOperandCount(count: Int) {
        val current = operands.toMutableList()
        while (current.size < count) current.add(OperandState())
        while (current.size > count) current.removeLast()
        operands = current
    }

    fun toSelNode(parentAlias: String? = null): SelNode {
        val args = SelArray(operands.map { it.toSelNode(parentAlias) })
        return SelOperation(operator, args)
    }

    fun copy(): ConditionState = ConditionState(initialOperator = operator).also { copy ->
        copy.operands = operands.map { it.copy() }
    }

    fun toPreviewText(): String {
        val op = operatorMetadata?.displayName ?: operator
        return when (operands.size) {
            1 -> "${operands[0].toPreviewText()} $op"
            2 -> "${operands[0].toPreviewText()} $op ${operands[1].toPreviewText()}"
            else -> operands.joinToString(" $op ") { it.toPreviewText() }
        }
    }
}

/**
 * Item in a condition group - either a condition or a nested group.
 */
sealed interface GroupItem {
    val id: String
    fun toSelNode(parentAlias: String?): SelNode
    fun copy(): GroupItem
    fun toPreviewText(): String

    @Stable
    class Condition(val state: ConditionState) : GroupItem {
        override val id: String get() = state.id
        override fun toSelNode(parentAlias: String?): SelNode = state.toSelNode(parentAlias)
        override fun copy(): GroupItem = Condition(state.copy())
        override fun toPreviewText(): String = state.toPreviewText()
    }

    @Stable
    class NestedGroup(val state: ConditionGroupState) : GroupItem {
        override val id: String get() = state.id
        override fun toSelNode(parentAlias: String?): SelNode = state.toSelNode(parentAlias)
        override fun copy(): GroupItem = NestedGroup(state.copy())
        override fun toPreviewText(): String = state.toPreviewText()
    }
}

/**
 * Represents a group of conditions/nested groups joined by AND/OR.
 * Supports recursive nesting for complex boolean logic.
 */
@Stable
class ConditionGroupState(
    initialJoinOperator: String = "and",
) {
    val id: String = UUID.randomUUID().toString()
    var joinOperator by mutableStateOf(initialJoinOperator)
    var items by mutableStateOf<List<GroupItem>>(listOf(GroupItem.Condition(ConditionState())))
    var collapsed by mutableStateOf(false)

    val itemCount: Int get() = items.size

    fun addCondition() {
        items = items + GroupItem.Condition(ConditionState())
    }

    fun addNestedGroup() {
        items = items + GroupItem.NestedGroup(ConditionGroupState(
            initialJoinOperator = if (joinOperator == "and") "or" else "and"
        ))
    }

    fun removeItem(id: String) {
        items = items.filter { it.id != id }
        if (items.isEmpty()) {
            items = listOf(GroupItem.Condition(ConditionState()))
        }
    }

    fun toSelNode(parentAlias: String? = null): SelNode = when (items.size) {
        0 -> SelBoolean.TRUE
        1 -> items.first().toSelNode(parentAlias)
        else -> SelOperation(joinOperator, SelArray(items.map { it.toSelNode(parentAlias) }))
    }

    fun copy(): ConditionGroupState = ConditionGroupState(initialJoinOperator = joinOperator).also { copy ->
        copy.items = items.map { it.copy() }
        copy.collapsed = collapsed
    }

    fun toPreviewText(): String {
        val joiner = " ${joinOperator.uppercase()} "
        return when (items.size) {
            0 -> "true"
            1 -> items.first().toPreviewText()
            else -> "(${items.joinToString(joiner) { it.toPreviewText() }})"
        }
    }
}
