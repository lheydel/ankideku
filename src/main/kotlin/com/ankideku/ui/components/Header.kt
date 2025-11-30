package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.PanelSizes
import com.ankideku.ui.theme.Spacing

@Composable
fun Header(
    isConnected: Boolean,
    onThemeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onSidebarToggle: () -> Unit,
    isSidebarVisible: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    // Glassmorphism-style header with gradient background
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            colors.accent.copy(alpha = 0.05f),
        ),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PanelSizes.headerHeight),
        color = Color.Transparent,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
        ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // App Icon
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(Spacing.sm))

                // Title and Subtitle
                Column {
                    Text(
                        text = "AnkiDeku",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "AI-Powered Deck Revision",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(Spacing.lg))

                // Connection Status
                ConnectionIndicator(isConnected = isConnected)
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                // Theme Toggle
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                    )
                }

                // Settings
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                    )
                }

                // Sidebar Toggle
                IconButton(onClick = onSidebarToggle) {
                    Icon(
                        imageVector = if (isSidebarVisible) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                        contentDescription = if (isSidebarVisible) "Hide AI sidebar" else "Show AI sidebar",
                    )
                }
            }
        }
        }  // Close Box for glassmorphism
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
