package com.ankideku.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw color palette - not meant for direct use in components.
 */
object Palette {
    // Primary (Green)
    val Primary = Color(0xFF0F8A35)
    val Primary50 = Color(0xFFf0fdf4)
    val Primary100 = Color(0xFFdcfce7)
    val Primary200 = Color(0xFFbbf7d0)
    val Primary600 = Color(0xFF16a34a)
    val Primary700 = Color(0xFF15803d)
    val Primary800 = Color(0xFF021f11)
    val Primary900 = Color(0xFF033F21)
    val PrimaryContainer = Color(0xFF1a5c2e)
    val OnPrimaryContainer = Color(0xFFb8f5c8)

    // Blue
    val Blue50 = Color(0xFFeff6ff)
    val Blue900 = Color(0xFF1e3a8a)

    // Amber
    val Amber50 = Color(0xFFFFFBEB)

    // Gray
    val Gray50 = Color(0xFFf9fafb)
    val Gray100 = Color(0xFFf3f4f6)
    val Gray200 = Color(0xFFe5e7eb)
    val Gray300 = Color(0xFFd1d5db)
    val Gray400 = Color(0xFF9ca3af)
    val Gray500 = Color(0xFF6b7280)
    val Gray600 = Color(0xFF4b5563)
    val Gray700 = Color(0xFF374151)
    val Gray750 = Color(0xFF2d3748)
    val Gray800 = Color(0xFF1f2937)
    val Gray900 = Color(0xFF111827)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFA726)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF42A5F5)

    // Material 3 scheme colors
    val BackgroundLight = Color(0xFFf9fafb)  // Gray-50
    val BackgroundDark = Color(0xFF111827)   // Gray-900
    val SurfaceLight = Color.White
    val SurfaceDark = Color(0xFF1f2937)      // Gray-800
    val SurfaceVariantLight = Color(0xFFf3f4f6)  // Gray-100
    val SurfaceVariantDark = Color(0xFF374151)   // Gray-700
    val OnBackgroundLight = Color(0xFF111827)  // Gray-900
    val OnBackgroundDark = Color(0xFFf3f4f6)   // Gray-100
    val OnSurfaceLight = Color(0xFF111827)     // Gray-900
    val OnSurfaceDark = Color(0xFFf3f4f6)      // Gray-100
}

/**
 * Semantic color scheme with reusable tokens.
 * Components should ONLY use LocalAppColors.current - never Palette directly.
 */
@Immutable
data class AppColorScheme(
    // Text hierarchy
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Surfaces & backgrounds
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val borderMuted: Color,
    val divider: Color,

    // Primary accent (green)
    val accent: Color,
    val accentMuted: Color,
    val accentStrong: Color,
    val accentSurface: Color,      // Light accent background (for processing indicator)
    val accentOnSurface: Color,    // Text on accent surface
    val accentTrack: Color,        // Progress bar track
    val onAccent: Color,

    // Secondary accent (blue) - for info/AI states
    val secondary: Color,
    val secondaryMuted: Color,

    // Warning accent (amber) - for edit states
    val warning: Color,
    val warningMuted: Color,
    val warningBorder: Color,
    val warningText: Color,

    // Success (green - distinct from accent for status)
    val success: Color,
    val successMuted: Color,

    // Error (red)
    val error: Color,
    val errorMuted: Color,

    // Diff colors
    val diffAdded: Color,
    val diffRemoved: Color,

    // Header gradient
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
)

val LightAppColors = AppColorScheme(
    // Text
    textPrimary = Palette.Gray900,
    textSecondary = Palette.Gray700,
    textMuted = Palette.Gray500,

    // Surfaces
    surface = Color.White,
    surfaceAlt = Palette.Gray50,
    border = Palette.Gray200,
    borderMuted = Palette.Gray100,
    divider = Palette.Gray300,

    // Primary (green)
    accent = Palette.Primary,
    accentMuted = Palette.Primary50,
    accentStrong = Palette.Primary600,
    accentSurface = Palette.Primary50,
    accentOnSurface = Palette.Primary700,
    accentTrack = Palette.Primary200,
    onAccent = Color.White,

    // Secondary (blue)
    secondary = Palette.Info,
    secondaryMuted = Palette.Blue50,

    // Warning (amber)
    warning = Palette.Warning,
    warningMuted = Palette.Amber50,
    warningBorder = Color(0xFFFDE68A),  // Amber-200
    warningText = Color(0xFF78350F),    // Amber-900

    // Success
    success = Palette.Success,
    successMuted = Palette.Success.copy(alpha = 0.1f),

    // Error
    error = Palette.Error,
    errorMuted = Palette.Error.copy(alpha = 0.1f),

    // Diff
    diffAdded = Color(0xFFDCF5DC),
    diffRemoved = Color(0xFFFDECEC),

    // Header gradient
    headerGradientStart = Palette.Primary50,
    headerGradientEnd = Palette.Blue50,
)

val DarkAppColors = AppColorScheme(
    // Text
    textPrimary = Palette.Gray100,
    textSecondary = Palette.Gray300,
    textMuted = Palette.Gray500,

    // Surfaces
    surface = Palette.Gray800,
    surfaceAlt = Palette.Gray750,
    border = Palette.Gray600,
    borderMuted = Palette.Gray700,
    divider = Palette.Gray600,

    // Primary (green)
    accent = Palette.Primary,
    accentMuted = Palette.Primary.copy(alpha = 0.15f),
    accentStrong = Palette.Primary,
    accentSurface = Palette.Primary900.copy(alpha = 0.3f),
    accentOnSurface = Color(0xFF86efac),  // Primary-300
    accentTrack = Palette.Primary800,
    onAccent = Color.White,

    // Secondary (blue)
    secondary = Palette.Info,
    secondaryMuted = Palette.Info.copy(alpha = 0.15f),

    // Warning (amber)
    warning = Palette.Warning,
    warningMuted = Palette.Warning.copy(alpha = 0.15f),
    warningBorder = Color(0xFF92400e).copy(alpha = 0.5f),  // Amber-700
    warningText = Color(0xFFFDE68A),  // Amber-200

    // Success
    success = Palette.Success,
    successMuted = Palette.Success.copy(alpha = 0.15f),

    // Error
    error = Palette.Error,
    errorMuted = Palette.Error.copy(alpha = 0.15f),

    // Diff
    diffAdded = Color(0xFF1a3d1a),
    diffRemoved = Color(0xFF3d1a1a),

    // Header gradient
    headerGradientStart = Palette.Primary900.copy(alpha = 0.3f),
    headerGradientEnd = Palette.Blue900.copy(alpha = 0.3f),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
