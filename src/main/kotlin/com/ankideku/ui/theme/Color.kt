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

    // Gray (neutral)
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

    // Slate (cooler, more modern gray with blue undertone)
    val Slate50 = Color(0xFFf8fafc)
    val Slate100 = Color(0xFFf1f5f9)
    val Slate200 = Color(0xFFe2e8f0)
    val Slate300 = Color(0xFFcbd5e1)
    val Slate400 = Color(0xFF94a3b8)
    val Slate500 = Color(0xFF64748b)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1e293b)
    val Slate900 = Color(0xFF0f172a)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFA726)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF42A5F5)

    // Material 3 scheme colors
    val BackgroundLight = Color(0xFFe2e8f0)  // Slate-200 (more visible tint)
    val BackgroundDark = Color(0xFF0f172a)   // Slate-900
    val SurfaceLight = Color(0xFFf8fafc)     // Slate-50 for cards
    val SurfaceDark = Color(0xFF1e293b)      // Slate-800
    val SurfaceVariantLight = Color(0xFFcbd5e1)  // Slate-300
    val SurfaceVariantDark = Color(0xFF334155)   // Slate-700
    val OnBackgroundLight = Color(0xFF0f172a)  // Slate-900
    val OnBackgroundDark = Color(0xFFf1f5f9)   // Slate-100
    val OnSurfaceLight = Color(0xFF0f172a)     // Slate-900
    val OnSurfaceDark = Color(0xFFf1f5f9)      // Slate-100
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
    val diffAddedText: Color,
    val diffRemoved: Color,
    val diffRemovedText: Color,

    // Field row background when changed (V1: bg-yellow-50/30)
    val fieldChangedBg: Color,

    // Header gradient (for cards/content headers)
    val headerGradientStart: Color,
    val headerGradientEnd: Color,

    // App header bar gradient
    val appHeaderStart: Color,
    val appHeaderMid: Color,
    val appHeaderEnd: Color,

    // Chat area
    val chatBackground: Color,
    val assistantBubble: Color,
    val userBubble: Color,
    val onUserBubble: Color,

    // Main content gradient (V1: bg-gradient-to-br from-gray-50 to-gray-100)
    val contentGradientStart: Color,
    val contentGradientEnd: Color,
)

val LightAppColors = AppColorScheme(
    // Text
    textPrimary = Palette.Slate900,
    textSecondary = Palette.Slate600,
    textMuted = Palette.Slate400,

    // Surfaces - tinted surfaces for modern look
    surface = Palette.Slate50,           // Subtle tint instead of pure white
    surfaceAlt = Palette.Slate200,       // More visible depth
    border = Palette.Slate300,
    borderMuted = Palette.Slate200,
    divider = Palette.Slate200,

    // Primary (green)
    accent = Palette.Primary,
    accentMuted = Color(0xFFecfdf5),     // Emerald-50 (richer green tint)
    accentStrong = Palette.Primary600,
    accentSurface = Color(0xFFecfdf5),   // Emerald-50
    accentOnSurface = Palette.Primary700,
    accentTrack = Palette.Primary200,
    onAccent = Color.White,

    // Secondary (blue)
    secondary = Color(0xFF3b82f6),       // Blue-500 (more vibrant)
    secondaryMuted = Color(0xFFeff6ff),  // Blue-50

    // Warning (amber)
    warning = Color(0xFFf59e0b),         // Amber-500
    warningMuted = Color(0xFFFEF3C7),    // Amber-100 (richer)
    warningBorder = Color(0xFFFCD34D),   // Amber-300
    warningText = Color(0xFF78350F),     // Amber-900

    // Success
    success = Color(0xFF10b981),         // Emerald-500 (more modern)
    successMuted = Color(0xFFd1fae5),    // Emerald-100

    // Error
    error = Color(0xFFef4444),           // Red-500
    errorMuted = Color(0xFFfee2e2),      // Red-100

    // Diff - richer colors for better visibility
    diffAdded = Color(0xFFd1fae5),       // Emerald-100
    diffAddedText = Color(0xFF065f46),   // Emerald-800
    diffRemoved = Color(0xFFfee2e2),     // Red-100
    diffRemovedText = Color(0xFF991b1b), // Red-800

    // Field row background
    fieldChangedBg = Color(0xFFfef9c3).copy(alpha = 0.5f),  // Yellow-100 at 50%

    // Header gradient - subtle emerald to blue
    headerGradientStart = Color(0xFFecfdf5),  // Emerald-50
    headerGradientEnd = Color(0xFFeff6ff),    // Blue-50

    // App header bar - clean subtle gradient
    appHeaderStart = Palette.Slate50,
    appHeaderMid = Palette.Slate100,
    appHeaderEnd = Palette.Slate50,

    // Chat area - slate tones
    chatBackground = Palette.Slate100,
    assistantBubble = Palette.Slate200,
    userBubble = Palette.Primary600,        // Darker green for good contrast with white
    onUserBubble = Color.White,

    // Main content gradient - more visible depth
    contentGradientStart = Palette.Slate100,
    contentGradientEnd = Palette.Slate200,
)

val DarkAppColors = AppColorScheme(
    // Text
    textPrimary = Palette.Slate100,
    textSecondary = Palette.Slate300,
    textMuted = Palette.Slate400,

    // Surfaces - slate tones for consistency
    surface = Palette.Slate800,
    surfaceAlt = Palette.Slate700,
    border = Palette.Slate600,
    borderMuted = Palette.Slate700,
    divider = Palette.Slate600,

    // Primary (green)
    accent = Color(0xFF10b981),          // Emerald-500
    accentMuted = Color(0xFF10b981).copy(alpha = 0.15f),
    accentStrong = Color(0xFF34d399),    // Emerald-400
    accentSurface = Color(0xFF064e3b).copy(alpha = 0.4f),  // Emerald-900
    accentOnSurface = Color(0xFF6ee7b7), // Emerald-300
    accentTrack = Color(0xFF064e3b),     // Emerald-900
    onAccent = Color.White,

    // Secondary (blue)
    secondary = Color(0xFF60a5fa),       // Blue-400
    secondaryMuted = Color(0xFF3b82f6).copy(alpha = 0.15f),

    // Warning (amber)
    warning = Color(0xFFfbbf24),         // Amber-400
    warningMuted = Color(0xFFf59e0b).copy(alpha = 0.15f),
    warningBorder = Color(0xFF92400e).copy(alpha = 0.5f),  // Amber-700
    warningText = Color(0xFFFDE68A),     // Amber-200

    // Success
    success = Color(0xFF34d399),         // Emerald-400
    successMuted = Color(0xFF10b981).copy(alpha = 0.15f),

    // Error
    error = Color(0xFFf87171),           // Red-400
    errorMuted = Color(0xFFef4444).copy(alpha = 0.15f),

    // Diff
    diffAdded = Color(0xFF064e3b).copy(alpha = 0.5f),     // Emerald-900
    diffAddedText = Color(0xFF6ee7b7),                     // Emerald-300
    diffRemoved = Color(0xFF7f1d1d).copy(alpha = 0.5f),   // Red-900
    diffRemovedText = Color(0xFFfca5a5),                   // Red-300

    // Field row background
    fieldChangedBg = Color(0xFF713f12).copy(alpha = 0.25f),  // Yellow-900

    // Header gradient
    headerGradientStart = Color(0xFF064e3b).copy(alpha = 0.3f),  // Emerald-900
    headerGradientEnd = Color(0xFF1e3a8a).copy(alpha = 0.3f),    // Blue-900

    // App header bar - subtle gradient in dark theme
    appHeaderStart = Palette.Slate800,
    appHeaderMid = Palette.Slate800.copy(alpha = 0.95f),
    appHeaderEnd = Color(0xFF10b981).copy(alpha = 0.05f),  // Subtle emerald tint

    // Chat area
    chatBackground = Palette.Slate900,
    assistantBubble = Palette.Slate700,
    userBubble = Color(0xFF15803d),         // Emerald-700 - darker for dark mode
    onUserBubble = Color.White,

    // Main content gradient
    contentGradientStart = Palette.Slate900,
    contentGradientEnd = Palette.Slate800,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
