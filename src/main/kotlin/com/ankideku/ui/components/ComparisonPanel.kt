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
import androidx.compose.ui.graphics.vector.ImageVector
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.ReviewAction
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import kotlin.collections.component1
import kotlin.collections.component2

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

@Composable
private fun ColumnScope.ComparisonContent(
    deckName: String,
    modelName: String,
    reasoning: String?,
    originalFields: Map<String, NoteField>,
    changes: Map<String, String>,
    noteTypeConfig: NoteTypeConfig?,
    breadcrumb: @Composable () -> Unit,
    onCopy: () -> Unit,
    onOpenNoteTypeSettings: () -> Unit,
    trailingBadge: @Composable () -> Unit,
    changesHeaderStyle: HeaderStyle,
    editedFields: Map<String, String> = emptyMap(),
    isEditMode: Boolean = false,
    showOriginal: Boolean = false,
    onEditField: ((String, String) -> Unit)? = null,
    changesHeaderControls: (@Composable () -> Unit)? = null,
    actionButtons: (@Composable () -> Unit)? = null,
) {
    breadcrumb()
    Spacer(Modifier.height(Spacing.sm))

    ComparisonHeaderCard(
        deckName = deckName,
        modelName = modelName,
        onCopy = onCopy,
        onOpenNoteTypeSettings = onOpenNoteTypeSettings,
        trailingBadge = trailingBadge,
    )
    Spacer(Modifier.height(Spacing.md))

    if (!reasoning.isNullOrBlank()) {
        ReasoningCard(reasoning = reasoning)
        Spacer(Modifier.height(Spacing.md))
    }

    Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OriginalCard(
            fields = originalFields,
            changes = changes.plus(editedFields),
            noteTypeConfig = noteTypeConfig,
            modifier = Modifier.weight(1f),
        )

        ChangesCard(
            fields = originalFields,
            changes = changes,
            noteTypeConfig = noteTypeConfig,
            title = changesHeaderStyle.title,
            headerIcon = changesHeaderStyle.icon,
            headerColor = changesHeaderStyle.color,
            headerBg = changesHeaderStyle.background,
            editedFields = editedFields,
            isEditMode = isEditMode,
            showOriginal = showOriginal,
            onEditField = onEditField,
            headerControls = changesHeaderControls,
            modifier = Modifier.weight(1f),
        )
    }

    if (actionButtons != null) {
        Spacer(Modifier.height(Spacing.md))
        actionButtons()
    }
}

@Composable
private fun ComparisonHeaderCard(
    deckName: String,
    modelName: String,
    onCopy: () -> Unit,
    onOpenNoteTypeSettings: () -> Unit,
    trailingBadge: @Composable () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                IconLabel(
                    icon = Icons.Default.Folder,
                    text = deckName,
                    iconTint = colors.accentStrong,
                    textColor = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )

                if (modelName.isNotBlank()) {
                    Text("|", color = colors.textMuted, style = MaterialTheme.typography.bodyMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.Description, null, Modifier.size(16.dp), tint = colors.secondary)
                        Text(modelName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                        IconButton(onClick = onOpenNoteTypeSettings, modifier = Modifier.size(24.dp).handPointer()) {
                            Icon(Icons.Default.Settings, "Configure note type", Modifier.size(14.dp), tint = colors.textMuted)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppButton(
                    onClick = onCopy,
                    variant = AppButtonVariant.Outlined,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }
                trailingBadge()
            }
        }
    }
}

private fun buildSuggestionCopyText(suggestion: Suggestion, editedFields: Map<String, String>): String {
    return buildCopyText(
        reasoning = suggestion.reasoning,
        originalFields = suggestion.originalFields,
        changes = suggestion.changes,
        editedFields = editedFields,
    )
}

private fun buildHistoryCopyText(entry: HistoryEntry): String {
    return buildCopyText(
        reasoning = entry.reasoning.orEmpty(),
        originalFields = entry.originalFields,
        changes = entry.appliedChanges ?: entry.aiChanges,
        editedFields = emptyMap(),
    )
}

private fun buildCopyText(
    reasoning: String,
    originalFields: Map<String, NoteField>,
    changes: Map<String, String>,
    editedFields: Map<String, String>,
) = buildString {
    appendLine("## AI Reasoning")
    appendLine(reasoning)
    appendLine()
    appendLine("## Original Card")
    originalFields.forEach { (name, field) -> appendLine("**$name:** ${field.value}") }
    appendLine()
    appendLine("## Changes")
    changes.forEach { (name, value) -> appendLine("**$name:** ${editedFields[name] ?: value}") }
}

@Composable
private fun OriginalCard(
    fields: Map<String, NoteField>,
    changes: Map<String, String>,
    noteTypeConfig: NoteTypeConfig?,
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
                    .background(Brush.horizontalGradient(listOf(colors.surfaceAlt, MaterialTheme.colorScheme.surface)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.History, null, Modifier.size(18.dp), tint = colors.textMuted)
                Spacer(Modifier.width(Spacing.sm))
                Text("Original Card", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
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
                        noteTypeConfig = noteTypeConfig,
                        style = originalFieldStyle(isChanged),
                        diffContent = if (isChanged && field.value.isNotEmpty()) {
                            { DiffHighlightedText(field.value, suggestedValue, DiffDisplayMode.Original) }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
fun getSuggestionHeaderStyle(isEditMode: Boolean, hasManualEdits: Boolean, showOriginal: Boolean): HeaderStyle {
    val colors = LocalAppColors.current
    return when {
        isEditMode && showOriginal -> HeaderStyle("AI Suggested", colors.secondary, colors.secondaryMuted, Icons.Default.SmartToy)
        isEditMode -> HeaderStyle("Editing...", colors.warning, colors.warningMuted, Icons.Default.Edit)
        hasManualEdits -> HeaderStyle("Manually Edited", colors.warning, colors.warningMuted, Icons.Default.Edit)
        else -> HeaderStyle("Suggested Card", colors.accent, colors.accentMuted, Icons.Default.AutoAwesome)
    }
}

@Composable
fun getHistoryHeaderStyle(accepted: Boolean): HeaderStyle {
    val colors = LocalAppColors.current
    return if (accepted) {
        HeaderStyle("Applied Changes", colors.success, colors.successMuted, Icons.Default.Check)
    } else {
        HeaderStyle("Rejected Changes", colors.error, colors.errorMuted, Icons.Default.Close)
    }
}

data class HeaderStyle(
    val title: String,
    val color: Color,
    val background: Color,
    val icon: ImageVector,
)
