package com.ankideku.ui.components.sel.operand

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.components.sel.state.OperandType
import com.ankideku.ui.components.sel.state.SubqueryState
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Full operand editor with type selector and type-specific input.
 *
 * @param operand The operand state to edit
 * @param entityType The current entity type being queried
 * @param expectedType The type expected by the parent operator
 * @param depth Nesting depth for visual styling
 * @param parentScopes Available parent scopes for property references (alias to EntityType)
 * @param noteTypeFields Map of note type names to their fields
 * @param onSubqueryClick Called when user clicks a subquery to edit it
 */
@Composable
internal fun OperandEditor(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    depth: Int,
    parentScopes: List<Pair<String, EntityType>> = emptyList(),
    noteTypeFields: Map<String, List<String>> = emptyMap(),
    onSubqueryClick: ((SubqueryState) -> Unit)? = null,
) {
    val colors = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // Type selector - compact horizontal tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OperandType.entries.forEach { type ->
                val selected = operand.type == type
                Surface(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { operand.type = type },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) colors.accent.copy(alpha = 0.15f) else Color.Transparent,
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.accent else colors.textMuted,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                    )
                }
            }
        }

        // Type-specific input
        when (operand.type) {
            OperandType.Field -> {
                FieldSelector(
                    operand = operand,
                    entityType = entityType,
                    noteTypeFields = noteTypeFields,
                )
            }
            OperandType.Property -> {
                PropertySelector(
                    operand = operand,
                    entityType = entityType,
                    expectedType = expectedType,
                    parentScopes = parentScopes,
                )
            }
            OperandType.Value -> {
                AppTextInput(
                    value = operand.value,
                    onValueChange = { operand.value = it },
                    placeholder = "value",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OperandType.Expression -> {
                ExpressionEditor(
                    operand = operand,
                    entityType = entityType,
                    expectedType = expectedType,
                    depth = depth,
                    parentScopes = parentScopes,
                    noteTypeFields = noteTypeFields,
                    onSubqueryClick = onSubqueryClick,
                )
            }
            OperandType.Subquery -> {
                SubqueryChip(
                    operand = operand,
                    expectedType = expectedType,
                    onSubqueryClick = onSubqueryClick,
                )
            }
        }
    }
}

/**
 * Compact chip displaying an operand's value for inline preview.
 */
@Composable
internal fun OperandChip(
    operand: OperandState?,
    entityType: EntityType,
    depth: Int,
) {
    val colors = LocalAppColors.current
    if (operand == null) return

    val accentColor = getDepthAccentColor(depth, colors)

    when (operand.type) {
        OperandType.Field -> {
            InlineChip(
                text = if (operand.fieldContext != null)
                    "${operand.fieldName}[${operand.fieldContext}]"
                else
                    operand.fieldName.ifEmpty { "field" },
                color = colors.success,
                prefix = "field.",
            )
        }
        OperandType.Property -> {
            InlineChip(
                text = operand.propertyName.ifEmpty { "prop" },
                color = colors.secondary,
                prefix = "prop.",
            )
        }
        OperandType.Value -> {
            val displayValue = operand.value.ifEmpty { "\"\"" }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.warning.copy(alpha = 0.1f),
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.warning,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                )
            }
        }
        OperandType.Expression -> {
            val expr = operand.expression
            if (expr != null) {
                // Wrap entire expression in a bordered block
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Function name badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = accentColor.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = expr.operator,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 1.dp),
                            )
                        }

                        Text("(", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        // Inline args
                        expr.operands.forEachIndexed { index, arg ->
                            if (index > 0) {
                                Text(", ", color = colors.textMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            OperandChip(arg, entityType, depth + 1)
                        }

                        Text(")", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("expr()", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            }
        }
        OperandType.Subquery -> {
            InlineChip(text = "subquery", color = colors.secondary, prefix = "")
        }
    }
}

@Composable
private fun InlineChip(
    text: String,
    color: Color,
    prefix: String,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (prefix.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.6f),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

/**
 * Small clickable text button for adding items.
 */
@Composable
internal fun AddButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
    )
}
