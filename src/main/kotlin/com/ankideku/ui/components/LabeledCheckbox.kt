package com.ankideku.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

/**
 * A checkbox with a clickable label.
 * Clicking anywhere on the row (checkbox or label) toggles the checkbox.
 */
@Composable
fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    labelColor: Color = LocalAppColors.current.textPrimary,
    checkedColor: Color = LocalAppColors.current.accentStrong,
) {
    val colors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .handPointer(enabled)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null, // Handled by Row click
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = checkedColor,
                disabledCheckedColor = colors.textMuted,
                disabledUncheckedColor = colors.textMuted,
            ),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = label,
            style = labelStyle,
            color = if (enabled) labelColor else colors.textMuted,
        )
    }
}
