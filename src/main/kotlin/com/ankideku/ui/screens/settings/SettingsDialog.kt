package com.ankideku.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.FontOption
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.Settings
import com.ankideku.ui.components.AppDialog
import com.ankideku.ui.components.AppDropdown
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
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 600.dp, max = 900.dp)
                .heightIn(min = 500.dp, max = 750.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            shadowElevation = 8.dp,
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

// ==================== General Tab ====================

@Composable
private fun GeneralTabContent(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    llmHealthStatus: LlmHealthStatus?,
    llmHealthChecking: Boolean,
    onTestConnection: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        AiProviderSection(
            selectedProvider = settings.llmProvider,
            onProviderSelected = { onSettingsChange(settings.copy(llmProvider = it)) },
            healthStatus = llmHealthStatus,
            isChecking = llmHealthChecking,
            onTestConnection = onTestConnection,
        )

        ThemeSection(
            selectedTheme = settings.theme,
            onThemeSelected = { onSettingsChange(settings.copy(theme = it)) },
        )
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
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "AI Provider",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceAlt,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

                        Text(
                            text = when {
                                isChecking -> "Checking connection..."
                                healthStatus?.available == true -> healthStatus.info ?: "Connected"
                                healthStatus != null -> healthStatus.error ?: "Connection failed"
                                else -> "Not tested - click Test Connection to verify"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isChecking -> colors.textSecondary
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> colors.error
                                else -> colors.warning
                            },
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isChecking -> colors.warning
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> colors.error
                                else -> colors.warning
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

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

                if (healthStatus != null && !healthStatus.available) {
                    Spacer(Modifier.height(Spacing.sm))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.errorMuted,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "Error: ${healthStatus.error ?: "Connection failed"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                            )
                            Text(
                                text = "Make sure Claude Code CLI is installed and accessible in your PATH.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
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
    val colors = LocalAppColors.current

    Column {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
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

// ==================== Note Types Tab ====================

@Composable
private fun NoteTypesTabContent(
    availableNoteTypes: List<String>,
    noteTypeConfigs: Map<String, NoteTypeConfig>,
    noteTypeFields: Map<String, List<String>>,
    selectedNoteType: String?,
    onNoteTypeSelected: (String) -> Unit,
    onSaveConfig: (NoteTypeConfig) -> Unit,
) {
    val colors = LocalAppColors.current

    if (availableNoteTypes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No note types available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
                Text(
                    text = "Sync a deck first to see available note types",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Left: Note type list
        LazyColumn(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(colors.surfaceAlt),
        ) {
            items(availableNoteTypes) { noteType ->
                val isSelected = noteType == selectedNoteType
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNoteTypeSelected(noteType) }
                        .then(Modifier.handPointer()),
                    color = if (isSelected) colors.accentMuted else colors.surfaceAlt,
                ) {
                    Text(
                        text = noteType,
                        modifier = Modifier.padding(Spacing.md),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) colors.accent else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        VerticalDivider(color = colors.divider)

        // Right: Config form for selected note type
        if (selectedNoteType != null) {
            NoteTypeConfigForm(
                noteType = selectedNoteType,
                config = noteTypeConfigs[selectedNoteType],
                fields = noteTypeFields[selectedNoteType] ?: emptyList(),
                onSaveConfig = onSaveConfig,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(
                modifier = Modifier.weight(1f).padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Select a note type to configure",
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun NoteTypeConfigForm(
    noteType: String,
    config: NoteTypeConfig?,
    fields: List<String>,
    onSaveConfig: (NoteTypeConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    // Local state for editing
    var defaultField by remember(noteType, config) {
        mutableStateOf(config?.defaultDisplayField ?: fields.firstOrNull())
    }
    var fieldFonts by remember(noteType, config) {
        mutableStateOf(config?.fieldFontConfig ?: emptyMap())
    }

    // Auto-save when config changes
    LaunchedEffect(defaultField, fieldFonts) {
        onSaveConfig(
            NoteTypeConfig(
                modelName = noteType,
                defaultDisplayField = defaultField,
                fieldFontConfig = fieldFonts,
            )
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Header
        Text(
            text = noteType,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )

        // Default display field
        if (fields.isNotEmpty()) {
            Column {
                Text(
                    text = "Default Display Field",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Field shown in the sidebar queue",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sm))
                AppDropdown(
                    items = fields,
                    selectedItem = defaultField,
                    onItemSelected = { defaultField = it },
                    itemLabel = { it },
                    modifier = Modifier.width(200.dp),
                )
            }
        }

        // Per-field font config
        if (fields.isNotEmpty()) {
            Column {
                Text(
                    text = "Field Fonts",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Choose which font to use for each field",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sm))

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    fields.forEach { field ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = field,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            AppDropdown(
                                items = FontOption.entries.toList(),
                                selectedItem = fieldFonts[field] ?: FontOption.DEFAULT,
                                onItemSelected = { font ->
                                    fieldFonts = fieldFonts + (field to font)
                                },
                                itemLabel = { fontDisplayName(it) },
                                modifier = Modifier.width(160.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Helpers ====================

private fun themeDisplayName(theme: AppTheme): String = when (theme) {
    AppTheme.Light -> "Light"
    AppTheme.Dark -> "Dark"
    AppTheme.System -> "System"
}

private fun providerDisplayName(provider: LlmProvider): String = when (provider) {
    LlmProvider.CLAUDE_CODE -> "Claude Code"
    LlmProvider.MOCK -> "Mock (Testing)"
}

private fun fontDisplayName(font: FontOption): String = when (font) {
    FontOption.DEFAULT -> "Default"
    FontOption.NOTO_SANS_JP -> "Noto Sans JP"
}
