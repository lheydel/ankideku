package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Session
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.DeckSelector
import com.ankideku.ui.screens.main.SyncProgressUi
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun DeckSelectorSection(
    decks: List<Deck>,
    selectedDeck: Deck?,
    isSyncing: Boolean,
    isProcessing: Boolean,
    isConnected: Boolean,
    syncProgress: SyncProgressUi?,
    currentSession: Session?,
    noteFilterCount: Int?,
    totalNoteCount: Int,
    colors: AppColorScheme,
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
fun NoteFilterRow(
    noteFilterCount: Int?,
    totalNoteCount: Int,
    onOpenFilter: () -> Unit,
    onClearFilter: () -> Unit,
    colors: AppColorScheme,
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

internal fun Int.formatWithCommas(): String = "%,d".format(this)
