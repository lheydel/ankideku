package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.InputPadding
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors

/**
 * Text input component with consistent styling matching AppDropdown.
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param placeholder Text to show when empty
 * @param enabled Whether the input is interactive
 * @param singleLine Whether to limit input to a single line
 */
@Composable
fun AppTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = InputShape,
        color = colors.surfaceAlt,
        border = BorderStroke(1.dp, if (focused) colors.accent else colors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InputPadding)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
    }
}
