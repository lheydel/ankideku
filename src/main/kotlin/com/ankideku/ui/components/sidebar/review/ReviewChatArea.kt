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
    colors: AppColorScheme,
    onApplySuggestion: (Long) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
    onResetConversation: () -> Unit,
    onEndSession: () -> Unit,
    onDeleteMemory: (String) -> Unit,
    onOpenConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMemoryExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Header with actions
        ReviewChatHeader(
            memoryCount = state.memory.size,
            colors = colors,
            onResetConversation = onResetConversation,
            onEndSession = onEndSession,
            onOpenConfig = onOpenConfig,
            onToggleMemory = { isMemoryExpanded = !isMemoryExpanded },
            isMemoryExpanded = isMemoryExpanded,
        )

        // Expandable memory viewer
        AnimatedVisibility(
            visible = isMemoryExpanded && state.memory.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            MemoryViewer(
                memory = state.memory,
                colors = colors,
                onDeleteMemory = onDeleteMemory,
            )
        }

        HorizontalDivider(color = colors.borderMuted, thickness = 1.dp)

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
    memoryCount: Int,
    colors: AppColorScheme,
    onResetConversation: () -> Unit,
    onEndSession: () -> Unit,
    onOpenConfig: () -> Unit,
    onToggleMemory: () -> Unit,
    isMemoryExpanded: Boolean,
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "Review Chat",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                // Memory badge (clickable to expand)
                if (memoryCount > 0) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip(containerColor = colors.surfaceAlt) {
                                Text(
                                    text = if (isMemoryExpanded) "Hide stored instructions" else "View stored instructions",
                                    color = colors.textPrimary,
                                )
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        Surface(
                            color = if (isMemoryExpanded) colors.accent else colors.accentMuted,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .handPointer()
                                .clickable(onClick = onToggleMemory),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isMemoryExpanded) colors.surface else colors.accent,
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "$memoryCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMemoryExpanded) colors.surface else colors.accent,
                                )
                                Icon(
                                    imageVector = if (isMemoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isMemoryExpanded) colors.surface else colors.accent,
                                )
                            }
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

/**
 * Expandable panel showing stored AI instructions (memory).
 */
@Composable
private fun MemoryViewer(
    memory: Map<String, String>,
    colors: AppColorScheme,
    onDeleteMemory: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceAlt,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
        ) {
            Text(
                text = "Stored Instructions",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
            memory.entries.forEach { (key, value) ->
                MemoryEntry(
                    key = key,
                    value = value,
                    colors = colors,
                    onDelete = { onDeleteMemory(key) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryEntry(
    key: String,
    value: String,
    colors: AppColorScheme,
    onDelete: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = colors.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .handPointer()
                        .clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp),
                        tint = colors.textMuted,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip(containerColor = colors.surfaceAlt) {
                            Text("Delete instruction", color = colors.textPrimary)
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    AppIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete instruction",
                            modifier = Modifier.size(14.dp),
                            tint = colors.textMuted,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = Spacing.xs, start = Spacing.md + Spacing.xs),
                )
            }
            if (!isExpanded) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Spacing.md + Spacing.xs),
                )
            }
        }
    }
}
