package com.ankideku.ui.components.comparison

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ankideku.ui.theme.LocalAppColors

data class HeaderStyle(
    val title: String,
    val color: Color,
    val background: Color,
    val icon: ImageVector,
)

@Composable
fun getSuggestionHeaderStyle(isEditMode: Boolean, hasManualEdits: Boolean, showOriginal: Boolean): HeaderStyle {
    val colors = LocalAppColors.current
    return when {
        isEditMode && showOriginal -> HeaderStyle("AI Suggested", colors.secondary, colors.secondaryMuted, Icons.Default.SmartToy)
        isEditMode -> HeaderStyle("Editing...", colors.warning, colors.warningMuted, Icons.Default.Edit)
        hasManualEdits -> HeaderStyle("Manually Edited", colors.warning, colors.warningMuted, Icons.Default.Edit)
        else -> HeaderStyle("Suggested Card", colors.accent, colors.accentMuted, Icons.Default.AutoAwesome)
    }
}

@Composable
fun getHistoryHeaderStyle(accepted: Boolean): HeaderStyle {
    val colors = LocalAppColors.current
    return if (accepted) {
        HeaderStyle("Applied Changes", colors.success, colors.successMuted, Icons.Default.Check)
    } else {
        HeaderStyle("Rejected Changes", colors.error, colors.errorMuted, Icons.Default.Close)
    }
}
