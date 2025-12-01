package com.ankideku.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages window state persistence to a JSON file.
 * Saves and restores window position, size, and placement between sessions.
 */
object WindowStateManager {

    private const val STATE_FILE_NAME = "window-state.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Serializable representation of window state
     */
    @Serializable
    private data class SavedWindowState(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val isMaximized: Boolean = false
    )


    /**
     * Get the window state file path
     */
    private fun getStateFile(): File {
        return File(getAppDataDir(), STATE_FILE_NAME)
    }

    /**
     * Load saved window state, or return null if not available
     */
    fun load(): WindowState? {
        val stateFile = getStateFile()
        if (!stateFile.exists()) {
            return null
        }

        return try {
            val content = stateFile.readText()
            val saved = json.decodeFromString<SavedWindowState>(content)

            WindowState(
                position = WindowPosition(saved.x.dp, saved.y.dp),
                size = DpSize(saved.width.dp, saved.height.dp),
                placement = if (saved.isMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
            )
        } catch (e: Exception) {
            // If parsing fails, delete corrupted file and return null
            stateFile.delete()
            null
        }
    }

    /**
     * Save the current window state to file
     */
    fun save(state: WindowState) {
        // Don't save position when maximized (it's not meaningful)
        val saved = SavedWindowState(
            x = state.position.x.value.toInt(),
            y = state.position.y.value.toInt(),
            width = state.size.width.value.toInt(),
            height = state.size.height.value.toInt(),
            isMaximized = state.placement == WindowPlacement.Maximized
        )

        try {
            val stateFile = getStateFile()
            stateFile.writeText(json.encodeToString(SavedWindowState.serializer(), saved))
        } catch (e: Exception) {
            // Silently fail - window state persistence is not critical
            e.printStackTrace()
        }
    }

    /**
     * Create a WindowState with saved values or sensible defaults
     */
    fun loadOrDefault(
        defaultWidth: Int = 1600,
        defaultHeight: Int = 1000
    ): WindowState {
        return load() ?: WindowState(
            position = WindowPosition.PlatformDefault,
            size = DpSize(defaultWidth.dp, defaultHeight.dp),
            placement = WindowPlacement.Floating
        )
    }
}
