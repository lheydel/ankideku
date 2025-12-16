package com.ankideku.ui.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.ui.theme.handPointer

/**
 * Icon button component that automatically includes pointer cursor on hover.
 *
 * @param onClick Called when the button is clicked
 * @param modifier Modifier to apply to the button
 * @param enabled Whether the button is enabled
 * @param colors Custom icon button colors
 * @param content The icon content
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.handPointer(enabled),
        enabled = enabled,
        colors = colors,
        content = content,
    )
}
