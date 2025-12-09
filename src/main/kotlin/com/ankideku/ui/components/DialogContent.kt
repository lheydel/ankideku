package com.ankideku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Reusable dialog content layout with consistent styling.
 * Provides title, optional message, optional custom content, and button row.
 */
@Composable
fun DialogContent(
    title: String,
    titleColor: Color = LocalAppColors.current.textPrimary,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.padding(Spacing.lg)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
        )

        if (message != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        if (content != null) {
            Spacer(Modifier.height(Spacing.md))
            content()
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = buttons,
        )
    }
}
