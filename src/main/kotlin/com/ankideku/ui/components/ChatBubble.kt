package com.ankideku.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.ChatMessageType
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun ChatBubble(message: ChatMessage, colors: AppColorScheme) {
    val isUserMessage = message.type == ChatMessageType.UserPrompt

    val backgroundColor = when (message.type) {
        ChatMessageType.UserPrompt -> colors.userBubble
        ChatMessageType.SystemInfo -> colors.warningMuted
        ChatMessageType.SessionResult -> colors.assistantBubble
        ChatMessageType.Error -> colors.errorMuted
    }

    val textColor = when (message.type) {
        ChatMessageType.UserPrompt -> colors.onUserBubble
        ChatMessageType.SystemInfo -> colors.warningText
        ChatMessageType.SessionResult -> colors.textPrimary
        ChatMessageType.Error -> colors.error
    }

    val borderColor = when (message.type) {
        ChatMessageType.SystemInfo -> colors.warningBorder
        else -> null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * 0.8f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    ),
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp),
            ) {
                // Custom selection colors for user bubble (light selection on dark green)
                val selectionColors = if (isUserMessage) {
                    TextSelectionColors(
                        handleColor = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.4f),
                    )
                } else {
                    LocalTextSelectionColors.current
                }

                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}
