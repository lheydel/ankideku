package com.ankideku.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        ChatMessageType.UserPrompt -> colors.accentStrong
        ChatMessageType.SystemInfo -> colors.warningMuted
        ChatMessageType.SessionResult -> colors.surfaceAlt
        ChatMessageType.Error -> colors.errorMuted
    }

    val textColor = when (message.type) {
        ChatMessageType.UserPrompt -> Color.White
        ChatMessageType.SystemInfo -> colors.warningText
        ChatMessageType.SessionResult -> colors.textPrimary
        ChatMessageType.Error -> colors.error
    }

    val borderColor = when (message.type) {
        ChatMessageType.SystemInfo -> colors.warningBorder
        else -> null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 240.dp)
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
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}
