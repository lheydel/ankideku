package com.ankideku.domain.model

import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.Serializable

@Serializable
data class NoteTypeConfig(
    val modelName: String,
    val defaultDisplayField: String?,
    val fieldFontConfig: Map<String, FontOption>,
)

enum class FontOption {
    DEFAULT,
    NOTO_SANS_JP,
}
