package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Session
import com.ankideku.ui.components.ProcessingIndicator
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.SyncProgressUi
import com.ankideku.ui.theme.LocalAppColors

@Composable
fun SidebarPanel(
    decks: List<Deck>,
    selectedDeck: Deck?,
    chatMessages: List<ChatMessage>,
    isConnected: Boolean,
    isSyncing: Boolean,
    syncProgress: SyncProgressUi?,
    isProcessing: Boolean,
    canStartSession: Boolean,
    currentSession: Session?,
    forceSyncBeforeStart: Boolean,
    llmProvider: LlmProvider,
    // Note filter
    noteFilterCount: Int?,
    totalNoteCount: Int,
    onOpenNoteFilter: () -> Unit,
    onClearNoteFilter: () -> Unit,
    // Callbacks
    onDeckSelected: (Deck) -> Unit,
    onRefreshDecks: () -> Unit,
    onSyncDeck: () -> Unit,
    onStartSession: (prompt: String) -> Unit,
    onCancelSession: () -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: () -> Unit,
    onForceSyncChanged: (Boolean) -> Unit,
    onCloseSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header with gradient
            SidebarHeader(
                currentSession = currentSession,
                colors = colors,
                onNewSession = onNewSession,
                onDeleteSession = onDeleteSession,
                onCloseSidebar = onCloseSidebar,
            )

            // Deck Selector section
            DeckSelectorSection(
                decks = decks,
                selectedDeck = selectedDeck,
                isSyncing = isSyncing,
                isProcessing = isProcessing,
                isConnected = isConnected,
                syncProgress = syncProgress,
                currentSession = currentSession,
                noteFilterCount = noteFilterCount,
                totalNoteCount = totalNoteCount,
                colors = colors,
                onDeckSelected = onDeckSelected,
                onRefreshDecks = onRefreshDecks,
                onSyncDeck = onSyncDeck,
                onOpenNoteFilter = onOpenNoteFilter,
                onClearNoteFilter = onClearNoteFilter,
            )

            // Border after deck selector
            HorizontalDivider(color = colors.borderMuted, thickness = 1.dp)

            // Processing Indicator (after deck selector)
            if (isProcessing && currentSession != null) {
                ProcessingIndicator(
                    progress = currentSession.progress,
                    onCancel = onCancelSession,
                    colors = colors,
                )
                HorizontalDivider(color = colors.accentMuted, thickness = 1.dp)
            }

            // Chat Messages area
            ChatMessagesArea(
                chatMessages = chatMessages,
                selectedDeck = selectedDeck,
                colors = colors,
                modifier = Modifier.weight(1f),
            )

            // Chat Input area at bottom (only when no active session)
            if (currentSession == null) {
                ChatInputArea(
                    isSyncing = isSyncing,
                    canStartSession = canStartSession,
                    selectedDeck = selectedDeck,
                    forceSyncBeforeStart = forceSyncBeforeStart,
                    llmProvider = llmProvider,
                    colors = colors,
                    onForceSyncChanged = onForceSyncChanged,
                    onStartSession = onStartSession,
                )
            }
        }
    }
}
