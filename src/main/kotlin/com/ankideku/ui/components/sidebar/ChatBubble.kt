package com.ankideku.ui.components.sidebar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.ChatMessageType
import com.ankideku.ui.theme.AppColorScheme

@Composable
fun ChatBubble(
    message: ChatMessage,
    colors: AppColorScheme,
    modifier: Modifier = Modifier,
) {
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

    BaseChatBubble(
        content = message.content,
        backgroundColor = backgroundColor,
        textColor = textColor,
        isUserMessage = isUserMessage,
        borderColor = borderColor,
        modifier = modifier,
    )
}
