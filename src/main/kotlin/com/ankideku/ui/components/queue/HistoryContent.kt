package com.ankideku.ui.components.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.ReviewAction
import com.ankideku.ui.components.ConfiguredText
import com.ankideku.ui.screens.main.HistoryViewMode
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.theme.handPointer

@Composable
fun HistoryContent(
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
