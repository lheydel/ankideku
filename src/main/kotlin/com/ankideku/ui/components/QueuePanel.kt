package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.theme.handPointer
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
    fieldDisplayConfig: Map<String, String>,
    onTabChanged: (QueueTab) -> Unit,
    onHistoryViewModeChanged: (HistoryViewMode) -> Unit,
    onSuggestionClick: (Int) -> Unit,
    onHistoryClick: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    // Use gradient background like other panels
    val gradientBackground = Brush.linearGradient(
        colors = listOf(colors.contentGradientStart, colors.contentGradientEnd),
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(gradientBackground),
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

            // Tab selector (matching v1 styling)
            QueueTabSelector(
                activeTab = activeTab,
                queueCount = suggestions.size,
                historyCount = historyEntries.size,
                onTabChanged = onTabChanged,
            )

            Spacer(Modifier.height(Spacing.md))

            // Tab content
            when (activeTab) {
                QueueTab.Queue -> QueueContent(
                    suggestions = suggestions,
                    currentIndex = currentSuggestionIndex,
                    fieldDisplayConfig = fieldDisplayConfig,
                    onSuggestionClick = onSuggestionClick,
                )
                QueueTab.History -> HistoryContent(
                    entries = historyEntries,
                    searchQuery = historySearchQuery,
                    viewMode = historyViewMode,
                    currentSessionId = currentSession?.id,
                    fieldDisplayConfig = fieldDisplayConfig,
                    onViewModeChanged = onHistoryViewModeChanged,
                    onHistoryClick = onHistoryClick,
                )
            }
        }
    }
}

/**
 * Custom tab selector matching v1 styling:
 * - Equal-width buttons
 * - Active: bottom border, primary text, subtle background tint
 * - Inactive: muted text with hover
 */
@Composable
private fun QueueTabSelector(
    activeTab: QueueTab,
    queueCount: Int,
    historyCount: Int,
    onTabChanged: (QueueTab) -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        // Queue tab
        QueueTabButton(
            text = "Queue",
            count = queueCount,
            isActive = activeTab == QueueTab.Queue,
            onClick = { onTabChanged(QueueTab.Queue) },
            modifier = Modifier.weight(1f),
        )

        // History tab
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
private fun QueueTabButton(
    text: String,
    count: Int,
    isActive: Boolean,
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
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                    color = textColor,
                )
                if (count > 0) {
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "($count)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
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
                    text = "Session ${session.id}",
                    style = MaterialTheme.typography.labelMedium,
                )
                SessionStateChip(session.state, small = true)
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
private fun QueueContent(
    suggestions: List<Suggestion>,
    currentIndex: Int,
    fieldDisplayConfig: Map<String, String>,
    onSuggestionClick: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val pendingSuggestions = suggestions.filter { it.status == SuggestionStatus.Pending }
    val doneSuggestions = suggestions.filter { it.status != SuggestionStatus.Pending }

    Column(modifier = Modifier.fillMaxSize()) {
        if (suggestions.isEmpty()) {
            // Simple empty state (matching V1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No cards in queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(suggestions) { index, suggestion ->
                    QueueCard(
                        suggestion = suggestion,
                        index = index + 1,  // 1-based display
                        isCurrent = index == currentIndex,
                        fieldDisplayConfig = fieldDisplayConfig,
                        onClick = { onSuggestionClick(index) },
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
    fieldDisplayConfig: Map<String, String>,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    // Get field value based on display config
    val displayValue = com.ankideku.util.getDisplayField(
        modelName = suggestion.modelName,
        fields = suggestion.originalFields,
        fieldDisplayConfig = fieldDisplayConfig,
    ).ifBlank { "Note #${suggestion.noteId}" }

    val cardBackground = if (isCurrent) {
        Brush.horizontalGradient(
            colors = listOf(
                colors.accent.copy(alpha = 0.15f),
                colors.accent.copy(alpha = 0.05f),
            )
        )
    } else {
        // Use surface color for lighter appearance
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
                    text = displayValue,
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
    fieldDisplayConfig: Map<String, String>,
    onViewModeChanged: (HistoryViewMode) -> Unit,
    onHistoryClick: (HistoryEntry) -> Unit,
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
                modifier = Modifier.handPointer(),
                label = { Text("Current Session") },
                leadingIcon = if (viewMode == HistoryViewMode.Session) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
            FilterChip(
                selected = viewMode == HistoryViewMode.Global,
                onClick = { onViewModeChanged(HistoryViewMode.Global) },
                modifier = Modifier.handPointer(),
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
                        fieldDisplayConfig = fieldDisplayConfig,
                        onClick = { onHistoryClick(entry) },
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
    fieldDisplayConfig: Map<String, String>,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val actionColor = when (entry.action) {
        ReviewAction.Accept -> colors.success
        ReviewAction.Reject -> colors.error
    }

    // Get field value based on display config
    val displayText = com.ankideku.util.getDisplayField(
        modelName = entry.modelName,
        fields = entry.originalFields,
        fieldDisplayConfig = fieldDisplayConfig,
        maxLength = 50,
    ).ifBlank { "Note #${entry.noteId}" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableWithPointer(onClick = onClick),
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
