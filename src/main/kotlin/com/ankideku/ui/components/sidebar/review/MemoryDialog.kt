package com.ankideku.ui.components.sidebar.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppDialog
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.DialogContent
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

/**
 * Dialog showing stored AI instructions (memory).
 */
@Composable
fun MemoryDialog(
    memory: Map<String, String>,
    colors: AppColorScheme,
    onDeleteMemory: (String) -> Unit,
    onSaveMemory: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var isAddingNew by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 500.dp, max = 650.dp),
    ) {
        DialogContent(
            title = "Stored Instructions",
            message = "The AI remembers these instructions across conversation resets.",
            content = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Add new instruction section
                    if (isAddingNew) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.surfaceAlt,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                Text(
                                    text = "New Instruction",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                AppTextInput(
                                    value = newKey,
                                    onValueChange = { newKey = it },
                                    label = "Name",
                                    placeholder = "e.g., formatting_rules",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                AppTextInput(
                                    value = newValue,
                                    onValueChange = { newValue = it },
                                    label = "Instruction",
                                    placeholder = "Describe the rule or preference...",
                                    singleLine = false,
                                    minHeight = 80.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    AppButton(
                                        onClick = {
                                            newKey = ""
                                            newValue = ""
                                            isAddingNew = false
                                        },
                                        variant = AppButtonVariant.Text,
                                    ) {
                                        Text("Cancel")
                                    }
                                    Spacer(Modifier.width(Spacing.sm))
                                    AppButton(
                                        onClick = {
                                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                                onSaveMemory(newKey.trim(), newValue.trim())
                                                newKey = ""
                                                newValue = ""
                                                isAddingNew = false
                                            }
                                        },
                                        variant = AppButtonVariant.Primary,
                                        enabled = newKey.isNotBlank() && newValue.isNotBlank(),
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    } else {
                        // Add button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .handPointer()
                                .clickable { isAddingNew = true },
                            color = colors.surface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                width = 1.dp,
                                color = colors.borderMuted,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.accent,
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    text = "Add instruction",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent,
                                )
                            }
                        }
                    }

                    // Existing entries
                    if (memory.isEmpty() && !isAddingNew) {
                        Text(
                            text = "No instructions stored yet. Add one above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            modifier = Modifier.padding(vertical = Spacing.md),
                        )
                    } else {
                        memory.entries.forEach { (key, value) ->
                            MemoryEntry(
                                key = key,
                                value = value,
                                colors = colors,
                                onDelete = { onDeleteMemory(key) },
                                onSave = { newValue -> onSaveMemory(key, newValue) },
                            )
                        }
                    }
                }
            },
            buttons = {
                AppButton(
                    onClick = onDismiss,
                    variant = AppButtonVariant.Primary,
                ) {
                    Text("Close")
                }
            },
        )
    }
}
