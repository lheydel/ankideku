package com.ankideku.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

/**
 * Checks if the key event is an Enter key press (regular or numpad).
 */
fun KeyEvent.isEnterKey(): Boolean =
    key == Key.Enter || key == Key.NumPadEnter
