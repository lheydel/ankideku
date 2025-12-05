package com.ankideku.ui.components.sel.header

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.sel.state.BreadcrumbEntry
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Breadcrumb navigation bar showing path to current subquery.
 */
@Composable
internal fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbEntry>,
    onNavigate: (Int) -> Unit,
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = colors.surfaceAlt,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            breadcrumbs.forEachIndexed { index, entry ->
                if (index > 0) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }

                val isLast = index == breadcrumbs.lastIndex
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLast) FontWeight.Medium else FontWeight.Normal,
                    color = if (isLast) colors.textPrimary else colors.accent,
                    modifier = if (!isLast) {
                        Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onNavigate(index) }
                    } else Modifier,
                )
            }
        }
    }
}
