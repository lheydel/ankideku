package com.ankideku.ui.components.sel.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.ast.SelOrderClause
import com.ankideku.domain.sel.ast.SelOrderDirection
import com.ankideku.domain.sel.schema.EntityProperty
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Reusable order-by selector with field and direction dropdowns.
 *
 * @param properties Available properties to order by
 * @param orderBy Current order clauses (only first is used)
 * @param onOrderByChange Called when order changes
 * @param labelColor Color for the "Order:" label
 * @param modifier Modifier for the row
 */
@Composable
fun OrderBySelector(
    properties: List<EntityProperty>,
    orderBy: List<SelOrderClause>,
    onOrderByChange: (List<SelOrderClause>) -> Unit,
    labelColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val currentOrderField = orderBy.firstOrNull()?.field
    val currentOrderDir = orderBy.firstOrNull()?.direction ?: SelOrderDirection.Asc
    val selectedOrderProp = properties.find { it.selKey == currentOrderField }
    val effectiveLabelColor = labelColor ?: colors.textMuted

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            "Order:",
            style = MaterialTheme.typography.bodyMedium,
            color = effectiveLabelColor,
        )
        AppDropdown(
            items = listOf(null) + properties,
            selectedItem = selectedOrderProp,
            onItemSelected = { prop ->
                onOrderByChange(
                    if (prop != null) listOf(SelOrderClause(prop.selKey, currentOrderDir))
                    else emptyList()
                )
            },
            itemLabel = { it?.displayName ?: "None" },
            modifier = Modifier.width(120.dp),
        )
        if (currentOrderField != null) {
            AppDropdown(
                items = SelOrderDirection.entries.toList(),
                selectedItem = currentOrderDir,
                onItemSelected = { dir ->
                    onOrderByChange(listOf(SelOrderClause(currentOrderField, dir)))
                },
                itemLabel = { it.name },
                modifier = Modifier.width(80.dp),
            )
        }
    }
}
