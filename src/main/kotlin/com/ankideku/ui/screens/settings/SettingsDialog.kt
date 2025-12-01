package com.ankideku.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.ui.components.AppDialog
import com.ankideku.ui.components.AppDropdown
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Settings
import com.ankideku.domain.model.Suggestion
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

@Composable
fun SettingsDialog(
    settings: Settings,
    suggestions: List<Suggestion>,
    llmHealthStatus: LlmHealthStatus?,
    llmHealthChecking: Boolean,
    onDismiss: () -> Unit,
    onSave: (Settings) -> Unit,
    onTestConnection: () -> Unit,
) {
    var currentSettings by remember { mutableStateOf(settings) }
    val scrollState = rememberScrollState()

    // Get unique model types from suggestions
    val modelTypes = remember(suggestions, settings.fieldDisplayConfig) {
        val models = mutableMapOf<String, Map<String, NoteField>?>()
        suggestions.forEach { suggestion ->
            val modelName = suggestion.modelName.ifBlank { "Unknown" }
            if (!models.containsKey(modelName)) {
                models[modelName] = suggestion.originalFields
            }
        }
        // Add from saved config
        settings.fieldDisplayConfig.keys.forEach { modelName ->
            if (!models.containsKey(modelName)) {
                models[modelName] = null
            }
        }
        models.toList()
    }

    AppDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 600.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Configure application settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.handPointer(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider()

                // Content - Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    // AI Provider Section
                    AiProviderSection(
                        selectedProvider = currentSettings.llmProvider,
                        onProviderSelected = { currentSettings = currentSettings.copy(llmProvider = it) },
                        healthStatus = llmHealthStatus,
                        isChecking = llmHealthChecking,
                        onTestConnection = onTestConnection,
                    )

                    // Theme Section
                    ThemeSection(
                        selectedTheme = currentSettings.theme,
                        onThemeSelected = { currentSettings = currentSettings.copy(theme = it) },
                    )

                    // Field Display Section (if model types available)
                    if (modelTypes.isNotEmpty()) {
                        FieldDisplaySection(
                            modelTypes = modelTypes,
                            fieldDisplayConfig = currentSettings.fieldDisplayConfig,
                            onFieldChange = { modelName, fieldName ->
                                currentSettings = currentSettings.copy(
                                    fieldDisplayConfig = currentSettings.fieldDisplayConfig + (modelName to fieldName)
                                )
                            },
                        )
                    }
                }

                HorizontalDivider()

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.handPointer(),
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(
                        onClick = {
                            onSave(currentSettings)
                            onDismiss()
                        },
                        modifier = Modifier.handPointer(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Save Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiProviderSection(
    selectedProvider: LlmProvider,
    onProviderSelected: (LlmProvider) -> Unit,
    healthStatus: LlmHealthStatus?,
    isChecking: Boolean,
    onTestConnection: () -> Unit,
) {
    val colors = LocalAppColors.current

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "AI Provider",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Provider dropdown and status
                    Column(modifier = Modifier.weight(1f)) {
                        AppDropdown(
                            items = LlmProvider.entries.toList(),
                            selectedItem = selectedProvider,
                            onItemSelected = onProviderSelected,
                            itemLabel = { providerDisplayName(it) },
                            enabled = !isChecking,
                            modifier = Modifier.width(180.dp),
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        // Status text
                        Text(
                            text = when {
                                isChecking -> "Checking connection..."
                                healthStatus?.available == true -> healthStatus.info ?: "Connected"
                                healthStatus != null -> healthStatus.error ?: "Connection failed"
                                else -> "Not tested - click Test Connection to verify"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> MaterialTheme.colorScheme.error
                                else -> colors.warning
                            },
                        )
                    }

                    // Status dot and test button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Status dot
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isChecking -> colors.warning
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> MaterialTheme.colorScheme.error
                                else -> colors.warning
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        // Test button
                        OutlinedButton(
                            onClick = onTestConnection,
                            modifier = Modifier.handPointer(!isChecking),
                            enabled = !isChecking,
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(Spacing.sm))
                            }
                            Text("Test Connection")
                        }
                    }
                }

                // Error details
                if (healthStatus != null && !healthStatus.available) {
                    Spacer(Modifier.height(Spacing.sm))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "Error: ${healthStatus.error ?: "Connection failed"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = "Make sure Claude Code CLI is installed and accessible in your PATH.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldDisplaySection(
    modelTypes: List<Pair<String, Map<String, NoteField>?>>,
    fieldDisplayConfig: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
) {
    Column {
        Text(
            text = "Field Display",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Configure which field to display in the sidebar for each note type",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            modelTypes.forEach { (modelName, fields) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (fields != null) {
                                Text(
                                    text = "${fields.size} field${if (fields.size != 1) "s" else ""} available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (fields != null) {
                            val sortedFields = fields.entries.sortedBy { it.value.order }.map { it.key }
                            val currentField = fieldDisplayConfig[modelName] ?: sortedFields.firstOrNull()

                            AppDropdown(
                                items = sortedFields,
                                selectedItem = currentField,
                                onItemSelected = { onFieldChange(modelName, it) },
                                itemLabel = { it },
                                modifier = Modifier.width(180.dp),
                            )
                        } else {
                            // No sample - show read-only
                            Text(
                                text = fieldDisplayConfig[modelName] ?: "Not configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Column {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppTheme.entries.forEach { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) },
                    modifier = Modifier.handPointer(),
                    label = { Text(themeDisplayName(theme)) },
                )
            }
        }
    }
}

private fun themeDisplayName(theme: AppTheme): String = when (theme) {
    AppTheme.Light -> "Light"
    AppTheme.Dark -> "Dark"
    AppTheme.System -> "System"
}

private fun providerDisplayName(provider: LlmProvider): String = when (provider) {
    LlmProvider.CLAUDE_CODE -> "Claude Code"
    LlmProvider.MOCK -> "Mock (Testing)"
}
