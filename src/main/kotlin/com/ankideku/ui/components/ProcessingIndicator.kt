package com.ankideku.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.SessionProgress
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.components.DestructiveButton
import com.ankideku.ui.components.AppButtonVariant

@Composable
fun ProcessingIndicator(
    progress: SessionProgress,
    onCancel: () -> Unit,
    colors: AppColorScheme,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage,
        animationSpec = tween(durationMillis = 300),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accentSurface)
            .padding(Spacing.md),
    ) {
        // Status row with pulsing dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = colors.accentStrong,
                            shape = CircleShape,
                        )
                )
                // Status text
                Text(
                    text = if (progress.totalCards > 0) {
                        "Processing ${progress.processedCards} / ${progress.totalCards} cards"
                    } else {
                        "AI Processing..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.accentOnSurface,
                )
            }

            // Cancel button
            DestructiveButton(
                onClick = onCancel,
                variant = AppButtonVariant.Text,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Progress bar
        if (progress.totalCards > 0) {
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.accentStrong,
                trackColor = colors.accentTrack,
            )
        }

        // Token counts
        if (progress.inputTokens > 0 || progress.outputTokens > 0) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Tokens: ${progress.inputTokens.formatWithCommas()} in / ${progress.outputTokens.formatWithCommas()} out",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accentOnSurface,
            )
        }
    }
}

private fun Int.formatWithCommas(): String {
    return "%,d".format(this)
}
