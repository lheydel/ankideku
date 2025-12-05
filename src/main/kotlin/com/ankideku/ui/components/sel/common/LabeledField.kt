package com.ankideku.ui.components.sel.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * A labeled field with a text label and content side by side.
 *
 * @param label The label text
 * @param labelColor Color for the label (defaults to textMuted)
 * @param content The content composable
 */
@Composable
fun LabeledField(
    label: String,
    labelColor: Color? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val effectiveLabelColor = labelColor ?: colors.textMuted

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = effectiveLabelColor,
        )
        content()
    }
}

/**
 * A labeled text input field.
 *
 * @param label The label text
 * @param value Current value
 * @param onValueChange Called when value changes
 * @param placeholder Placeholder text
 * @param inputWidth Width of the input field
 * @param labelColor Color for the label
 */
@Composable
fun LabeledTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    inputWidth: Dp = 80.dp,
    labelColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = label, labelColor = labelColor, modifier = modifier) {
        AppTextInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.width(inputWidth),
        )
    }
}
