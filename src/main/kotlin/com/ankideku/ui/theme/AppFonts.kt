package com.ankideku.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.ankideku.domain.model.FontOption

object AppFonts {
    val NotoSansJP: FontFamily by lazy {
        FontFamily(
            Font(
                resource = "fonts/NotoSansJP-Medium.ttf",
                weight = FontWeight.Medium,
            )
        )
    }

    /**
     * Get the FontFamily for a given FontOption.
     * Returns null for DEFAULT (meaning use the default app font).
     */
    fun forOption(option: FontOption): FontFamily? = when (option) {
        FontOption.DEFAULT -> null
        FontOption.NOTO_SANS_JP -> NotoSansJP
    }
}
