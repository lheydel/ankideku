package com.ankideku.ui.components.batch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ankideku.ui.components.AccentButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.DestructiveButton
import com.ankideku.ui.screens.main.BatchProgress
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Floating action bar shown at the bottom of QueuePanel when batch filter mode is active.
 * Contains Reject All and Accept All buttons side by side, with a progress bar when processing.
 */
@Composable
fun BatchActionBar(
    matchCount: Int,
    isProcessing: Boolean,
    onRejectAll: () -> Unit,
    onAcceptAll: () -> Unit,
    modifier: Modifier = Modifier,
    batchProgress: BatchProgress? = null,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.sm))
            .background(colors.surfaceAlt)
            .padding(Spacing.sm),
    ) {
        // Progress bar when processing
        if (isProcessing && batchProgress != null && batchProgress.total > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { batchProgress.current.toFloat() / batchProgress.total },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${batchProgress.current}/${batchProgress.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
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
}
