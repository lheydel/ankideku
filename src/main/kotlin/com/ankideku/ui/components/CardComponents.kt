package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.ReviewAction
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.DestructiveButton

/**
 * Breadcrumb navigation for going back to sessions
 */
@Composable
fun Breadcrumb(
    text: String,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clickableWithPointer(onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.accent,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
        )
    }
}

/**
 * Vertical divider for header cards
 */
@Composable
fun HeaderDivider() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(16.dp)
            .background(colors.divider),
    )
}

/**
 * Icon with label text
 */
@Composable
fun IconLabel(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconTint,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            color = textColor,
        )
    }
}

/**
 * Badge for action status (Accept/Reject/Skip)
 */
@Composable
fun ActionBadge(
    action: ReviewAction,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (action) {
                    ReviewAction.Accept -> Icons.Default.Check
                    ReviewAction.Reject -> Icons.Default.Close
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = action.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

/**
 * Card header with icon and title
 */
@Composable
fun CardHeader(
    title: String,
    titleColor: Color,
    headerBg: Color,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = titleColor,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
        )
    }
}

/**
 * Reasoning card with AI lightbulb icon
 */
@Composable
fun ReasoningCard(reasoning: String) {
    val colors = LocalAppColors.current
    val gradientBrush = Brush.linearGradient(
        colors = listOf(colors.accentMuted, colors.secondaryMuted, colors.accentMuted),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.accentStrong, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.onAccent,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Reasoning",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = reasoning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent,
                )
            }
        }
    }
}

/**
 * Empty state for when there are no suggestions to review
 */
@Composable
fun EmptyState(
    isProcessing: Boolean,
    processedCards: Int,
    totalCards: Int,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = colors.accent,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Analyzing Cards...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalCards > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "$processedCards / $totalCards cards processed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    LinearProgressIndicator(
                        progress = { if (totalCards > 0) processedCards.toFloat() / totalCards else 0f },
                        modifier = Modifier.width(200.dp),
                        color = colors.accent,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.success,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "No card to review",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "All suggestions have been reviewed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Action buttons row for Accept/Reject/Skip
 */
@Composable
fun ActionButtons(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    hasManualEdits: Boolean,
    isEditMode: Boolean,
    showOriginal: Boolean,
    isLoading: Boolean,
) {
    // V1 behavior: actions disabled when:
    // - in edit mode (must click Done first)
    // - has edits AND showing original AI (ambiguous state)
    // - loading
    val isDisabled = isEditMode || (hasManualEdits && showOriginal) || isLoading

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Left side: Reject + Skip
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DestructiveButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Outlined,
                enabled = !isDisabled,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Reject")
            }

            AppButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Outlined,
                enabled = !isDisabled,
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Skip")
            }
        }

        // Right side: Accept
        AppButton(
            onClick = onAccept,
            modifier = Modifier.weight(0.8f),
            enabled = !isDisabled,
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = 12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (hasManualEdits) "Apply Edits" else "Accept",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
