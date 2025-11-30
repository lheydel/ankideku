package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.*
import com.ankideku.ui.screens.main.HistoryViewMode
import com.ankideku.ui.screens.main.QueueTab
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.appInputColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun QueuePanel(
    suggestions: List<Suggestion>,
    currentSuggestionIndex: Int,
    historyEntries: List<HistoryEntry>,
    activeTab: QueueTab,
    currentSession: Session?,
    historySearchQuery: String,
    historyViewMode: HistoryViewMode,
    onTabChanged: (QueueTab) -> Unit,
    onHistoryViewModeChanged: (HistoryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
        ) {
            // Session info
            currentSession?.let { session ->
                SessionInfoCard(session)
                Spacer(Modifier.height(Spacing.md))
            }

            // Tab selector
            TabRow(
                selectedTabIndex = if (activeTab == QueueTab.Queue) 0 else 1,
            ) {
                Tab(
                    selected = activeTab == QueueTab.Queue,
                    onClick = { onTabChanged(QueueTab.Queue) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Queue")
                            if (suggestions.isNotEmpty()) {
                                Spacer(Modifier.width(Spacing.xs))
                                Badge { Text("${suggestions.size}") }
                            }
                        }
                    },
                )
                Tab(
                    selected = activeTab == QueueTab.History,
                    onClick = { onTabChanged(QueueTab.History) },
                    text = { Text("History") },
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // Tab content
            when (activeTab) {
                QueueTab.Queue -> QueueContent(
                    suggestions = suggestions,
                    currentIndex = currentSuggestionIndex,
                )
                QueueTab.History -> HistoryContent(
                    entries = historyEntries,
                    searchQuery = historySearchQuery,
                    viewMode = historyViewMode,
                    currentSessionId = currentSession?.id,
                    onViewModeChanged = onHistoryViewModeChanged,
                )
            }
        }
    }
}

@Composable
private fun SessionInfoCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Current Session",
                    style = MaterialTheme.typography.labelMedium,
                )
                SessionStateChip(session.state)
            }

            Spacer(Modifier.height(Spacing.sm))

            // Progress
            val progress = session.progress
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${progress.processedCards}/${progress.totalCards} cards",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "${progress.suggestionsCount} suggestions",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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

@Composable
private fun QueueContent(
    suggestions: List<Suggestion>,
    currentIndex: Int,
) {
    val colors = LocalAppColors.current
    val pendingSuggestions = suggestions.filter { it.status == SuggestionStatus.Pending }
    val doneSuggestions = suggestions.filter { it.status != SuggestionStatus.Pending }

    Column(modifier = Modifier.fillMaxSize()) {
        if (suggestions.isEmpty()) {
            // Enhanced empty state with gradient background
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.success.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Spacing.lg),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = colors.success.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "All cards have been reviewed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()

            // Auto-scroll to current card
            LaunchedEffect(currentIndex) {
                if (currentIndex >= 0 && currentIndex < suggestions.size) {
                    listState.animateScrollToItem(currentIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(suggestions) { index, suggestion ->
                    QueueCard(
                        suggestion = suggestion,
                        index = index + 1,  // 1-based display
                        isCurrent = index == currentIndex,
                    )
                }
            }
        }

        // Progress footer with Done / Left counts
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Done count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.success,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "Done: ${doneSuggestions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.success,
                    )
                }

                // Left count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "Left: ${pendingSuggestions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueCard(
    suggestion: Suggestion,
    index: Int,
    isCurrent: Boolean,
) {
    val colors = LocalAppColors.current
    // Get the first field value (sorted by order)
    val firstFieldValue = suggestion.originalFields.values
        .minByOrNull { it.order }
        ?.value
        ?.take(100)  // Limit length
        ?.replace(Regex("<[^>]*>"), "")  // Strip HTML tags
        ?.trim()
        ?: "Note #${suggestion.noteId}"

    val cardBackground = if (isCurrent) {
        Brush.horizontalGradient(
            colors = listOf(
                colors.accent.copy(alpha = 0.15f),
                colors.accent.copy(alpha = 0.05f),
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant,
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Left accent bar for current card
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
                        contentDescription = "Current card",
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

            // Card content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstFieldValue,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = "${suggestion.changes.size} field(s) changed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    entries: List<HistoryEntry>,
    searchQuery: String,
    viewMode: HistoryViewMode,
    currentSessionId: Long?,
    onViewModeChanged: (HistoryViewMode) -> Unit,
) {
    // Filter entries based on view mode
    val filteredEntries = when (viewMode) {
        HistoryViewMode.Session -> entries.filter { it.sessionId == currentSessionId }
        HistoryViewMode.Global -> entries
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Session / All toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterChip(
                selected = viewMode == HistoryViewMode.Session,
                onClick = { onViewModeChanged(HistoryViewMode.Session) },
                label = { Text("Current Session") },
                leadingIcon = if (viewMode == HistoryViewMode.Session) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
            FilterChip(
                selected = viewMode == HistoryViewMode.Global,
                onClick = { onViewModeChanged(HistoryViewMode.Global) },
                label = { Text("All Sessions") },
                leadingIcon = if (viewMode == HistoryViewMode.Global) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "No results found"
                        viewMode == HistoryViewMode.Session && currentSessionId == null -> "No active session"
                        else -> "No history yet"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(filteredEntries) { _, entry ->
                    HistoryCard(
                        entry = entry,
                        showDeckName = viewMode == HistoryViewMode.Global,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    showDeckName: Boolean,
) {
    val colors = LocalAppColors.current
    val actionColor = when (entry.action) {
        ReviewAction.Accept -> colors.success
        ReviewAction.Reject -> colors.error
        ReviewAction.Skip -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Full-height action indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(actionColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // First field value or Note ID
                    val displayText = entry.originalFields.values
                        .sortedBy { it.order }
                        .firstOrNull()?.value
                        ?.take(50)
                        ?.replace(Regex("<[^>]*>"), "")
                        ?.trim()
                        ?: "Note #${entry.noteId}"

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(Spacing.sm))

                    // Timestamp in 24-hour format
                    Text(
                        text = formatTime24h(entry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(Spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Deck name (in Global view) or reasoning preview
                    if (showDeckName) {
                        Text(
                            text = entry.deckName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = entry.reasoning?.take(50) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Action badge
                    Surface(
                        color = actionColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = entry.action.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = actionColor,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime24h(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
