package com.ankideku.ui.components.sel.operand

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.components.sel.state.SubqueryResultType
import com.ankideku.ui.components.sel.state.SubqueryState
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Clickable chip that shows subquery summary and navigates to edit it.
 *
 * @param operand The operand state containing the subquery
 * @param expectedType The type expected by the parent operator, used to filter result types
 * @param onSubqueryClick Callback when the chip is clicked to edit the subquery
 */
@Composable
internal fun SubqueryChip(
    operand: OperandState,
    expectedType: SelType,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current

    // Filter result types based on expected type
    val availableResultTypes = remember(expectedType) {
        SubqueryResultType.entries.filter { resultType ->
            when (expectedType) {
                SelType.Any -> true // All types allowed
                SelType.Boolean -> resultType.returnType == SelType.Boolean
                SelType.Number -> resultType.returnType == SelType.Number || resultType.returnType == null
                SelType.String -> resultType.returnType == null // Only ScalarProperty can return string
                else -> resultType.returnType == expectedType || resultType.returnType == null
            }
        }
    }

    // Initialize subquery if not present, with appropriate default result type
    if (operand.subquery == null) {
        val defaultResultType = availableResultTypes.firstOrNull() ?: SubqueryResultType.Exists
        operand.subquery = SubqueryState(initialResultType = defaultResultType)
    }
    val subquery = operand.subquery!!

    // Ensure current result type is valid for expected type
    if (subquery.resultType !in availableResultTypes && availableResultTypes.isNotEmpty()) {
        subquery.resultType = availableResultTypes.first()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onSubqueryClick != null) {
                    Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onSubqueryClick(subquery) }
                } else Modifier
            ),
        shape = InputShape,
        color = colors.secondary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Result type dropdown (filtered by expected type)
            AppDropdown(
                items = availableResultTypes,
                selectedItem = subquery.resultType,
                onItemSelected = { subquery.resultType = it },
                itemLabel = { it.displayName },
                modifier = Modifier.width(120.dp),
            )

            // Target entity dropdown
            AppDropdown(
                items = listOf(EntityType.Note, EntityType.Suggestion),
                selectedItem = subquery.target,
                onItemSelected = { subquery.target = it },
                itemLabel = { it.name },
                modifier = Modifier.width(120.dp),
            )

            // Condition count indicator
            val conditionCount = subquery.rootGroup.items.size
            Text(
                text = "($conditionCount condition${if (conditionCount != 1) "s" else ""})",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )

            Spacer(Modifier.weight(1f))

            // Edit indicator
            if (onSubqueryClick != null) {
                Text(
                    text = "Edit →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.secondary,
                )
            }
        }
    }
}
