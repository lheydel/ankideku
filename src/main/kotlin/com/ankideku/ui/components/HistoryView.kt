package com.ankideku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer

@Composable
fun HistoryBreadcrumb(onClose: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clickableWithPointer(onClick = onClose)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close history view",
            modifier = Modifier.size(18.dp),
            tint = colors.accent,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "Close History View",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
        )
    }
}
