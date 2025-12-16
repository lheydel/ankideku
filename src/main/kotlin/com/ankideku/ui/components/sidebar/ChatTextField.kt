package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.util.isEnterKey

/**
 * Reusable chat text field with consistent styling.
 * Submits on Enter, Shift+Enter for new line.
 *
 * @param state TextFieldState for the input
 * @param placeholder Placeholder text when empty
 * @param enabled Whether the input is interactive
 * @param colors App color scheme
 * @param onSubmit Callback when Enter is pressed (without Shift)
 */
@Composable
fun ChatTextField(
    state: TextFieldState,
    placeholder: String,
    enabled: Boolean,
    colors: AppColorScheme,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isEnterKey()) {
                    if (!event.isShiftPressed) {
                        onSubmit()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        enabled = enabled,
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 3),
        textStyle = MaterialTheme.typography.bodySmall,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colors.surfaceAlt,
            focusedContainerColor = colors.surfaceAlt,
            unfocusedBorderColor = colors.border,
            focusedBorderColor = colors.accent,
            unfocusedPlaceholderColor = colors.textMuted,
            focusedPlaceholderColor = colors.textMuted,
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    )
}
