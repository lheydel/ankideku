package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

/**
 * Generic chat input area with provider indicator and customizable checkbox.
 */
@Composable
fun ChatInputArea(
    placeholder: String,
    enabled: Boolean,
    llmProvider: LlmProvider,
    colors: AppColorScheme,
    onSubmit: (String) -> Unit,
    checkbox: @Composable (RowScope.() -> Unit)? = null,
    bottomHint: String? = null,
    modifier: Modifier = Modifier,
) {
    val inputState = rememberTextFieldState()

    HorizontalDivider(color = colors.border, thickness = 1.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(Spacing.md),
    ) {
        // Checkbox row with provider indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checkbox != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    checkbox()
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Provider indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.textMuted,
                )
                Text(
                    text = when (llmProvider) {
                        LlmProvider.CLAUDE_CODE -> "Claude Code"
                        LlmProvider.MOCK -> "Mock (Testing)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ChatTextField(
            state = inputState,
            placeholder = placeholder,
            enabled = enabled,
            colors = colors,
            onSubmit = {
                val text = inputState.text.toString()
                if (text.isNotBlank()) {
                    onSubmit(text)
                    inputState.setTextAndPlaceCursorAtEnd("")
                }
            },
        )

        if (bottomHint != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = bottomHint,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
        }
    }
}
