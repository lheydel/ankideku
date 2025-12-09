package com.ankideku.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.Deck
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing
import com.ankideku.util.isEnterKey

@Composable
fun ChatInputArea(
    isSyncing: Boolean,
    canStartSession: Boolean,
    selectedDeck: Deck?,
    forceSyncBeforeStart: Boolean,
    llmProvider: LlmProvider,
    colors: AppColorScheme,
    onForceSyncChanged: (Boolean) -> Unit,
    onStartSession: (String) -> Unit,
) {
    HorizontalDivider(color = colors.border, thickness = 1.dp)

    Column(
        modifier = Modifier
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Checkbox(
                    checked = forceSyncBeforeStart,
                    onCheckedChange = onForceSyncChanged,
                    enabled = !isSyncing,
                    modifier = Modifier.size(20.dp).pointerHoverIcon(PointerIcon.Hand),
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accentStrong,
                    ),
                )
                Text(
                    text = "Sync deck before processing",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSyncing) colors.textMuted else colors.textSecondary,
                )
            }
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

        val promptState = rememberTextFieldState()

        fun submitPrompt() {
            val prompt = promptState.text.toString()
            if (prompt.isNotBlank() && canStartSession) {
                onStartSession(prompt)
                promptState.setTextAndPlaceCursorAtEnd("")
            }
        }

        OutlinedTextField(
            state = promptState,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isEnterKey()) {
                        if (!event.isShiftPressed) {
                            submitPrompt()
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
                    text = if (isSyncing) "Syncing deck..." else "Describe what you want to improve... (Shift+Enter for new line)",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            enabled = !isSyncing,
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

        if (selectedDeck == null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Select a deck to start",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
        }
    }
}
