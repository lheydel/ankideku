package com.ankideku.ui.components.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import com.ankideku.ui.components.AppIconButton

/**
 * Pre-session note list content - shows deck notes before starting a session.
 */
@Composable
fun NoteListContent(
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
                AppIconButton(
                    onClick = onOpenNoteFilter,
                    modifier = Modifier.size(32.dp),
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
