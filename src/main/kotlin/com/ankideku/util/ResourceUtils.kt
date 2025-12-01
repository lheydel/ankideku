package com.ankideku.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/**
 * Loads an image resource from the classpath as a Painter.
 * This replaces the deprecated painterResource function.
 */
@Composable
fun classpathPainterResource(resourcePath: String): Painter {
    return remember(resourcePath) {
        BitmapPainter(loadImageBitmapFromClasspath(resourcePath))
    }
}

private fun loadImageBitmapFromClasspath(resourcePath: String): ImageBitmap {
    val bytes = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream(resourcePath)
        ?.readAllBytes()
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
