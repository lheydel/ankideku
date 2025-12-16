package com.ankideku.ui.components.sidebar.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppIconButton
import com.ankideku.ui.components.ButtonTooltip
import com.ankideku.ui.screens.main.ReviewChatMessage
import com.ankideku.ui.screens.main.ReviewSessionState
import com.ankideku.ui.screens.main.ReviewSuggestionUi
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewChatArea(
    state: ReviewSessionState,
    currentViewedSessionId: Long?,
    colors: AppColorScheme,
    onApplySuggestion: (Long) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
    onResetConversation: () -> Unit,
    onEndSession: () -> Unit,
    onDeleteMemory: (String) -> Unit,
    onSaveMemory: (String, String) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenConfig: () -> Unit,
    onNavigateToSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMemoryDialogOpen by remember { mutableStateOf(false) }
    var memoryWasModified by remember { mutableStateOf(false) }
    val isViewingDifferentSession = state.activeSessionId != null &&
        currentViewedSessionId != null &&
        state.activeSessionId != currentViewedSessionId

    // Memory dialog
    if (isMemoryDialogOpen) {
        MemoryDialog(
            memory = state.memory,
            colors = colors,
            onDeleteMemory = { key ->
                onDeleteMemory(key)
                memoryWasModified = true
            },
            onSaveMemory = { key, value ->
                onSaveMemory(key, value)
                memoryWasModified = true
            },
            onDismiss = {
                isMemoryDialogOpen = false
                if (memoryWasModified) {
                    onSendMessage("[Memory updated] I've updated my stored instructions. Please acknowledge this update.")
                    memoryWasModified = false
                }
            },
        )
    }

    Column(modifier = modifier) {
        // Header with actions
        ReviewChatHeader(
            activeSessionId = state.activeSessionId,
            memoryCount = state.memory.size,
            colors = colors,
            onResetConversation = onResetConversation,
            onEndSession = onEndSession,
            onOpenConfig = onOpenConfig,
            onOpenMemory = { isMemoryDialogOpen = true },
            onNavigateToSession = onNavigateToSession,
        )

        HorizontalDivider(color = colors.borderMuted, thickness = 1.dp)

        // Warning banner when viewing a different session
        if (isViewingDifferentSession) {
            SessionMismatchBanner(
                activeSessionId = state.activeSessionId,
                colors = colors,
                onNavigateToSession = onNavigateToSession,
            )
        }

        // Messages area
        ReviewMessagesArea(
            messages = state.messages,
            pendingSuggestions = state.pendingSuggestions,
            isLoading = state.isLoading,
            colors = colors,
            onApplySuggestion = onApplySuggestion,
            onDismissSuggestion = onDismissSuggestion,
            modifier = Modifier.weight(1f),
        )

        // Error message
        if (state.error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.errorMuted,
            ) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewChatHeader(
    activeSessionId: Long?,
    memoryCount: Int,
    colors: AppColorScheme,
    onResetConversation: () -> Unit,
    onEndSession: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMemory: () -> Unit,
    onNavigateToSession: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceAlt,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Review Chat",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                // Session indicator (clickable to navigate)
                if (activeSessionId != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textMuted,
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip(containerColor = colors.surfaceAlt) {
                                Text("Go to session #$activeSessionId", color = colors.textPrimary)
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        Text(
                            text = "#$activeSessionId",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.accent,
                            modifier = Modifier
                                .handPointer()
                                .clickable { onNavigateToSession(activeSessionId) },
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.xs))
                // Memory button with brain icon
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(containerColor = colors.surfaceAlt) {
                            Text(
                                text = if (memoryCount > 0) "View stored instructions ($memoryCount)" else "No stored instructions",
                                color = colors.textPrimary,
                            )
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    BadgedBox(
                        badge = {
                            if (memoryCount > 0) {
                                Badge(
                                    containerColor = colors.accent,
                                    contentColor = colors.surface,
                                ) {
                                    Text("$memoryCount")
                                }
                            }
                        },
                    ) {
                        AppIconButton(
                            onClick = onOpenMemory,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Memory",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Reset button with tooltip
                AppButton(
                    onClick = onResetConversation,
                    variant = AppButtonVariant.Text,
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
                    tooltip = ButtonTooltip(
                        title = "Why reset?",
                        description = "If the AI starts forgetting your instructions or making repeated mistakes, resetting the chat can help.",
                        highlight = "Your saved instructions will be preserved.",
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "Reset chat",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }

                // Settings icon button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(containerColor = colors.surfaceAlt) {
                            Text("Settings", color = colors.textPrimary)
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    AppIconButton(
                        onClick = onOpenConfig,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // End session icon button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(containerColor = colors.surfaceAlt) {
                            Text("End review session", color = colors.textPrimary)
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    AppIconButton(
                        onClick = onEndSession,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "End review session",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewMessagesArea(
    messages: List<ReviewChatMessage>,
    pendingSuggestions: List<ReviewSuggestionUi>,
    isLoading: Boolean,
    colors: AppColorScheme,
    onApplySuggestion: (Long) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.chatBackground),
    ) {
        if (messages.isEmpty() && pendingSuggestions.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Ask the AI to review suggestions",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Messages
                items(messages, key = { it.id }) { message ->
                    ReviewChatBubble(
                        message = message,
                        colors = colors,
                    )
                }

                // Pending suggestions
                if (pendingSuggestions.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = "Pending Suggestions",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                        )
                    }
                    items(pendingSuggestions, key = { it.id }) { suggestion ->
                        ReviewSuggestionCard(
                            suggestion = suggestion,
                            colors = colors,
                            onApply = { onApplySuggestion(suggestion.id) },
                            onDismiss = { onDismissSuggestion(suggestion.id) },
                        )
                    }
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = "Thinking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionMismatchBanner(
    activeSessionId: Long,
    colors: AppColorScheme,
    onNavigateToSession: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.warningMuted,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.warning,
                )
                Text(
                    text = "Viewing different session",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.warning,
                )
            }
            AppButton(
                onClick = { onNavigateToSession(activeSessionId) },
                variant = AppButtonVariant.Text,
                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
            ) {
                Text(
                    text = "Go to active",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                )
            }
        }
    }
}
