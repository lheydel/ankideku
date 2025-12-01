package com.ankideku.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionState
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionSelector(
    sessions: List<Session>,
    onLoadSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    // V1-style gradient background (diagonal)
    val gradientBackground = Brush.linearGradient(
        colors = listOf(colors.contentGradientStart, colors.contentGradientEnd),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
        ) {
            // Header
            Text(
                text = "AI Sessions",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Select a previous session to review",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))

            if (sessions.isEmpty()) {
            // Enhanced empty state with gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Spacing.xl),
                ) {
                    // Lightning icon with subtle background
                    Surface(
                        modifier = Modifier.size(80.dp),
                        color = colors.accentMuted,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = colors.accent,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        text = "No Sessions Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Use the AI Assistant sidebar to create your first session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Select a deck, enter a prompt, and click Start Session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(sessions.sortedByDescending { it.createdAt }) { session ->
                    SessionCard(
                        session = session,
                        onLoad = { onLoadSession(session.id) },
                        onDelete = { onDeleteSession(session.id) },
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SessionCard(
    session: Session,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }

    val colors = LocalAppColors.current
    // Animate hover effects
    val scale by animateFloatAsState(if (isHovered) 1.02f else 1f)
    val elevation by animateDpAsState(if (isHovered) 8.dp else 1.dp)
    val borderColor = if (isHovered) colors.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Card(
        onClick = onLoad,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .handPointer()
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colors.headerGradientStart,
                                    colors.headerGradientEnd,
                                ),
                            ),
                            shape = MaterialTheme.shapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }

                // Deck name centered between icon and status
                Text(
                    text = session.deckName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SessionStateChip(session.state, small = false)
                    // Delete button - only visible on hover
                    if (isHovered) {
                        DeleteSessionButton(
                            onDelete = onDelete,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Fixed height for prompt area (2 lines worth)
            Box(modifier = Modifier.height(40.dp)) {
                Text(
                    text = session.prompt,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            Text(
                text = formatDateTime(session.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${session.progress.pendingSuggestionsCount} pending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "#${session.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }

}

private fun formatDateTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
