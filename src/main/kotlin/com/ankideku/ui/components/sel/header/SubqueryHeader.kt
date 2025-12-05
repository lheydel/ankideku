package com.ankideku.ui.components.sel.header

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.sel.common.LabeledTextInput
import com.ankideku.ui.components.sel.common.OrderBySelector
import com.ankideku.ui.components.sel.state.SubqueryResultType
import com.ankideku.ui.components.sel.state.SubqueryState
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Header for subquery showing result type, target, order, and limit.
 * Wrapped in a colored surface to visually distinguish from root query.
 */
@Composable
internal fun SubqueryHeader(
    subquery: SubqueryState,
) {
    val colors = LocalAppColors.current
    val schema = SelEntityRegistry[subquery.target]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = colors.secondary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Result type dropdown
            Text(
                "Return:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppDropdown(
                items = SubqueryResultType.entries.toList(),
                selectedItem = subquery.resultType,
                onItemSelected = { subquery.resultType = it },
                itemLabel = { it.displayName },
                modifier = Modifier.width(140.dp),
            )

            // Target entity dropdown
            Text(
                "from:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppDropdown(
                items = listOf(EntityType.Note, EntityType.Suggestion, EntityType.HistoryEntry),
                selectedItem = subquery.target,
                onItemSelected = { subquery.target = it },
                itemLabel = { it.name },
                modifier = Modifier.width(140.dp),
            )

            // Result property (for scalar result type)
            if (subquery.resultType == SubqueryResultType.ScalarProperty) {
                Text(
                    "property:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondary,
                )
                AppTextInput(
                    value = subquery.resultProperty,
                    onValueChange = { subquery.resultProperty = it },
                    placeholder = "property name",
                    modifier = Modifier.width(140.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            // Order by
            OrderBySelector(
                properties = schema.visibleProperties,
                orderBy = subquery.orderBy,
                onOrderByChange = { subquery.orderBy = it },
                labelColor = colors.secondary,
            )

            // Limit input
            LabeledTextInput(
                label = "Limit",
                value = subquery.limit?.toString() ?: "",
                onValueChange = { subquery.limit = it.replace(Regex("[^0-9]"), "").toIntOrNull() },
                placeholder = "∞",
                inputWidth = 80.dp,
                labelColor = colors.secondary,
            )
        }
    }
}
