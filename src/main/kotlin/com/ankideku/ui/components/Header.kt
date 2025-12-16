package com.ankideku.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.ankideku.util.classpathPainterResource
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.sel.SelBuilderWindow
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

@Composable
fun Header(
    isConnected: Boolean,
    onSettingsClick: () -> Unit,
    onSidebarToggle: () -> Unit,
    isSidebarVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var showSelWindow by remember { mutableStateOf(false) }

    // SEL Builder Window
    if (showSelWindow) {
        SelBuilderWindow(
            onClose = { showSelWindow = false },
            onConfirm = { query ->
                println("SEL Query: ${query.toJson()}")
                showSelWindow = false
            },
        )
    }

    val background = Brush.horizontalGradient(
        colors = listOf(colors.appHeaderStart, colors.appHeaderMid, colors.appHeaderEnd),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App title and logo
            Image(
                painter = classpathPainterResource("icons/icon.png"),
                contentDescription = "AnkiDeku Logo",
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "AnkiDeku",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )

            Spacer(Modifier.width(Spacing.lg))

            // Connection status
            ConnectionIndicator(isConnected = isConnected)

            Spacer(Modifier.weight(1f))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Search button (SEL query builder)
                AppIconButton(
                    onClick = { showSelWindow = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                    )
                }

                AppIconButton(
                    onClick = onSettingsClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp),
                    )
                }

                AppIconButton(
                    onClick = onSidebarToggle,
                ) {
                    Icon(
                        imageVector = if (isSidebarVisible) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                        contentDescription = if (isSidebarVisible) "Hide AI sidebar" else "Show AI sidebar",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(isConnected: Boolean) {
    val colors = LocalAppColors.current
    val statusColor = if (isConnected) colors.success else colors.error
    val statusMutedColor = if (isConnected) colors.successMuted else colors.errorMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = statusMutedColor,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small,
                ),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = if (isConnected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
        )
    }
}
