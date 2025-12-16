package com.ankideku.ui.components.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.ChangesCard
import com.ankideku.ui.components.IconLabel
import com.ankideku.ui.components.ReasoningCard
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.components.AppIconButton

@Composable
fun ColumnScope.ComparisonContent(
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
fun ComparisonHeaderCard(
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
                        AppIconButton(onClick = onOpenNoteTypeSettings, modifier = Modifier.size(24.dp)) {
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
