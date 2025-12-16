package com.ankideku.ui.components.sidebar.review

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppIconButton
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun MemoryEntry(
    key: String,
    value: String,
    colors: AppColorScheme,
    onDelete: () -> Unit,
    onSave: (String) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedValue by remember(value) { mutableStateOf(value) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceAlt,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Header row with key and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (!isEditing) {
                        // Edit button
                        AppIconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit instruction",
                                modifier = Modifier.size(16.dp),
                                tint = colors.textMuted,
                            )
                        }
                    }
                    // Delete button
                    AppIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete instruction",
                            modifier = Modifier.size(16.dp),
                            tint = colors.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Content area - editable or read-only
            if (isEditing) {
                AppTextInput(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    singleLine = false,
                    minHeight = 80.dp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.sm))

                // Save/Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppButton(
                        onClick = {
                            editedValue = value
                            isEditing = false
                        },
                        variant = AppButtonVariant.Text,
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    AppButton(
                        onClick = {
                            if (editedValue != value && editedValue.isNotBlank()) {
                                onSave(editedValue)
                            }
                            isEditing = false
                        },
                        variant = AppButtonVariant.Primary,
                        enabled = editedValue.isNotBlank() && editedValue != value,
                    ) {
                        Text("Save")
                    }
                }
            } else {
                // Read-only display
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
