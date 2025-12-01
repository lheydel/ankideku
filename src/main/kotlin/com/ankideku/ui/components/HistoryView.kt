package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.ReviewAction
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.theme.handPointer

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

@Composable
fun HistoryHeaderCard(
    entry: HistoryEntry,
    onCopy: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val actionColor = when (entry.action) {
        ReviewAction.Accept -> colors.success
        ReviewAction.Reject -> colors.error
    }
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(colors.surface, colors.surfaceAlt),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // History badge
                Surface(
                    color = colors.secondaryMuted,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.secondary,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.secondary,
                        )
                    }
                }

                HeaderDivider()

                IconLabel(
                    icon = Icons.Default.Folder,
                    text = entry.deckName,
                    iconTint = colors.accentStrong,
                    textColor = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )

                HeaderDivider()

                IconLabel(
                    icon = Icons.Default.Edit,
                    text = "${entry.aiChanges.size} field(s)",
                    iconTint = colors.textMuted,
                    textColor = colors.textMuted,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onCopy(buildHistoryCopyText(entry)) },
                    modifier = Modifier.handPointer(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }

                ActionBadge(action = entry.action, color = actionColor)
            }
        }
    }
}

fun buildHistoryCopyText(entry: HistoryEntry): String {
    return buildString {
        appendLine("## History Entry")
        appendLine("Action: ${entry.action.name}")
        appendLine("Deck: ${entry.deckName}")
        appendLine()
        if (!entry.reasoning.isNullOrBlank()) {
            appendLine("## AI Reasoning")
            appendLine(entry.reasoning)
            appendLine()
        }
        appendLine("## Original Card")
        entry.originalFields.forEach { (name, field) ->
            appendLine("**$name:** ${field.value}")
        }
        appendLine()
        appendLine("## AI Changes")
        entry.aiChanges.forEach { (name, value) ->
            appendLine("**$name:** $value")
        }
        if (entry.appliedChanges != null) {
            appendLine()
            appendLine("## Applied Changes")
            entry.appliedChanges.forEach { (name, value) ->
                appendLine("**$name:** $value")
            }
        }
    }
}
