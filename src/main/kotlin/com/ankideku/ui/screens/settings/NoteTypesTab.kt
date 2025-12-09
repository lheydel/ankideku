package com.ankideku.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.FontOption
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

@Composable
fun NoteTypesTabContent(
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

// Helper function
internal fun fontDisplayName(font: FontOption): String = when (font) {
    FontOption.DEFAULT -> "Default"
    FontOption.NOTO_SANS_JP -> "Noto Sans JP"
}
