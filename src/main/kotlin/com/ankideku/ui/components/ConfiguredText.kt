package com.ankideku.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.ankideku.domain.model.FontOption
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.theme.AppFonts

/**
 * Text composable that applies font configuration based on the field and note type.
 *
 * @param text The text to display
 * @param fieldName The name of the field (used to look up font config)
 * @param noteTypeConfig The config for this note type (contains field font mappings)
 * @param modifier Modifier for the Text
 * @param style Base TextStyle to apply (font will override fontFamily if configured)
 * @param color Text color
 * @param maxLines Maximum lines before truncation
 * @param overflow How to handle overflow
 */
@Composable
fun ConfiguredText(
    text: String,
    fieldName: String,
    noteTypeConfig: NoteTypeConfig?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val fontOption = noteTypeConfig?.fieldFontConfig?.get(fieldName) ?: FontOption.DEFAULT
    val fontFamily = AppFonts.forOption(fontOption)

    val effectiveStyle = if (fontFamily != null) {
        style.copy(fontFamily = fontFamily)
    } else {
        style
    }

    Text(
        text = text,
        modifier = modifier,
        style = effectiveStyle,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}
