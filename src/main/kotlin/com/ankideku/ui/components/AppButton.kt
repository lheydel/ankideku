package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

/**
 * Tooltip configuration for buttons.
 */
data class ButtonTooltip(
    val title: String,
    val description: String,
    val highlight: String? = null,
)

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
 * @param isLoading Shows a loading spinner and disables the button
 * @param loadingText Optional text to show while loading (defaults to content)
 * @param colors Custom button colors (uses defaults based on variant if null)
 * @param contentPadding Padding around the button content
 * @param border Border stroke for outlined variant
 * @param tooltip Optional tooltip that shows on hover (persistent until clicked away)
 * @param content The button content (text, icon, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    border: BorderStroke? = null,
    tooltip: ButtonTooltip? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val isEnabled = enabled && !isLoading

    val buttonContent: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            if (loadingText != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(loadingText)
            }
        } else {
            content()
        }
    }

    val button: @Composable () -> Unit = {
        val buttonModifier = modifier.handPointer(isEnabled)

        when (variant) {
            AppButtonVariant.Primary -> {
                Button(
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = isEnabled,
                    colors = colors ?: ButtonDefaults.buttonColors(),
                    contentPadding = contentPadding,
                    content = buttonContent,
                )
            }
            AppButtonVariant.Outlined -> {
                OutlinedButton(
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = isEnabled,
                    colors = colors ?: ButtonDefaults.outlinedButtonColors(),
                    contentPadding = contentPadding,
                    border = border ?: ButtonDefaults.outlinedButtonBorder(isEnabled),
                    content = buttonContent,
                )
            }
            AppButtonVariant.Text -> {
                TextButton(
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = isEnabled,
                    colors = colors ?: ButtonDefaults.textButtonColors(),
                    contentPadding = contentPadding,
                    content = buttonContent,
                )
            }
            AppButtonVariant.Tonal -> {
                FilledTonalButton(
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = isEnabled,
                    colors = colors ?: ButtonDefaults.filledTonalButtonColors(),
                    contentPadding = contentPadding,
                    content = buttonContent,
                )
            }
        }
    }

    if (tooltip != null) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = { Text(tooltip.title) },
                    colors = TooltipDefaults.richTooltipColors(
                        containerColor = appColors.surfaceAlt,
                        titleContentColor = appColors.textPrimary,
                        contentColor = appColors.textSecondary,
                    ),
                ) {
                    Column {
                        Text(
                            text = tooltip.description,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (tooltip.highlight != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tooltip.highlight,
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.accent,
                            )
                        }
                    }
                }
            },
            state = rememberTooltipState(isPersistent = true),
        ) {
            button()
        }
    } else {
        button()
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
    AppButton(
        onClick = onClick,
        modifier = modifier,
        variant = AppButtonVariant.Primary,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(),
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
