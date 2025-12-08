package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.ankideku.util.isEnterKey
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Session
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.SyncProgressUi
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

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

@Composable
private fun SidebarHeader(
    currentSession: Session?,
    colors: com.ankideku.ui.theme.AppColorScheme,
    onNewSession: () -> Unit,
    onDeleteSession: () -> Unit,
    onCloseSidebar: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        colors.headerGradientStart,
                        colors.headerGradientEnd,
                    )
                )
            )
            .padding(horizontal = Spacing.md, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title with chat icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.accentStrong,
                )
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Session buttons (when session is active)
                if (currentSession != null) {
                    AppButton(
                        onClick = onNewSession,
                        variant = AppButtonVariant.Outlined,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "New Session",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    DeleteSessionButton(
                        onDelete = onDeleteSession,
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(
                    onClick = onCloseSidebar,
                    modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sidebar",
                        modifier = Modifier.size(20.dp),
                        tint = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckSelectorSection(
    decks: List<Deck>,
    selectedDeck: Deck?,
    isSyncing: Boolean,
    isProcessing: Boolean,
    isConnected: Boolean,
    syncProgress: SyncProgressUi?,
    currentSession: Session?,
    noteFilterCount: Int?,
    totalNoteCount: Int,
    colors: com.ankideku.ui.theme.AppColorScheme,
    onDeckSelected: (Deck) -> Unit,
    onRefreshDecks: () -> Unit,
    onSyncDeck: () -> Unit,
    onOpenNoteFilter: () -> Unit,
    onClearNoteFilter: () -> Unit,
) {
    val hasActiveSession = currentSession != null
    // When there's an active session, show the session's deck as a readonly display
    val displayDeck = if (currentSession != null) {
        Deck(id = currentSession.deckId, name = currentSession.deckName)
    } else {
        selectedDeck
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(Spacing.md),
    ) {
        // Uppercase DECK label with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.accentStrong,
            )
            Text(
                text = "DECK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp,
            )
        }

        // Deck selector + sync button inline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckSelector(
                decks = decks,
                selectedDeck = displayDeck,
                onDeckSelected = onDeckSelected,
                onOpen = onRefreshDecks,
                enabled = !isSyncing && !isProcessing && !hasActiveSession,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onSyncDeck,
                enabled = selectedDeck != null && !isSyncing && isConnected,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .background(color = colors.surface, shape = RoundedCornerShape(8.dp))
                    .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync deck",
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Sync progress bar
        if (isSyncing && syncProgress != null) {
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Step ${syncProgress.step}/${syncProgress.totalSteps}: ${syncProgress.statusText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
                Text(
                    text = "${(syncProgress.step.toFloat() / syncProgress.totalSteps * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { syncProgress.step.toFloat() / syncProgress.totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.accent,
                trackColor = colors.border,
            )
        }

        // Cache info
        if (!isSyncing && selectedDeck?.lastSyncTimestamp != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "${selectedDeck.noteCount.formatWithCommas()} cards · ~${selectedDeck.tokenEstimate.formatWithCommas()} input tokens",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )

            // Note filter row (only when no active session)
            if (currentSession == null && totalNoteCount > 0) {
                Spacer(Modifier.height(Spacing.sm))
                NoteFilterRow(
                    noteFilterCount = noteFilterCount,
                    totalNoteCount = totalNoteCount,
                    onOpenFilter = onOpenNoteFilter,
                    onClearFilter = onClearNoteFilter,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun NoteFilterRow(
    noteFilterCount: Int?,
    totalNoteCount: Int,
    onOpenFilter: () -> Unit,
    onClearFilter: () -> Unit,
    colors: com.ankideku.ui.theme.AppColorScheme,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Filter button
        AppButton(
            onClick = onOpenFilter,
            variant = if (noteFilterCount != null) AppButtonVariant.Outlined else AppButtonVariant.Text,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (noteFilterCount != null) {
                    "$noteFilterCount of $totalNoteCount notes"
                } else {
                    "Filter Notes"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }

        // Clear button (only when filter active)
        if (noteFilterCount != null) {
            IconButton(
                onClick = onClearFilter,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear filter",
                    modifier = Modifier.size(16.dp),
                    tint = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ChatMessagesArea(
    chatMessages: List<ChatMessage>,
    selectedDeck: Deck?,
    colors: com.ankideku.ui.theme.AppColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.chatBackground),
    ) {
        if (chatMessages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.accent.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (selectedDeck == null) {
                        "Select a deck to start"
                    } else {
                        "Describe what you want to improve"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message, colors)
                }
            }
        }
    }
}

@Composable
private fun ChatInputArea(
    isSyncing: Boolean,
    canStartSession: Boolean,
    selectedDeck: Deck?,
    forceSyncBeforeStart: Boolean,
    llmProvider: LlmProvider,
    colors: com.ankideku.ui.theme.AppColorScheme,
    onForceSyncChanged: (Boolean) -> Unit,
    onStartSession: (String) -> Unit,
) {
    HorizontalDivider(color = colors.border, thickness = 1.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(Spacing.md),
    ) {
        // Checkbox row with provider indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
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
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.textMuted,
                )
                Text(
                    text = when (llmProvider) {
                        LlmProvider.CLAUDE_CODE -> "Claude Code"
                        LlmProvider.MOCK -> "Mock (Testing)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val promptState = rememberTextFieldState()

        fun submitPrompt() {
            val prompt = promptState.text.toString()
            if (prompt.isNotBlank() && canStartSession) {
                onStartSession(prompt)
                promptState.setTextAndPlaceCursorAtEnd("")
            }
        }

        OutlinedTextField(
            state = promptState,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isEnterKey()) {
                        if (!event.isShiftPressed) {
                            submitPrompt()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
            placeholder = {
                Text(
                    text = if (isSyncing) "Syncing deck..." else "Describe what you want to improve... (Shift+Enter for new line)",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            enabled = !isSyncing,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 3),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = colors.surfaceAlt,
                focusedContainerColor = colors.surfaceAlt,
                unfocusedBorderColor = colors.border,
                focusedBorderColor = colors.accent,
                unfocusedPlaceholderColor = colors.textMuted,
                focusedPlaceholderColor = colors.textMuted,
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        )

        if (selectedDeck == null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Select a deck to start",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
        }
    }
}

private fun Int.formatWithCommas(): String {
    return "%,d".format(this)
}
