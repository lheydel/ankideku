package com.ankideku.ui.components.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.ui.screens.main.QueueTab
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer

/**
 * Custom tab selector:
 * - Shows Notes+History in pre-session mode, Queue+History during active session
 * - Equal-width buttons
 * - Active: bottom border, primary text, subtle background tint
 * - Inactive: muted text with hover
 */
@Composable
fun QueueTabSelector(
    activeTab: QueueTab,
    isPreSession: Boolean,
    notesCount: Int,
    queueCount: Int,
    historyCount: Int,
    hasNoteFilter: Boolean = false,
    onTabChanged: (QueueTab) -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        if (isPreSession) {
            // Pre-session: show Notes tab
            QueueTabButton(
                text = "Notes",
                count = notesCount,
                isActive = activeTab == QueueTab.Notes,
                hasFilter = hasNoteFilter,
                onClick = { onTabChanged(QueueTab.Notes) },
                modifier = Modifier.weight(1f),
            )
        } else {
            // Active session: show Queue tab
            QueueTabButton(
                text = "Queue",
                count = queueCount,
                isActive = activeTab == QueueTab.Queue,
                onClick = { onTabChanged(QueueTab.Queue) },
                modifier = Modifier.weight(1f),
            )
        }

        // History tab (always visible)
        QueueTabButton(
            text = "History",
            count = historyCount,
            isActive = activeTab == QueueTab.History,
            onClick = { onTabChanged(QueueTab.History) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun QueueTabButton(
    text: String,
    count: Int,
    isActive: Boolean,
    hasFilter: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    // V1 styling:
    // Active: text-primary-600, border-b-2 border-primary-600, bg-primary-50/50
    // Inactive: text-gray-600, hover:text-gray-900, hover:bg-gray-50
    val textColor = if (isActive) colors.accent else colors.textSecondary
    val backgroundColor = if (isActive) colors.accentMuted.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = modifier
            .clickableWithPointer(onClick = onClick)
            .background(backgroundColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Filter indicator
                if (hasFilter) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.accent,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                )
                if (count > 0) {
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "($count)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                    )
                }
            }

            // Bottom border indicator (only for active tab)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (isActive) colors.accent else Color.Transparent),
            )
        }
    }
}
