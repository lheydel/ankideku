package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * Configuration for field item styling
 */
data class FieldItemStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val labelColor: Color,
    val badge: String? = null,
    val badgeColor: Color? = null,
)

/**
 * Unified field item component used by Original, Suggested, and History views.
 */
@Composable
fun FieldItem(
    fieldName: String,
    value: String,
    isChanged: Boolean,
    noteTypeConfig: NoteTypeConfig?,
    style: FieldItemStyle,
    modifier: Modifier = Modifier,
    diffContent: @Composable (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Content area with background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isChanged) it.background(colors.fieldChangedBg) else it }
                .padding(vertical = 12.dp),
        ) {
            // Field name header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = style.iconTint,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = fieldName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = style.labelColor,
                )
                if (style.badge != null && style.badgeColor != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Surface(
                        color = style.badgeColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = style.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = style.badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            // Field value
            if (value.isEmpty()) {
                Text(
                    text = "(empty)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            } else if (diffContent != null) {
                diffContent()
            } else {
                SelectionContainer {
                    ConfiguredText(
                        text = value,
                        fieldName = fieldName,
                        noteTypeConfig = noteTypeConfig,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        // Divider outside the background
        HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
    }
}

/**
 * Editable field item for the suggested card in edit mode.
 */
@Composable
fun EditableFieldItem(
    fieldName: String,
    value: String,
    isChanged: Boolean,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val fieldState = rememberTextFieldState(value)

    LaunchedEffect(value) {
        if (fieldState.text.toString() != value) {
            fieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }
            .drop(1)
            .collectLatest { onEdit(it) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Content area with background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isChanged) it.background(colors.fieldChangedBg) else it }
                .padding(vertical = 12.dp),
        ) {
            // Field name header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isChanged) Icons.Default.Check else Icons.Default.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isChanged) colors.success else colors.textMuted,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = fieldName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isChanged) colors.success else colors.textMuted,
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            OutlinedTextField(
                state = fieldState,
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (isChanged) colors.diffAdded.copy(alpha = 0.5f) else colors.surfaceAlt,
                    focusedContainerColor = if (isChanged) colors.diffAdded.copy(alpha = 0.5f) else colors.surfaceAlt,
                    unfocusedBorderColor = colors.border,
                    focusedBorderColor = colors.accentStrong,
                ),
                shape = InputShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        // Divider outside the background
        HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
    }
}

/**
 * Helper to create style for original card fields
 */
@Composable
fun originalFieldStyle(isChanged: Boolean): FieldItemStyle {
    val colors = LocalAppColors.current
    return FieldItemStyle(
        icon = if (isChanged) Icons.Default.Warning else Icons.Default.Check,
        iconTint = if (isChanged) colors.warning else colors.textMuted,
        labelColor = if (isChanged) colors.warning else colors.textMuted,
    )
}

/**
 * Helper to create style for suggested card fields
 */
@Composable
fun suggestedFieldStyle(isChanged: Boolean): FieldItemStyle {
    val colors = LocalAppColors.current
    return FieldItemStyle(
        icon = if (isChanged) Icons.Default.Check else Icons.Default.Remove,
        iconTint = if (isChanged) colors.success else colors.textMuted,
        labelColor = if (isChanged) colors.success else colors.textMuted,
    )
}

/**
 * Helper to create style for note preview fields (neutral, no change indication)
 */
@Composable
fun previewFieldStyle(): FieldItemStyle {
    val colors = LocalAppColors.current
    return FieldItemStyle(
        icon = Icons.Default.TextFields,
        iconTint = colors.textMuted,
        labelColor = colors.textSecondary,
    )
}
