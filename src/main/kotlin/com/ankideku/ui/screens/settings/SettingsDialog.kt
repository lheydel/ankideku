package com.ankideku.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.Settings
import com.ankideku.ui.components.AppDialog
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

enum class SettingsTab(val label: String) {
    GENERAL("General"),
    NOTE_TYPES("Note Types"),
}

@Composable
fun SettingsDialog(
    settings: Settings,
    llmHealthStatus: LlmHealthStatus?,
    llmHealthChecking: Boolean,
    // Note Type Config
    availableNoteTypes: List<String> = emptyList(),
    noteTypeConfigs: Map<String, NoteTypeConfig> = emptyMap(),
    noteTypeFields: Map<String, List<String>> = emptyMap(),
    initialTab: SettingsTab? = null,
    initialNoteType: String? = null,
    onDismiss: () -> Unit,
    onSave: (Settings) -> Unit,
    onSaveNoteTypeConfig: (NoteTypeConfig) -> Unit = {},
    onTestConnection: () -> Unit,
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableStateOf(initialTab ?: SettingsTab.GENERAL) }
    var selectedNoteType by remember { mutableStateOf(initialNoteType ?: availableNoteTypes.firstOrNull()) }

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 600.dp, max = 900.dp)
            .heightIn(min = 500.dp, max = 750.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            SettingsHeader(onDismiss = onDismiss)

            HorizontalDivider(color = colors.divider)

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = colors.surface,
                contentColor = colors.textPrimary,
                divider = {},
            ) {
                SettingsTab.entries.forEach { tab ->
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
                                        SettingsTab.GENERAL -> Icons.Default.Settings
                                        SettingsTab.NOTE_TYPES -> Icons.Default.Description
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
                    SettingsTab.GENERAL -> GeneralTabContent(
                        settings = settings,
                        onSettingsChange = onSave, // Auto-save on change
                        llmHealthStatus = llmHealthStatus,
                        llmHealthChecking = llmHealthChecking,
                        onTestConnection = onTestConnection,
                    )
                    SettingsTab.NOTE_TYPES -> NoteTypesTabContent(
                        availableNoteTypes = availableNoteTypes,
                        noteTypeConfigs = noteTypeConfigs,
                        noteTypeFields = noteTypeFields,
                        selectedNoteType = selectedNoteType,
                        onNoteTypeSelected = { selectedNoteType = it },
                        onSaveConfig = onSaveNoteTypeConfig,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onDismiss: () -> Unit) {
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
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = "Configure application settings",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.handPointer(),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary,
            )
        }
    }
}
