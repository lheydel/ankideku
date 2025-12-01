package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

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
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    onEditField: (String, String) -> Unit,
    onToggleEditMode: () -> Unit,
    onToggleOriginal: () -> Unit,
    onBackToSessions: () -> Unit,
    onRevertEdits: () -> Unit,
    onCloseHistoryView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    val copyToClipboard: (String) -> Unit = { text ->
        scope.launch { clipboard.setClipEntry(ClipEntry(AnnotatedString(text))) }
    }

    // V1-style gradient background (diagonal)
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
            if (historyEntry != null) {
                HistoryContent(
                    entry = historyEntry,
                    onClose = onCloseHistoryView,
                    onCopy = copyToClipboard,
                )
            } else {
                SuggestionContent(
                    suggestion = suggestion,
                    session = session,
                    suggestions = suggestions,
                    currentIndex = currentIndex,
                    editedFields = editedFields,
                    isEditMode = isEditMode,
                    hasManualEdits = hasManualEdits,
                    showOriginal = showOriginal,
                    isActionLoading = isActionLoading,
                    isProcessing = isProcessing,
                    onAccept = onAccept,
                    onReject = onReject,
                    onSkip = onSkip,
                    onEditField = onEditField,
                    onToggleEditMode = onToggleEditMode,
                    onToggleOriginal = onToggleOriginal,
                    onBackToSessions = onBackToSessions,
                    onRevertEdits = onRevertEdits,
                    onCopy = copyToClipboard,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.HistoryContent(
    entry: HistoryEntry,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
) {
    HistoryBreadcrumb(onClose = onClose)
    Spacer(Modifier.height(Spacing.sm))

    HistoryHeaderCard(entry = entry, onCopy = onCopy)
    Spacer(Modifier.height(Spacing.md))

    if (!entry.reasoning.isNullOrBlank()) {
        ReasoningCard(reasoning = entry.reasoning)
        Spacer(Modifier.height(Spacing.md))
    }

    // Two-column comparison
    ComparisonRow(
        originalFields = entry.originalFields,
        changes = entry.appliedChanges ?: entry.aiChanges,
        modifier = Modifier.weight(1f),
    ) {
        // Changes card (right column) - History mode
        ChangesCard(
            fields = entry.originalFields,
            changes = entry.appliedChanges ?: entry.aiChanges,
            userEdits = entry.userEdits,
            mode = ChangesCardMode.History(
                action = entry.action,
                hasUserEdits = !entry.userEdits.isNullOrEmpty(),
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColumnScope.SuggestionContent(
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
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    onEditField: (String, String) -> Unit,
    onToggleEditMode: () -> Unit,
    onToggleOriginal: () -> Unit,
    onBackToSessions: () -> Unit,
    onRevertEdits: () -> Unit,
    onCopy: (String) -> Unit,
) {
    Breadcrumb(text = "Back to Sessions", onClick = onBackToSessions)
    Spacer(Modifier.height(Spacing.sm))

    if (suggestion == null) {
        EmptyState(
            isProcessing = isProcessing,
            processedCards = session?.progress?.processedCards ?: 0,
            totalCards = session?.progress?.totalCards ?: 0,
        )
    } else {
        SuggestionHeaderCard(
            deckName = session?.deckName ?: "Unknown Deck",
            currentIndex = currentIndex,
            totalSuggestions = suggestions.size,
            suggestion = suggestion,
            editedFields = editedFields,
            onCopy = onCopy,
        )
        Spacer(Modifier.height(Spacing.md))

        ReasoningCard(reasoning = suggestion.reasoning)
        Spacer(Modifier.height(Spacing.md))

        // Two-column comparison
        ComparisonRow(
            originalFields = suggestion.originalFields,
            changes = suggestion.changes.plus(editedFields),
            modifier = Modifier.weight(1f),
        ) {
            // Changes card (right column) - Suggestion mode
            ChangesCard(
                fields = suggestion.originalFields,
                changes = suggestion.changes,
                editedFields = editedFields,
                mode = ChangesCardMode.Suggestion(
                    isEditMode = isEditMode,
                    hasManualEdits = hasManualEdits,
                    showOriginal = showOriginal,
                    onToggleEditMode = onToggleEditMode,
                    onToggleOriginal = onToggleOriginal,
                    onRevertEdits = onRevertEdits,
                ),
                onEditField = onEditField,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.md))

        ActionButtons(
            onAccept = onAccept,
            onReject = onReject,
            onSkip = onSkip,
            hasManualEdits = hasManualEdits,
            isEditMode = isEditMode,
            showOriginal = showOriginal,
            isLoading = isActionLoading,
        )
    }
}

/**
 * Two-column comparison layout with Original card on left
 */
@Composable
private fun ComparisonRow(
    originalFields: Map<String, NoteField>,
    changes: Map<String, String>,
    modifier: Modifier = Modifier,
    changesCard: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OriginalCard(
            fields = originalFields,
            changes = changes,
            modifier = Modifier.weight(1f),
        )
        changesCard()
    }
}

@Composable
private fun SuggestionHeaderCard(
    deckName: String,
    currentIndex: Int,
    totalSuggestions: Int,
    suggestion: Suggestion,
    editedFields: Map<String, String>,
    onCopy: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(colors.surface, colors.surfaceAlt),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                IconLabel(
                    icon = Icons.Default.Folder,
                    text = deckName,
                    iconTint = colors.accentStrong,
                    textColor = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onCopy(buildCopyText(suggestion, editedFields)) },
                    modifier = Modifier.handPointer(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Copy Suggestion", style = MaterialTheme.typography.labelMedium)
                }

                Surface(
                    color = colors.accentMuted,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = colors.accentStrong)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = "${currentIndex + 1} / $totalSuggestions",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

private fun buildCopyText(suggestion: Suggestion, editedFields: Map<String, String>): String {
    return buildString {
        appendLine("## AI Reasoning")
        appendLine(suggestion.reasoning)
        appendLine()
        appendLine("## Original Card")
        suggestion.originalFields.forEach { (name, field) ->
            appendLine("**$name:** ${field.value}")
        }
        appendLine()
        appendLine("## Suggested Changes")
        suggestion.changes.forEach { (name, value) ->
            appendLine("**$name:** ${editedFields[name] ?: value}")
        }
    }
}

@Composable
private fun OriginalCard(
    fields: Map<String, NoteField>,
    changes: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(colors.surfaceAlt, MaterialTheme.colorScheme.surface),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.History, null, Modifier.size(18.dp), tint = colors.textMuted)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Original Card",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }

            HorizontalDivider(color = colors.divider)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                val sortedFields = fields.entries.sortedBy { it.value.order }
                items(sortedFields) { (fieldName, field) ->
                    val suggestedValue = changes[fieldName]
                    val isChanged = suggestedValue != null && suggestedValue != field.value

                    FieldItem(
                        fieldName = fieldName,
                        value = field.value,
                        isChanged = isChanged,
                        style = originalFieldStyle(isChanged),
                        diffContent = if (isChanged && field.value.isNotEmpty() && suggestedValue != null) {
                            { DiffHighlightedText(field.value, suggestedValue, DiffDisplayMode.Original) }
                        } else null,
                    )
                }
            }
        }
    }
}
