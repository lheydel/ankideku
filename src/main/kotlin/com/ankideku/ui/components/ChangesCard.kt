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
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant

/**
 * Generic changes card. Shows field changes with diff highlighting.
 * If onEditField is provided and isEditMode is true, fields are editable.
 */
@Composable
fun ChangesCard(
    fields: Map<String, NoteField>,
    changes: Map<String, String>,
    noteTypeConfig: NoteTypeConfig?,
    title: String,
    headerIcon: ImageVector,
    headerColor: Color,
    headerBg: Color,
    editedFields: Map<String, String> = emptyMap(),
    isEditMode: Boolean = false,
    showOriginal: Boolean = false,
    onEditField: ((String, String) -> Unit)? = null,
    headerControls: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
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

                headerControls?.invoke()
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
                    val editedValue = editedFields[fieldName]
                    val displayValue = if (showOriginal) aiValue else (editedValue ?: aiValue)
                    val isChanged = displayValue != field.value
                    val isEditable = isEditMode && !showOriginal && onEditField != null

                    if (isEditable) {
                        EditableFieldItem(
                            fieldName = fieldName,
                            value = displayValue,
                            isChanged = isChanged,
                            onEdit = { onEditField?.invoke(fieldName, it) },
                        )
                    } else {
                        FieldItem(
                            fieldName = fieldName,
                            value = displayValue,
                            isChanged = isChanged,
                            noteTypeConfig = noteTypeConfig,
                            style = suggestedFieldStyle(isChanged),
                            diffContent = if (isChanged && displayValue.isNotEmpty()) {
                                { DiffHighlightedText(field.value, displayValue, DiffDisplayMode.Suggested) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Edit controls for the header.
 */
@Composable
fun SuggestionEditControls(
    isEditMode: Boolean,
    hasManualEdits: Boolean,
    showOriginal: Boolean,
    onToggleEditMode: () -> Unit,
    onToggleOriginal: () -> Unit,
    onRevertEdits: () -> Unit,
) {
    val colors = LocalAppColors.current
    var showRevertDialog by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditMode || hasManualEdits) {
            FilterChip(
                selected = showOriginal,
                onClick = onToggleOriginal,
                label = {
                    Text(
                        text = if (showOriginal) "Edited" else "AI",
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

        AppButton(
            onClick = onToggleEditMode,
            modifier = Modifier.height(28.dp),
            variant = AppButtonVariant.Tonal,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isEditMode) colors.accentStrong else colors.surfaceAlt,
                contentColor = if (isEditMode) colors.onAccent else colors.textPrimary,
            ),
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isEditMode) "Done" else "Edit", style = MaterialTheme.typography.labelMedium)
        }

        if (hasManualEdits && !isEditMode) {
            AppIconButton(
                onClick = { showRevertDialog = true },
                modifier = Modifier.size(28.dp),
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

    if (showRevertDialog) {
        ConfirmDialog(
            title = "Revert Manual Edits",
            message = "Are you sure you want to revert all manual edits?",
            confirmLabel = "Revert",
            isDestructive = true,
            onConfirm = {
                onRevertEdits()
                showRevertDialog = false
            },
            onDismiss = { showRevertDialog = false },
        )
    }
}

