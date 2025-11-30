package com.ankideku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Palette.Primary,
    onPrimary = Color.White,
    primaryContainer = Palette.PrimaryContainer,
    onPrimaryContainer = Palette.OnPrimaryContainer,
    background = Palette.BackgroundLight,
    onBackground = Palette.OnBackgroundLight,
    surface = Palette.SurfaceLight,
    onSurface = Palette.OnSurfaceLight,
    surfaceVariant = Palette.SurfaceVariantLight,
    error = Palette.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Color.White,
    primaryContainer = Palette.PrimaryContainer,
    onPrimaryContainer = Palette.OnPrimaryContainer,
    background = Palette.BackgroundDark,
    onBackground = Palette.OnBackgroundDark,
    surface = Palette.SurfaceDark,
    onSurface = Palette.OnSurfaceDark,
    surfaceVariant = Palette.SurfaceVariantDark,
    error = Palette.Error,
)

@Composable
fun AnkiDekuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/**
 * App input field colors using semantic theme colors.
 * Uses LocalAppColors for proper light/dark theme support.
 */
@Composable
fun appInputColors(): TextFieldColors {
    val colors = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        // Container colors
        unfocusedContainerColor = colors.surfaceAlt,
        focusedContainerColor = colors.surfaceAlt,
        disabledContainerColor = colors.surfaceAlt.copy(alpha = 0.5f),
        // Border colors
        unfocusedBorderColor = colors.border,
        focusedBorderColor = colors.accentStrong,
        disabledBorderColor = colors.border.copy(alpha = 0.5f),
        // Text colors
        unfocusedTextColor = colors.textPrimary,
        focusedTextColor = colors.textPrimary,
        disabledTextColor = colors.textPrimary.copy(alpha = 0.5f),
        // Placeholder colors
        unfocusedPlaceholderColor = colors.textMuted,
        focusedPlaceholderColor = colors.textMuted,
        disabledPlaceholderColor = colors.textMuted.copy(alpha = 0.5f),
        // Cursor
        cursorColor = colors.accent,
        // Label colors
        unfocusedLabelColor = colors.textMuted,
        focusedLabelColor = colors.accent,
    )
}

/** Standard input border radius (8dp) */
val InputShape = RoundedCornerShape(8.dp)

/** Standard input padding (12dp horizontal, 6dp vertical) */
val InputPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
