package com.ankideku.ui.components.sidebar

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.Spacing

/**
 * Base chat bubble component with shared layout and styling logic.
 *
 * @param content The text content to display
 * @param backgroundColor Background color of the bubble
 * @param textColor Color of the text content
 * @param isUserMessage Whether this is a user message (affects alignment and selection colors)
 * @param borderColor Optional border color for the bubble
 * @param maxWidthFraction Maximum width as fraction of parent (0.0-1.0)
 * @param cornerRadius Corner radius for all corners (when asymmetricCorners is false)
 * @param asymmetricCorners Whether to use asymmetric corners based on message sender
 * @param header Optional composable to show above the content (e.g., role label)
 * @param footer Optional composable to show below the content (e.g., action chips)
 */
@Composable
fun BaseChatBubble(
    content: String,
    backgroundColor: Color,
    textColor: Color,
    isUserMessage: Boolean,
    borderColor: Color? = null,
    maxWidthFraction: Float = 0.8f,
    cornerRadius: Dp = 8.dp,
    asymmetricCorners: Boolean = false,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = if (asymmetricCorners) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = if (isUserMessage) 12.dp else 4.dp,
            bottomEnd = if (isUserMessage) 4.dp else 12.dp,
        )
    } else {
        RoundedCornerShape(cornerRadius)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * maxWidthFraction

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(1.dp, borderColor, shape)
                        } else {
                            Modifier
                        }
                    ),
                color = backgroundColor,
                shape = shape,
            ) {
                // Custom selection colors for user bubble (light selection on dark background)
                val selectionColors = if (isUserMessage) {
                    TextSelectionColors(
                        handleColor = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.4f),
                    )
                } else {
                    LocalTextSelectionColors.current
                }

                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        header?.invoke()

                        SelectionContainer {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }

                        footer?.invoke()
                    }
                }
            }
        }
    }
}
