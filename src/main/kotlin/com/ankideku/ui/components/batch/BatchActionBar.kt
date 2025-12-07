package com.ankideku.ui.components.batch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ankideku.ui.components.AccentButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.DestructiveButton
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Floating action bar shown at the bottom of QueuePanel when batch filter mode is active.
 * Contains Reject All and Accept All buttons side by side.
 */
@Composable
fun BatchActionBar(
    matchCount: Int,
    isProcessing: Boolean,
    onRejectAll: () -> Unit,
    onAcceptAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.sm))
            .background(colors.surfaceAlt)
            .padding(Spacing.sm),
    ) {
        // Reject All button - left half
        DestructiveButton(
            onClick = onRejectAll,
            variant = AppButtonVariant.Outlined,
            enabled = !isProcessing && matchCount > 0,
            modifier = Modifier.weight(1f),
        ) {
            Text("Reject All")
        }

        // Accept All button - right half
        AccentButton(
            onClick = onAcceptAll,
            enabled = !isProcessing && matchCount > 0,
            modifier = Modifier.weight(1f).padding(start = Spacing.sm),
        ) {
            Text("Accept All")
        }
    }
}
