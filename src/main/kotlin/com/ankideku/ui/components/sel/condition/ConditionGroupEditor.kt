package com.ankideku.ui.components.sel.condition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.ui.components.AppIconButton
import com.ankideku.ui.components.sel.operand.AddButton
import com.ankideku.ui.components.sel.operand.OperandChip
import com.ankideku.ui.components.sel.operand.OperandEditor
import com.ankideku.ui.components.sel.state.*
import com.ankideku.ui.theme.*

/**
 * Renders a group of conditions joined by AND/OR.
 * Supports recursive nesting for complex boolean logic.
 *
 * @param parentScopes Available parent scopes for property references
 * @param onSubqueryClick Called when user clicks a subquery to edit it
 */
@Composable
internal fun ConditionGroupEditor(
    group: ConditionGroupState,
    entityType: EntityType,
    depth: Int,
    parentScopes: List<Pair<String, EntityType>> = emptyList(),
    noteTypeFields: Map<String, List<String>> = emptyMap(),
    onSubqueryClick: ((SubqueryState) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val isAnd = group.joinOperator == "and"
    val groupColor = if (isAnd) colors.accent else colors.warning

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        group.items.forEachIndexed { index, item ->
            // Show join operator between items
            if (index > 0) {
                JoinOperatorBadge(
                    operator = group.joinOperator,
                    color = groupColor,
                    onClick = { group.joinOperator = if (isAnd) "or" else "and" },
                    modifier = Modifier.padding(start = (depth * 16).dp),
                )
            }

            // Item row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp),
                verticalAlignment = Alignment.Top,
            ) {
                when (item) {
                    is GroupItem.Condition -> {
                        ConditionRowEditor(
                            condition = item.state,
                            entityType = entityType,
                            onRemove = if (group.items.size > 1) {
                                { group.removeItem(item.id) }
                            } else null,
                            parentScopes = parentScopes,
                            noteTypeFields = noteTypeFields,
                            onSubqueryClick = onSubqueryClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    is GroupItem.NestedGroup -> {
                        NestedGroupEditor(
                            group = group,
                            nestedGroup = item.state,
                            entityType = entityType,
                            depth = depth,
                            onRemove = { group.removeItem(item.id) },
                            parentScopes = parentScopes,
                            noteTypeFields = noteTypeFields,
                            onSubqueryClick = onSubqueryClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Add buttons
        Row(
            modifier = Modifier.padding(start = (depth * 16).dp, top = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AddButton("+ Condition") { group.addCondition() }
            AddButton("+ Group") { group.addNestedGroup() }
        }
    }
}

@Composable
private fun JoinOperatorBadge(
    operator: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.15f),
        ) {
            Text(
                text = operator.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun NestedGroupEditor(
    group: ConditionGroupState,
    nestedGroup: ConditionGroupState,
    entityType: EntityType,
    depth: Int,
    onRemove: () -> Unit,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "(",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textMuted,
            )

            AppIconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove group",
                    tint = colors.textMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        ConditionGroupEditor(
            group = nestedGroup,
            entityType = entityType,
            depth = depth + 1,
            parentScopes = parentScopes,
            noteTypeFields = noteTypeFields,
            onSubqueryClick = onSubqueryClick,
        )

        Text(
            ")",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textMuted,
            modifier = Modifier.padding(start = (depth * 16).dp),
        )
    }
}

/**
 * Renders a single condition with operator and operands.
 */
@Composable
internal fun ConditionRowEditor(
    condition: ConditionState,
    entityType: EntityType,
    onRemove: (() -> Unit)?,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = InputShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Header row
            ConditionHeader(
                condition = condition,
                entityType = entityType,
                expanded = expanded,
                onExpandToggle = { expanded = !expanded },
                onRemove = onRemove,
            )

            // Expanded operands
            if (expanded) {
                ConditionOperands(
                    condition = condition,
                    entityType = entityType,
                    parentScopes = parentScopes,
                    noteTypeFields = noteTypeFields,
                    onSubqueryClick = onSubqueryClick,
                )
            }
        }
    }
}

@Composable
private fun ConditionHeader(
    condition: ConditionState,
    entityType: EntityType,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // Collapse toggle
        AppIconButton(
            onClick = onExpandToggle,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }

        OperatorSelector(
            selectedOperator = condition.operator,
            onOperatorSelected = { newOp ->
                condition.operator = newOp
                SelOperatorRegistry.getMetadata(newOp)?.let { meta ->
                    condition.setOperandCount(meta.signature.minArgs)
                }
            },
            returnType = SelType.Boolean,
            modifier = Modifier.width(180.dp),
        )

        // Inline preview when collapsed
        if (!expanded) {
            Spacer(Modifier.width(Spacing.sm))
            OperandChip(condition.operands.getOrNull(0), entityType, depth = 0)
            if (condition.operands.size > 1) {
                OperandChip(condition.operands.getOrNull(1), entityType, depth = 0)
            }
        }

        Spacer(Modifier.weight(1f))

        // Remove button
        if (onRemove != null) {
            AppIconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ConditionOperands(
    condition: ConditionState,
    entityType: EntityType,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current
    val signature = condition.operatorMetadata?.signature

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        condition.operands.forEachIndexed { index, operand ->
            val expectedType = signature?.argTypeAt(index) ?: SelType.Any

            Surface(
                modifier = Modifier.weight(1f),
                shape = InputShape,
                color = colors.surfaceAlt,
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        "Operand ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                    )
                    OperandEditor(
                        operand = operand,
                        entityType = entityType,
                        expectedType = expectedType,
                        depth = 0,
                        parentScopes = parentScopes,
                        noteTypeFields = noteTypeFields,
                        onSubqueryClick = onSubqueryClick,
                    )
                }
            }
        }
    }
}
