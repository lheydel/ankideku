package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.ReviewAction
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

/**
 * Configuration for the changes card header and styling
 */
sealed class ChangesCardMode {
    /**
     * Editable suggestion mode with Edit/Done toggle
     */
    data class Suggestion(
        val isEditMode: Boolean,
        val hasManualEdits: Boolean,
        val showOriginal: Boolean,
        val onToggleEditMode: () -> Unit,
        val onToggleOriginal: () -> Unit,
        val onRevertEdits: () -> Unit,
    ) : ChangesCardMode()

    /**
     * Read-only history mode showing the action taken
     */
    data class History(
        val action: ReviewAction,
        val hasUserEdits: Boolean,
    ) : ChangesCardMode()
}

/**
 * Unified changes card component used for both suggestion review and history viewing.
 *
 * @param fields Original fields from the note
 * @param changes AI-suggested or applied changes
 * @param editedFields User's manual edits (only used in Suggestion mode)
 * @param userEdits Fields that were edited by user (only used in History mode)
 * @param mode Either Suggestion (editable) or History (read-only)
 * @param onEditField Callback when field is edited (only used in Suggestion mode)
 */
@Composable
fun ChangesCard(
    fields: Map<String, NoteField>,
    changes: Map<String, String>,
    editedFields: Map<String, String> = emptyMap(),
    userEdits: Map<String, String>? = null,
    mode: ChangesCardMode,
    onEditField: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var showRevertDialog by remember { mutableStateOf(false) }

    // Determine header styling based on mode
    val (title, headerColor, headerBg, headerIcon) = when (mode) {
        is ChangesCardMode.Suggestion -> getSuggestionHeaderStyle(mode, colors)
        is ChangesCardMode.History -> getHistoryHeaderStyle(mode, colors)
    }

    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Title with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = headerColor,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = headerColor,
                    )
                }

                // Edit controls (only for Suggestion mode)
                if (mode is ChangesCardMode.Suggestion) {
                    SuggestionEditControls(
                        mode = mode,
                        headerColor = headerColor,
                        onShowRevertDialog = { showRevertDialog = true },
                    )
                }
            }

            HorizontalDivider(color = headerColor.copy(alpha = 0.3f), thickness = 2.dp)

            // Fields list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                val sortedFields = fields.entries.sortedBy { it.value.order }
                items(sortedFields) { (fieldName, field) ->
                    val aiValue = changes[fieldName] ?: field.value
                    val isChanged = aiValue != field.value

                    when (mode) {
                        is ChangesCardMode.Suggestion -> {
                            val editedValue = editedFields[fieldName]
                            val displayValue = if (mode.showOriginal) aiValue else (editedValue ?: aiValue)
                            val isEditable = mode.isEditMode && !mode.showOriginal
                            // Consider both AI changes and user edits for highlighting/diff
                            val displayChanged = displayValue != field.value

                            if (isEditable) {
                                EditableFieldItem(
                                    fieldName = fieldName,
                                    value = displayValue,
                                    isChanged = displayChanged,
                                    onEdit = { onEditField(fieldName, it) },
                                )
                            } else {
                                FieldItem(
                                    fieldName = fieldName,
                                    value = displayValue,
                                    isChanged = displayChanged,
                                    style = suggestedFieldStyle(displayChanged),
                                    diffContent = if (displayChanged && displayValue.isNotEmpty()) {
                                        { DiffHighlightedText(field.value, displayValue, DiffDisplayMode.Suggested) }
                                    } else null,
                                )
                            }
                        }
                        is ChangesCardMode.History -> {
                            val wasEdited = userEdits?.containsKey(fieldName) == true
                            HistoryFieldItem(
                                fieldName = fieldName,
                                originalValue = field.value,
                                changedValue = aiValue,
                                isChanged = isChanged,
                                wasEdited = wasEdited,
                                action = mode.action,
                            )
                        }
                    }
                }
            }
        }
    }

    // Revert confirmation dialog (only for Suggestion mode)
    if (showRevertDialog && mode is ChangesCardMode.Suggestion) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = { Text("Revert Manual Edits") },
            text = { Text("Are you sure you want to revert all manual edits? This will restore the original AI suggestion and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mode.onRevertEdits()
                        showRevertDialog = false
                    },
                    modifier = Modifier.handPointer(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Revert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDialog = false }, modifier = Modifier.handPointer()) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SuggestionEditControls(
    mode: ChangesCardMode.Suggestion,
    headerColor: Color,
    onShowRevertDialog: () -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Toggle AI/Edited - only show when editing OR has manual edits
        if (mode.isEditMode || mode.hasManualEdits) {
            FilterChip(
                selected = mode.showOriginal,
                onClick = mode.onToggleOriginal,
                label = {
                    Text(
                        text = if (mode.showOriginal) "Edited" else "AI",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                modifier = Modifier.height(28.dp).handPointer(),
            )
        }

        // Edit/Done button
        FilledTonalButton(
            onClick = mode.onToggleEditMode,
            modifier = Modifier.height(28.dp).handPointer(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (mode.isEditMode) colors.accentStrong else colors.surfaceAlt,
                contentColor = if (mode.isEditMode) colors.onAccent else colors.textPrimary,
            ),
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (mode.isEditMode) "Done" else "Edit", style = MaterialTheme.typography.labelMedium)
        }

        // Revert button - only show when has manual edits AND not in edit mode
        if (mode.hasManualEdits && !mode.isEditMode) {
            IconButton(
                onClick = onShowRevertDialog,
                modifier = Modifier.size(28.dp).handPointer(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Revert edits",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HistoryFieldItem(
    fieldName: String,
    originalValue: String,
    changedValue: String,
    isChanged: Boolean,
    wasEdited: Boolean,
    action: ReviewAction,
) {
    val colors = LocalAppColors.current

    val bgColor = when {
        action == ReviewAction.Reject && isChanged -> colors.errorMuted.copy(alpha = 0.3f)
        wasEdited -> colors.warningMuted.copy(alpha = 0.3f)
        isChanged -> colors.successMuted.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val labelColor = when {
        action == ReviewAction.Reject && isChanged -> colors.error
        wasEdited -> colors.warning
        isChanged -> colors.success
        else -> colors.textMuted
    }

    val badge = if (isChanged) {
        when {
            action == ReviewAction.Reject -> "Rejected"
            wasEdited -> "Edited"
            else -> "Changed"
        }
    } else null

    val icon = when {
        action == ReviewAction.Reject && isChanged -> Icons.Default.Close
        wasEdited -> Icons.Default.Edit
        isChanged -> Icons.Default.Check
        else -> Icons.Default.Remove
    }

    FieldItem(
        fieldName = fieldName,
        value = changedValue,
        isChanged = isChanged,
        style = FieldItemStyle(
            icon = icon,
            iconTint = labelColor,
            labelColor = labelColor,
            badge = badge,
            badgeColor = labelColor,
        ),
        modifier = Modifier.background(bgColor),
        diffContent = if (isChanged && changedValue.isNotEmpty()) {
            { DiffHighlightedText(originalValue, changedValue, DiffDisplayMode.Suggested) }
        } else null,
    )
}

private data class HeaderStyle(
    val title: String,
    val color: Color,
    val background: Color,
    val icon: ImageVector,
)

@Composable
private fun getSuggestionHeaderStyle(
    mode: ChangesCardMode.Suggestion,
    colors: com.ankideku.ui.theme.AppColorScheme,
): HeaderStyle {
    return when {
        mode.isEditMode && mode.showOriginal -> HeaderStyle(
            "AI Suggested",
            colors.secondary,
            colors.secondaryMuted,
            Icons.Default.SmartToy,
        )
        mode.isEditMode -> HeaderStyle(
            "Editing...",
            colors.warning,
            colors.warningMuted,
            Icons.Default.Edit,
        )
        mode.hasManualEdits -> HeaderStyle(
            "Manually Edited",
            colors.warning,
            colors.warningMuted,
            Icons.Default.Edit,
        )
        else -> HeaderStyle(
            "Suggested Card",
            colors.accent,
            colors.accentMuted,
            Icons.Default.AutoAwesome,
        )
    }
}

@Composable
private fun getHistoryHeaderStyle(
    mode: ChangesCardMode.History,
    colors: com.ankideku.ui.theme.AppColorScheme,
): HeaderStyle {
    return when {
        mode.action == ReviewAction.Reject -> HeaderStyle(
            "Rejected Changes",
            colors.error,
            colors.errorMuted,
            Icons.Default.Close,
        )
        mode.hasUserEdits -> HeaderStyle(
            "Applied with Edits",
            colors.warning,
            colors.warningMuted,
            Icons.Default.Edit,
        )
        else -> HeaderStyle(
            "Applied Changes",
            colors.success,
            colors.successMuted,
            Icons.Default.Check,
        )
    }
}
