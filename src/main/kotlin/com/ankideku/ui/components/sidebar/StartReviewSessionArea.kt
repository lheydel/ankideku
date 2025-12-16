package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun StartReviewSessionArea(
    colors: AppColorScheme,
    isLoading: Boolean,
    onStartReviewSession: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceAlt,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Review suggestions with AI assistance",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(Modifier.height(Spacing.sm))
            AppButton(
                onClick = onStartReviewSession,
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                loadingText = "Starting...",
            ) {
                Text("Start Review Session")
            }
        }
    }
}
