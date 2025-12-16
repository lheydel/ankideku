package com.ankideku.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.ReviewContextConfig
import com.ankideku.ui.components.AppDialog
import com.ankideku.ui.components.AppIconButton
import com.ankideku.ui.components.LabeledCheckbox
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

private enum class ConfigTab(val label: String) {
    GENERAL("General"),
    CONTEXT("Context"),
}

/**
 * Dialog for configuring review session settings.
 * Matches the style of the global SettingsDialog.
 */
@Composable
fun ReviewSessionConfigDialog(
    currentConfig: ReviewContextConfig?,
    availableFields: List<String>,
    onDismiss: () -> Unit,
    onSave: (ReviewContextConfig) -> Unit,
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableStateOf(ConfigTab.GENERAL) }

    // Local state for editing
    var originalFields by remember(currentConfig) {
        mutableStateOf(currentConfig?.originalFields?.toSet())
    }
    var changesFields by remember(currentConfig) {
        mutableStateOf(currentConfig?.changesFields?.toSet())
    }
    var editedFields by remember(currentConfig) {
        mutableStateOf(currentConfig?.editedFields?.toSet())
    }
    var includeReasoning by remember(currentConfig) {
        mutableStateOf(currentConfig?.includeReasoning ?: true)
    }
    var messageHistoryLimit by remember(currentConfig) {
        mutableStateOf(currentConfig?.messageHistoryLimit ?: 10)
    }
    var customSystemPrompt by remember(currentConfig) {
        mutableStateOf(currentConfig?.customSystemPrompt ?: "")
    }

    fun saveConfig() {
        onSave(
            ReviewContextConfig(
                originalFields = originalFields?.toList(),
                changesFields = changesFields?.toList(),
                editedFields = editedFields?.toList(),
                includeReasoning = includeReasoning,
                messageHistoryLimit = messageHistoryLimit,
                customSystemPrompt = customSystemPrompt.ifBlank { null },
            )
        )
    }

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 500.dp, max = 700.dp)
            .heightIn(min = 400.dp, max = 600.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            ConfigHeader(onDismiss = onDismiss)

            HorizontalDivider(color = colors.divider)

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = colors.surface,
                contentColor = colors.textPrimary,
                divider = {},
            ) {
                ConfigTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        modifier = Modifier.handPointer(),
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        ConfigTab.GENERAL -> Icons.Default.Settings
                                        ConfigTab.CONTEXT -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(tab.label)
                            }
                        },
                    )
                }
            }

            HorizontalDivider(color = colors.divider)

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    ConfigTab.GENERAL -> GeneralTabContent(
                        messageHistoryLimit = messageHistoryLimit,
                        onMessageHistoryLimitChange = { messageHistoryLimit = it },
                        customSystemPrompt = customSystemPrompt,
                        onCustomSystemPromptChange = { customSystemPrompt = it },
                    )
                    ConfigTab.CONTEXT -> ContextTabContent(
                        availableFields = availableFields,
                        originalFields = originalFields,
                        onOriginalFieldsChange = { originalFields = it },
                        changesFields = changesFields,
                        onChangesFieldsChange = { changesFields = it },
                        editedFields = editedFields,
                        onEditedFieldsChange = { editedFields = it },
                        includeReasoning = includeReasoning,
                        onIncludeReasoningChange = { includeReasoning = it },
                    )
                }
            }

            HorizontalDivider(color = colors.divider)

            // Footer with save button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.handPointer(),
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(Spacing.sm))
                Button(
                    onClick = {
                        saveConfig()
                        onDismiss()
                    },
                    modifier = Modifier.handPointer(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentStrong,
                    ),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ConfigHeader(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "Review Session Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = "Configure how context is sent to the AI",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        AppIconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun GeneralTabContent(
    messageHistoryLimit: Int,
    onMessageHistoryLimitChange: (Int) -> Unit,
    customSystemPrompt: String,
    onCustomSystemPromptChange: (String) -> Unit,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Message history limit
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Message History Limit",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = "Number of recent messages to include when resuming a conversation",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Slider(
                    value = messageHistoryLimit.toFloat(),
                    onValueChange = { onMessageHistoryLimitChange(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 8,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentStrong,
                        activeTrackColor = colors.accent,
                    ),
                )
                Text(
                    text = "$messageHistoryLimit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.width(32.dp),
                )
            }
        }

        HorizontalDivider(color = colors.borderMuted)

        // Custom system prompt
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Custom Instructions",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = "Additional instructions to include in the system prompt",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )

            OutlinedTextField(
                value = customSystemPrompt,
                onValueChange = onCustomSystemPromptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = {
                    Text(
                        text = "e.g., Always use formal language, focus on grammar corrections...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderMuted,
                ),
            )
        }
    }
}

@Composable
private fun ContextTabContent(
    availableFields: List<String>,
    originalFields: Set<String>?,
    onOriginalFieldsChange: (Set<String>?) -> Unit,
    changesFields: Set<String>?,
    onChangesFieldsChange: (Set<String>?) -> Unit,
    editedFields: Set<String>?,
    onEditedFieldsChange: (Set<String>?) -> Unit,
    includeReasoning: Boolean,
    onIncludeReasoningChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "Select which data to include when sending suggestion context to the AI",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )

        // AI Reasoning
        LabeledCheckbox(
            checked = includeReasoning,
            onCheckedChange = onIncludeReasoningChange,
            label = "Include AI reasoning",
        )

        HorizontalDivider(color = colors.borderMuted)

        // Three columns for field contexts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Original Fields
            FieldContextSection(
                title = "Original",
                description = "Original values",
                availableFields = availableFields,
                selectedFields = originalFields,
                onSelectedFieldsChange = onOriginalFieldsChange,
                modifier = Modifier.weight(1f),
            )

            // AI Changes
            FieldContextSection(
                title = "AI Changes",
                description = "AI suggestions",
                availableFields = availableFields,
                selectedFields = changesFields,
                onSelectedFieldsChange = onChangesFieldsChange,
                modifier = Modifier.weight(1f),
            )

            // Edited Changes
            FieldContextSection(
                title = "Edited",
                description = "Your edits",
                availableFields = availableFields,
                selectedFields = editedFields,
                onSelectedFieldsChange = onEditedFieldsChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FieldContextSection(
    title: String,
    description: String,
    availableFields: List<String>,
    selectedFields: Set<String>?,
    onSelectedFieldsChange: (Set<String>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val allSelected = selectedFields == null
    val noneSelected = selectedFields?.isEmpty() == true

    Surface(
        color = colors.surfaceAlt,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )

            HorizontalDivider(color = colors.borderMuted, modifier = Modifier.padding(vertical = Spacing.xs))

            if (availableFields.isEmpty()) {
                Text(
                    text = "Select a suggestion first",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            } else {
                // All fields checkbox
                LabeledCheckbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        onSelectedFieldsChange(if (checked) null else availableFields.toSet())
                    },
                    label = "All",
                    labelStyle = MaterialTheme.typography.bodySmall,
                )

                // Individual field checkboxes
                availableFields.forEach { field ->
                    LabeledCheckbox(
                        checked = !noneSelected && (allSelected || field in (selectedFields ?: emptySet())),
                        onCheckedChange = { checked ->
                            if (allSelected) {
                                // Switching from all to specific selection
                                val newSet = if (checked) availableFields.toSet() else availableFields.toSet() - field
                                onSelectedFieldsChange(newSet)
                            } else {
                                val current = selectedFields ?: emptySet()
                                val newSet = if (checked) current + field else current - field
                                onSelectedFieldsChange(newSet.ifEmpty { emptySet() })
                            }
                        },
                        label = field,
                        labelStyle = MaterialTheme.typography.bodySmall,
                        enabled = !allSelected && !noneSelected,
                    )
                }

                // None checkbox
                LabeledCheckbox(
                    checked = noneSelected,
                    onCheckedChange = { checked ->
                        onSelectedFieldsChange(if (checked) emptySet() else null)
                    },
                    label = "None",
                    labelStyle = MaterialTheme.typography.bodySmall,
                    labelColor = colors.textMuted,
                )
            }
        }
    }
}
