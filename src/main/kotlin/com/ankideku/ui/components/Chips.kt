package com.ankideku.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.domain.model.SessionState
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

@Composable
fun SessionStateChip(
    state: SessionState,
    small: Boolean,
) {
    val colors = LocalAppColors.current
    val (text, color) = when (state) {
        SessionState.Pending -> "Pending" to MaterialTheme.colorScheme.outline
        SessionState.Running -> "Running" to colors.accent
        SessionState.Completed -> "Completed" to colors.success
        SessionState.Incomplete -> "Incomplete" to colors.warning
        is SessionState.Failed -> "Failed" to colors.error
        SessionState.Cancelled -> "Cancelled" to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}
