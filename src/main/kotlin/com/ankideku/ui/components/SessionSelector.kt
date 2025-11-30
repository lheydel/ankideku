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
    Column(
        modifier = modifier
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

        val colors = LocalAppColors.current

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SessionCard(
    session: Session,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Icon
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SessionStateChip(session.state)
                    // Delete button - only visible on hover
                    if (isHovered) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = session.prompt,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(Spacing.xs))

            Text(
                text = formatDateTime(session.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${session.progress.suggestionsCount} suggestions",
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
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete this session? This will permanently remove all suggestions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SessionStateChip(state: SessionState) {
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
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
