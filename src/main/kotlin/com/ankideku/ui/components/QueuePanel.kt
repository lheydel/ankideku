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
import com.ankideku.ui.components.batch.BatchActionBar
import com.ankideku.ui.screens.main.BatchProgress
import com.ankideku.util.toJson
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
    noteTypeConfigs: Map<String, NoteTypeConfig>,
    // Batch filter mode
    isInBatchFilterMode: Boolean = false,
    isBatchProcessing: Boolean = false,
    batchProgress: BatchProgress? = null,
    // Pre-session note browsing
    notes: List<Note> = emptyList(),
    selectedNoteIndex: Int = 0,
    hasNoteFilter: Boolean = false,
    onNoteClick: (Int) -> Unit = {},
    onOpenNoteFilter: (() -> Unit)? = null,
    onClearNoteFilter: (() -> Unit)? = null,
    onTabChanged: (QueueTab) -> Unit,
    onHistoryViewModeChanged: (HistoryViewMode) -> Unit,
    onSuggestionClick: (Int) -> Unit,
    onHistoryClick: (HistoryEntry) -> Unit,
    // Batch actions
    onOpenBatchFilter: (() -> Unit)? = null,
    onClearBatchFilter: (() -> Unit)? = null,
    onBatchAcceptAll: (() -> Unit)? = null,
    onBatchRejectAll: (() -> Unit)? = null,
    onRefreshBaselines: (() -> Unit)? = null,
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

            // Tab selector - show Notes+History pre-session, Queue+History during session
            val isPreSession = currentSession == null
            QueueTabSelector(
                activeTab = activeTab,
                isPreSession = isPreSession,
                notesCount = notes.size,
                queueCount = suggestions.size,
                historyCount = historyEntries.size,
                hasNoteFilter = hasNoteFilter,
                onTabChanged = onTabChanged,
            )

            Spacer(Modifier.height(Spacing.md))

            // Tab content
            when (activeTab) {
                QueueTab.Notes -> NoteListContent(
                    notes = notes,
                    selectedIndex = selectedNoteIndex,
                    noteTypeConfigs = noteTypeConfigs,
                    hasNoteFilter = hasNoteFilter,
                    onNoteClick = onNoteClick,
                    onOpenNoteFilter = onOpenNoteFilter,
                    onClearNoteFilter = onClearNoteFilter,
                )
                QueueTab.Queue -> QueueContent(
                    suggestions = suggestions,
                    session = currentSession,
                    currentIndex = currentSuggestionIndex,
                    noteTypeConfigs = noteTypeConfigs,
                    onSuggestionClick = onSuggestionClick,
                    isInBatchFilterMode = isInBatchFilterMode,
                    isBatchProcessing = isBatchProcessing,
                    batchProgress = batchProgress,
                    onOpenBatchFilter = onOpenBatchFilter,
                    onClearBatchFilter = onClearBatchFilter,
                    onBatchAcceptAll = onBatchAcceptAll,
                    onBatchRejectAll = onBatchRejectAll,
                    onRefreshBaselines = onRefreshBaselines,
                )
                QueueTab.History -> HistoryContent(
                    entries = historyEntries,
                    searchQuery = historySearchQuery,
                    viewMode = historyViewMode,
                    currentSessionId = currentSession?.id,
                    noteTypeConfigs = noteTypeConfigs,
                    onViewModeChanged = onHistoryViewModeChanged,
                    onHistoryClick = onHistoryClick,
                )
            }
        }
    }
}

/**
 * Custom tab selector:
 * - Shows Notes+History in pre-session mode, Queue+History during active session
 * - Equal-width buttons
 * - Active: bottom border, primary text, subtle background tint
 * - Inactive: muted text with hover
 */
@Composable
private fun QueueTabSelector(
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
private fun QueueTabButton(
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
    session: Session?,
    currentIndex: Int,
    noteTypeConfigs: Map<String, NoteTypeConfig>,
    onSuggestionClick: (Int) -> Unit,
    // Batch mode
    isInBatchFilterMode: Boolean = false,
    isBatchProcessing: Boolean = false,
    batchProgress: BatchProgress? = null,
    onOpenBatchFilter: (() -> Unit)? = null,
    onClearBatchFilter: (() -> Unit)? = null,
    onBatchAcceptAll: (() -> Unit)? = null,
    onBatchRejectAll: (() -> Unit)? = null,
    onRefreshBaselines: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val progress = session?.progress
    val pendingCount = suggestions.size
    val doneCount = (progress?.suggestionsCount ?: 0) - pendingCount

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row with filter button
        if ((progress?.suggestionsCount ?: 0) > 0 || isInBatchFilterMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isInBatchFilterMode) {
                    // Filter active indicator with match count and clear button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.accent,
                        )
                        Text(
                            text = "Filter active",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                        )
                        Text(
                            text = "${suggestions.size} match${if (suggestions.size != 1) "es" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                        if (onClearBatchFilter != null) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textMuted,
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent,
                                modifier = Modifier.clickableWithPointer { onClearBatchFilter() },
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row {
                    // Refresh baselines button
                    if (onRefreshBaselines != null && !isInBatchFilterMode) {
                        IconButton(
                            onClick = onRefreshBaselines,
                            modifier = Modifier.size(32.dp).handPointer(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh baselines from Anki",
                                modifier = Modifier.size(20.dp),
                                tint = colors.textSecondary,
                            )
                        }
                    }

                    // Filter button
                    if (onOpenBatchFilter != null && !isInBatchFilterMode) {
                        IconButton(
                            onClick = onOpenBatchFilter,
                            modifier = Modifier.size(32.dp).handPointer(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter suggestions",
                                modifier = Modifier.size(20.dp),
                                tint = colors.textSecondary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        if (suggestions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isInBatchFilterMode) "No suggestions match filter" else "No cards in queue",
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
                        noteTypeConfig = noteTypeConfigs[suggestion.modelName],
                        onClick = { onSuggestionClick(index) },
                    )
                }
            }
        }

        // Batch action bar (when in filter mode)
        if (isInBatchFilterMode && onBatchAcceptAll != null && onBatchRejectAll != null) {
            Spacer(Modifier.height(Spacing.sm))
            BatchActionBar(
                matchCount = suggestions.size,
                isProcessing = isBatchProcessing,
                onRejectAll = onBatchRejectAll,
                onAcceptAll = onBatchAcceptAll,
                batchProgress = batchProgress,
            )
        } else if (suggestions.isNotEmpty() && !isInBatchFilterMode) {
            // Normal progress footer with Done / Left counts
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
                        text = "Done: $doneCount",
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
                        text = "Left: $pendingCount",
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
    noteTypeConfig: NoteTypeConfig?,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val (fieldName, fieldValue) = com.ankideku.util.getDisplayField(
        fields = suggestion.originalFields,
        noteTypeConfig = noteTypeConfig,
    )
    val wasSkipped = suggestion.skippedAt != null

    ItemCard(
        displayValue = fieldValue.ifBlank { "Note #${suggestion.noteId}" },
        fieldName = fieldName,
        index = index,
        isCurrent = isCurrent,
        noteTypeConfig = noteTypeConfig,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = "${suggestion.changes.size} field(s) changed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (wasSkipped) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Previously skipped",
                    modifier = Modifier.size(12.dp),
                    tint = colors.textMuted,
                )
            }
        }
    }
}

/**
 * Generic item card used by both QueueCard and NoteCard.
 */
@Composable
private fun ItemCard(
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

/**
 * Pre-session note list content - shows deck notes before starting a session.
 */
@Composable
private fun NoteListContent(
    notes: List<Note>,
    selectedIndex: Int,
    noteTypeConfigs: Map<String, NoteTypeConfig>,
    hasNoteFilter: Boolean,
    onNoteClick: (Int) -> Unit,
    onOpenNoteFilter: (() -> Unit)?,
    onClearNoteFilter: (() -> Unit)?,
) {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row with filter controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasNoteFilter) {
                // Filter active indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.accent,
                    )
                    Text(
                        text = "Filtered",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                    Text(
                        text = "${notes.size} note${if (notes.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    if (onClearNoteFilter != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            modifier = Modifier.clickableWithPointer { onClearNoteFilter() },
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Filter button (when no filter active)
            if (onOpenNoteFilter != null && !hasNoteFilter && notes.isNotEmpty()) {
                IconButton(
                    onClick = onOpenNoteFilter,
                    modifier = Modifier.size(32.dp).handPointer(),
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter notes",
                        modifier = Modifier.size(20.dp),
                        tint = colors.textSecondary,
                    )
                }
            }
        }

        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
        }

        if (notes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasNoteFilter) "No notes match filter" else "No notes in deck",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                    if (!hasNoteFilter) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "Select a deck or sync to load notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(notes) { index, note ->
                    NoteCard(
                        note = note,
                        index = index + 1,
                        isCurrent = index == selectedIndex,
                        noteTypeConfig = noteTypeConfigs[note.modelName],
                        onClick = { onNoteClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    index: Int,
    isCurrent: Boolean,
    noteTypeConfig: NoteTypeConfig?,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val (fieldName, fieldValue) = com.ankideku.util.getDisplayField(
        fields = note.fields,
        noteTypeConfig = noteTypeConfig,
    )

    ItemCard(
        displayValue = fieldValue.ifBlank { "Note #${note.id}" },
        fieldName = fieldName,
        index = index,
        isCurrent = isCurrent,
        noteTypeConfig = noteTypeConfig,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = note.modelName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
            if (note.tags.isNotEmpty()) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
                Text(
                    text = "${note.tags.size} tag${if (note.tags.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
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
    noteTypeConfigs: Map<String, NoteTypeConfig>,
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
                        noteTypeConfig = noteTypeConfigs[entry.modelName],
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
    noteTypeConfig: NoteTypeConfig?,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val actionColor = when (entry.action) {
        ReviewAction.Accept -> colors.success
        ReviewAction.Reject -> colors.error
    }

    // Get field value based on display config
    val (fieldName, fieldValue) = com.ankideku.util.getDisplayField(
        fields = entry.originalFields,
        noteTypeConfig = noteTypeConfig,
        maxLength = 50,
    )
    val displayText = fieldValue.ifBlank { "Note #${entry.noteId}" }

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
                    ConfiguredText(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fieldName = fieldName,
                        noteTypeConfig = noteTypeConfig,
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
