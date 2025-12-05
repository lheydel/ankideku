package com.ankideku.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ankideku.ui.theme.LocalAppColors

/**
 * App-wide dialog wrapper with consistent styling.
 *
 * Provides:
 * - Themed Surface with rounded corners and shadow
 * - Re-provides LocalMinimumInteractiveComponentSize since Dialog creates a separate composition
 * - Customizable modifier for size constraints
 *
 * @param onDismissRequest Called when the dialog should be dismissed
 * @param modifier Modifier for the dialog Surface (use for size constraints like widthIn, heightIn)
 * @param properties Dialog window properties
 * @param content Dialog content
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                shadowElevation = 8.dp,
            ) {
                content()
            }
        }
    }
}
