package com.ankideku.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Combines clickable with pointer hand cursor for desktop.
 */
@Composable
fun Modifier.clickableWithPointer(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this
    .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
    .clickable(enabled = enabled, onClick = onClick)

/**
 * Combines clickable with pointer hand cursor, no ripple effect.
 */
@Composable
fun Modifier.clickableWithPointerNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this
    .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
    .clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
    )

/**
 * Adds hand pointer cursor for interactive elements.
 */
fun Modifier.handPointer(enabled: Boolean = true): Modifier =
    pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
