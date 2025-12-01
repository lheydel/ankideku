package com.ankideku

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ankideku.di.appModule
import com.ankideku.util.WindowStateManager
import com.ankideku.util.classpathPainterResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.context.startKoin

@OptIn(FlowPreview::class)
fun main() = application {
    // Initialize Koin
    startKoin {
        modules(appModule)
    }

    // Load saved window state or use defaults
    val windowState = WindowStateManager.loadOrDefault()

    Window(
        onCloseRequest = {
            // Save state before closing
            WindowStateManager.save(windowState)
            exitApplication()
        },
        title = "AnkiDeku",
        icon = classpathPainterResource("icons/icon.png"),
        state = windowState,
    ) {
        // Set minimum window size
        window.minimumSize = java.awt.Dimension(760, 710)

        // Save window state when it changes (debounced to avoid excessive writes)
        LaunchedEffect(windowState) {
            combine(
                snapshotFlow { windowState.position },
                snapshotFlow { windowState.size },
                snapshotFlow { windowState.placement }
            ) { position, size, placement -> Triple(position, size, placement) }
                .debounce(500) // Wait 500ms after last change before saving
                .onEach { WindowStateManager.save(windowState) }
                .launchIn(this)
        }

        App()
    }
}
