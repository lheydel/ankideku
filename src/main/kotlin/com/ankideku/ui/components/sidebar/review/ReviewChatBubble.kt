package com.ankideku.ui.components.sidebar.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.sidebar.BaseChatBubble
import com.ankideku.ui.screens.main.ReviewChatMessage
import com.ankideku.ui.screens.main.ReviewChatRole
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun ReviewChatBubble(
    message: ReviewChatMessage,
    colors: AppColorScheme,
    modifier: Modifier = Modifier,
) {
    val isUserMessage = message.role == ReviewChatRole.User
    val isActionResult = message.role == ReviewChatRole.ActionResult

    val backgroundColor = when (message.role) {
        ReviewChatRole.User -> colors.userBubble
        ReviewChatRole.Assistant -> colors.assistantBubble
        ReviewChatRole.ActionResult -> colors.surfaceAlt
    }

    val textColor = when (message.role) {
        ReviewChatRole.User -> colors.onUserBubble
        ReviewChatRole.Assistant -> colors.textPrimary
        ReviewChatRole.ActionResult -> colors.textMuted
    }

    BaseChatBubble(
        content = message.content,
        backgroundColor = backgroundColor,
        textColor = textColor,
        isUserMessage = isUserMessage,
        maxWidthFraction = 0.85f,
        asymmetricCorners = true,
        header = if (!isUserMessage) {
            {
                Text(
                    text = if (isActionResult) "Action Result" else "Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(4.dp))
            }
        } else null,
        footer = if (!message.actionCalls.isNullOrEmpty()) {
            {
                Spacer(Modifier.height(Spacing.sm))
                message.actionCalls.forEach { actionCall ->
                    ActionCallChip(
                        action = actionCall.action,
                        colors = colors,
                    )
                }
            }
        } else null,
        modifier = modifier,
    )
}

@Composable
private fun ActionCallChip(
    action: String,
    colors: AppColorScheme,
) {
    Surface(
        color = colors.accentMuted,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = action,
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}
