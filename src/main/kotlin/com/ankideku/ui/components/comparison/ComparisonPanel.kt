package com.ankideku.ui.components.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.ReviewAction
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import com.ankideku.ui.components.ActionBadge
import com.ankideku.ui.components.ActionButtons
import com.ankideku.ui.components.Breadcrumb
import com.ankideku.ui.components.EmptyState
import com.ankideku.ui.components.HistoryBreadcrumb
import com.ankideku.ui.components.SuggestionEditControls
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

@Composable
fun ComparisonPanel(
    suggestion: Suggestion?,
    session: Session?,
    suggestions: List<Suggestion>,
    currentIndex: Int,
    editedFields: Map<String, String>,
    isEditMode: Boolean,
    hasManualEdits: Boolean,
    showOriginal: Boolean,
    isActionLoading: Boolean,
    isProcessing: Boolean,
    historyEntry: HistoryEntry?,
    noteTypeConfigs: Map<String, NoteTypeConfig>,
    // Pre-session note preview
    previewNote: Note? = null,
    selectedDeck: Deck? = null,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    onEditField: (String, String) -> Unit,
    onToggleEditMode: () -> Unit,
    onToggleOriginal: () -> Unit,
    onBackToSessions: () -> Unit,
    onRevertEdits: () -> Unit,
    onCloseHistoryView: () -> Unit,
    onOpenNoteTypeSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    val copyToClipboard: (String) -> Unit = { text ->
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

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
            when {
                historyEntry != null -> {
                    val noteTypeConfig = noteTypeConfigs[historyEntry.modelName]
                    val changes = historyEntry.appliedChanges ?: historyEntry.aiChanges
                    val accepted = historyEntry.action == ReviewAction.Accept

                    ComparisonContent(
                        deckName = historyEntry.deckName,
                        modelName = historyEntry.modelName,
                        reasoning = historyEntry.reasoning,
                        originalFields = historyEntry.originalFields,
                        changes = changes,
                        noteTypeConfig = noteTypeConfig,
                        breadcrumb = { HistoryBreadcrumb(onClose = onCloseHistoryView) },
                        onCopy = { copyToClipboard(buildHistoryCopyText(historyEntry)) },
                        onOpenNoteTypeSettings = { onOpenNoteTypeSettings(historyEntry.modelName) },
                        trailingBadge = {
                            ActionBadge(action = historyEntry.action, color = if (accepted) colors.success else colors.error)
                        },
                        changesHeaderStyle = getHistoryHeaderStyle(accepted),
                    )
                }

                // Pre-session note preview mode
                previewNote != null -> {
                    val noteTypeConfig = noteTypeConfigs[previewNote.modelName]
                    NotePreviewContent(
                        note = previewNote,
                        deckName = selectedDeck?.name ?: previewNote.deckName,
                        noteTypeConfig = noteTypeConfig,
                        onCopy = { copyToClipboard(buildNoteCopyText(previewNote)) },
                        onOpenNoteTypeSettings = { onOpenNoteTypeSettings(previewNote.modelName) },
                        onBackToSessions = onBackToSessions,
                    )
                }

                suggestion == null -> {
                    Breadcrumb(text = "Back to Sessions", onClick = onBackToSessions)
                    Spacer(Modifier.height(Spacing.sm))
                    EmptyState(
                        isProcessing = isProcessing,
                        processedCards = session?.progress?.processedCards ?: 0,
                        totalCards = session?.progress?.totalCards ?: 0,
                    )
                }

                else -> {
                    val noteTypeConfig = noteTypeConfigs[suggestion.modelName]

                    ComparisonContent(
                        deckName = session?.deckName ?: "Unknown Deck",
                        modelName = suggestion.modelName,
                        reasoning = suggestion.reasoning,
                        originalFields = suggestion.originalFields,
                        changes = suggestion.changes,
                        noteTypeConfig = noteTypeConfig,
                        breadcrumb = { Breadcrumb(text = "Back to Sessions", onClick = onBackToSessions) },
                        onCopy = { copyToClipboard(buildSuggestionCopyText(suggestion, editedFields)) },
                        onOpenNoteTypeSettings = { onOpenNoteTypeSettings(suggestion.modelName) },
                        trailingBadge = {
                            Surface(color = colors.accentMuted, shape = RoundedCornerShape(50)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = colors.accentStrong)
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text(
                                        text = "${currentIndex + 1} / ${suggestions.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        },
                        changesHeaderStyle = getSuggestionHeaderStyle(isEditMode, hasManualEdits, showOriginal),
                        editedFields = editedFields,
                        isEditMode = isEditMode,
                        showOriginal = showOriginal,
                        onEditField = onEditField,
                        changesHeaderControls = {
                            SuggestionEditControls(
                                isEditMode = isEditMode,
                                hasManualEdits = hasManualEdits,
                                showOriginal = showOriginal,
                                onToggleEditMode = onToggleEditMode,
                                onToggleOriginal = onToggleOriginal,
                                onRevertEdits = onRevertEdits,
                            )
                        },
                        actionButtons = {
                            ActionButtons(
                                onAccept = onAccept,
                                onReject = onReject,
                                onSkip = onSkip,
                                hasManualEdits = hasManualEdits,
                                isEditMode = isEditMode,
                                showOriginal = showOriginal,
                                isLoading = isActionLoading,
                            )
                        },
                    )
                }
            }
        }
    }
}
