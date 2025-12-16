package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.ReviewContextConfig
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionState
import com.ankideku.ui.components.ProcessingIndicator
import com.ankideku.ui.components.dialogs.ReviewSessionConfigDialog
import com.ankideku.ui.components.sidebar.review.ReviewChatArea
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.ReviewSessionState
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
    // Review session
    reviewSessionState: ReviewSessionState,
    reviewContextConfig: ReviewContextConfig?,
    currentSuggestionId: Long?,
    availableFields: List<String>,
    onStartReviewSession: () -> Unit,
    onEndReviewSession: () -> Unit,
    onSendReviewMessage: (String, Boolean) -> Unit,
    onApplyReviewSuggestion: (Long) -> Unit,
    onDismissReviewSuggestion: (Long) -> Unit,
    onResetReviewConversation: () -> Unit,
    onDeleteMemory: (String) -> Unit,
    onUpdateReviewConfig: (ReviewContextConfig) -> Unit,
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
    var showConfigDialog by remember { mutableStateOf(false) }

    // Config dialog
    if (showConfigDialog) {
        ReviewSessionConfigDialog(
            currentConfig = reviewContextConfig,
            availableFields = availableFields,
            onDismiss = { showConfigDialog = false },
            onSave = { config ->
                onUpdateReviewConfig(config)
                showConfigDialog = false
            },
        )
    }

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

            // Chat area - switches between regular chat and review chat
            if (reviewSessionState.isActive) {
                // Review chat mode
                ReviewChatArea(
                    state = reviewSessionState,
                    colors = colors,
                    onApplySuggestion = onApplyReviewSuggestion,
                    onDismissSuggestion = onDismissReviewSuggestion,
                    onResetConversation = onResetReviewConversation,
                    onEndSession = onEndReviewSession,
                    onDeleteMemory = onDeleteMemory,
                    onOpenConfig = { showConfigDialog = true },
                    modifier = Modifier.weight(1f),
                )

                // Review chat input
                var includeContext by remember { mutableStateOf(false) }
                val hasCurrentSuggestion = currentSuggestionId != null

                ChatInputArea(
                    placeholder = "Ask the AI... (Shift+Enter for new line)",
                    enabled = !reviewSessionState.isLoading,
                    llmProvider = llmProvider,
                    colors = colors,
                    onSubmit = { message -> onSendReviewMessage(message, includeContext) },
                    checkbox = if (hasCurrentSuggestion) {
                        {
                            Checkbox(
                                checked = includeContext,
                                onCheckedChange = { includeContext = it },
                                modifier = Modifier.size(20.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accentStrong,
                                ),
                            )
                            Text(
                                text = "Include current suggestion",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    } else null,
                )
            } else {
                // Regular chat mode
                ChatMessagesArea(
                    chatMessages = chatMessages,
                    selectedDeck = selectedDeck,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )

                // Bottom area - depends on session state
                when {
                    currentSession == null -> {
                        // No session - show prompt input
                        ChatInputArea(
                            placeholder = if (isSyncing) "Syncing deck..." else "Describe what you want to improve... (Shift+Enter for new line)",
                            enabled = !isSyncing && canStartSession,
                            llmProvider = llmProvider,
                            colors = colors,
                            onSubmit = onStartSession,
                            checkbox = {
                                Checkbox(
                                    checked = forceSyncBeforeStart,
                                    onCheckedChange = onForceSyncChanged,
                                    enabled = !isSyncing,
                                    modifier = Modifier.size(20.dp).pointerHoverIcon(PointerIcon.Hand),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accentStrong,
                                    ),
                                )
                                Text(
                                    text = "Sync deck before processing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSyncing) colors.textMuted else colors.textSecondary,
                                )
                            },
                            bottomHint = if (selectedDeck == null) "Select a deck to start" else null,
                        )
                    }
                    currentSession.state == SessionState.Completed -> {
                        StartReviewSessionArea(
                            colors = colors,
                            isLoading = reviewSessionState.isLoading,
                            onStartReviewSession = onStartReviewSession,
                        )
                    }
                }
            }
        }
    }
}
