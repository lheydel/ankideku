package com.ankideku.ui.components.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.ankideku.domain.model.*
import com.ankideku.ui.screens.main.BatchProgress
import com.ankideku.ui.screens.main.HistoryViewMode
import com.ankideku.ui.screens.main.QueueTab
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

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
