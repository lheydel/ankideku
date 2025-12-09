package com.ankideku.ui.components.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import com.ankideku.ui.components.batch.BatchActionBar
import com.ankideku.ui.screens.main.BatchProgress
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.theme.handPointer

@Composable
fun QueueContent(
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
