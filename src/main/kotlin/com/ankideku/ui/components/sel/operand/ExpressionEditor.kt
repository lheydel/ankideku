package com.ankideku.ui.components.sel.operand

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.sel.state.ExpressionState
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.components.sel.state.SubqueryState
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Editor for expression operands with nested operands.
 */
@Composable
internal fun ExpressionEditor(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    depth: Int,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current

    if (operand.expression == null) {
        operand.expression = ExpressionState()
    }
    val expr = operand.expression!!

    val accentColor = getDepthAccentColor(depth, colors)

    // Filter operators to those that return the expected type
    val filteredOperators = remember(expectedType) {
        SelOperatorRegistry.returningType(expectedType)
    }

    // Entire expression in a colored bordered block
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = accentColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Function selector row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Function badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = expr.operator,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }

                AppDropdown(
                    items = filteredOperators,
                    selectedItem = SelOperatorRegistry[expr.operator],
                    onItemSelected = { op ->
                        expr.operator = op.key
                        expr.setOperandCount(op.metadata.signature.minArgs)
                    },
                    itemLabel = { it.metadata.displayName },
                    placeholder = "fn",
                    modifier = Modifier.width(100.dp),
                )
            }

            // Get the expression operator's signature to know what types its args expect
            val exprSignature = expr.operatorMetadata?.signature

            // Arguments layout - horizontal if 2 or less, vertical otherwise
            ExpressionOperands(
                expr = expr,
                exprSignature = exprSignature,
                entityType = entityType,
                depth = depth,
                accentColor = accentColor,
                parentScopes = parentScopes,
                noteTypeFields = noteTypeFields,
                onSubqueryClick = onSubqueryClick,
            )
        }
    }
}

@Composable
private fun ExpressionOperands(
    expr: ExpressionState,
    exprSignature: com.ankideku.domain.sel.operator.SelOperatorSignature?,
    entityType: EntityType,
    depth: Int,
    accentColor: Color,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current

    if (expr.operands.size <= 2) {
        // Horizontal layout for 1-2 args
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            expr.operands.forEachIndexed { index, arg ->
                val argExpectedType = exprSignature?.argTypeAt(index) ?: SelType.Any
                OperandBlock(
                    arg = arg,
                    index = index,
                    argExpectedType = argExpectedType,
                    entityType = entityType,
                    depth = depth,
                    accentColor = accentColor,
                    parentScopes = parentScopes,
                    noteTypeFields = noteTypeFields,
                    onSubqueryClick = onSubqueryClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        // Vertical layout for 3+ args
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            expr.operands.forEachIndexed { index, arg ->
                val argExpectedType = exprSignature?.argTypeAt(index) ?: SelType.Any
                OperandBlock(
                    arg = arg,
                    index = index,
                    argExpectedType = argExpectedType,
                    entityType = entityType,
                    depth = depth,
                    accentColor = accentColor,
                    parentScopes = parentScopes,
                    noteTypeFields = noteTypeFields,
                    onSubqueryClick = onSubqueryClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OperandBlock(
    arg: OperandState,
    index: Int,
    argExpectedType: SelType,
    entityType: EntityType,
    depth: Int,
    accentColor: Color,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier,
        shape = InputShape,
        color = colors.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "arg ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
            )
            OperandEditor(
                operand = arg,
                entityType = entityType,
                expectedType = argExpectedType,
                depth = depth + 1,
                parentScopes = parentScopes,
                noteTypeFields = noteTypeFields,
                onSubqueryClick = onSubqueryClick,
            )
        }
    }
}

/**
 * Get accent color based on nesting depth for visual distinction.
 */
internal fun getDepthAccentColor(depth: Int, colors: AppColorScheme): Color {
    return when (depth % 3) {
        0 -> colors.accent
        1 -> colors.secondary
        else -> colors.warning
    }
}
