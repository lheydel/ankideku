package com.ankideku.util

import java.io.File

/**
 * Get the platform-specific app data directory for AnkiDeku.
 * Creates the directory if it doesn't exist.
 *
 * - Windows: %USERPROFILE%/AppData/Roaming/AnkiDeku
 * - macOS: ~/Library/Application Support/AnkiDeku
 * - Linux: ~/.config/AnkiDeku
 */
fun getAppDataDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    val path = when {
        os.contains("win") -> "$home/AppData/Roaming/AnkiDeku"
        os.contains("mac") -> "$home/Library/Application Support/AnkiDeku"
        else -> "$home/.config/AnkiDeku"
    }

    return File(path).also { it.mkdirs() }
}
