package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.handPointer

/**
 * Button variant styles.
 */
enum class AppButtonVariant {
    /** Filled button for primary actions (Save, Accept, Search) */
    Primary,
    /** Outlined button for secondary actions (Cancel, Skip) */
    Outlined,
    /** Text button for tertiary/subtle actions (Reset, Cancel in dialogs) */
    Text,
    /** Filled tonal button for toggle states */
    Tonal,
}

/**
 * Generic button component that automatically includes pointer cursor on hover.
 *
 * @param onClick Called when the button is clicked
 * @param modifier Modifier to apply to the button
 * @param variant The visual style of the button
 * @param enabled Whether the button is enabled
 * @param colors Custom button colors (uses defaults based on variant if null)
 * @param contentPadding Padding around the button content
 * @param border Border stroke for outlined variant
 * @param content The button content (text, icon, etc.)
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val buttonModifier = modifier.handPointer(enabled)

    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors ?: ButtonDefaults.buttonColors(),
                contentPadding = contentPadding,
                content = content,
            )
        }
        AppButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors ?: ButtonDefaults.outlinedButtonColors(),
                contentPadding = contentPadding,
                border = border ?: ButtonDefaults.outlinedButtonBorder(enabled),
                content = content,
            )
        }
        AppButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors ?: ButtonDefaults.textButtonColors(),
                contentPadding = contentPadding,
                content = content,
            )
        }
        AppButtonVariant.Tonal -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors ?: ButtonDefaults.filledTonalButtonColors(),
                contentPadding = contentPadding,
                content = content,
            )
        }
    }
}

/**
 * Convenience composable for accent-colored primary button.
 */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    AppButton(
        onClick = onClick,
        modifier = modifier,
        variant = AppButtonVariant.Primary,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Convenience composable for destructive/error-styled button.
 */
@Composable
fun DestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val buttonColors = when (variant) {
        AppButtonVariant.Primary -> ButtonDefaults.buttonColors(containerColor = colors.error)
        AppButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
        AppButtonVariant.Text -> ButtonDefaults.textButtonColors(contentColor = colors.error)
        AppButtonVariant.Tonal -> ButtonDefaults.filledTonalButtonColors(containerColor = colors.error)
    }
    AppButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled,
        colors = buttonColors,
        contentPadding = contentPadding,
        content = content,
    )
}
