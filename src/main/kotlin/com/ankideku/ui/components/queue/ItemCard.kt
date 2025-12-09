package com.ankideku.ui.components.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.components.ConfiguredText
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer

/**
 * Generic item card used by both QueueCard and NoteCard.
 */
@Composable
fun ItemCard(
    displayValue: String,
    fieldName: String,
    index: Int,
    isCurrent: Boolean,
    noteTypeConfig: NoteTypeConfig?,
    onClick: () -> Unit,
    subtitle: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current

    val cardBackground = if (isCurrent) {
        Brush.horizontalGradient(
            colors = listOf(
                colors.accent.copy(alpha = 0.15f),
                colors.accent.copy(alpha = 0.05f),
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(colors.surface, colors.surface)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableWithPointer(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground)
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent bar for current item
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(colors.accent, MaterialTheme.shapes.small),
                )
                Spacer(Modifier.width(Spacing.sm))
            }

            // Index number
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isCurrent) colors.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Selected item",
                        modifier = Modifier.size(14.dp),
                        tint = colors.onAccent,
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(Spacing.sm))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                ConfiguredText(
                    text = displayValue,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fieldName = fieldName,
                    noteTypeConfig = noteTypeConfig,
                )
                Spacer(Modifier.height(Spacing.xxs))
                subtitle()
            }
        }
    }
}
