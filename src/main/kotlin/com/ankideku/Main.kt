package com.ankideku

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ankideku.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    // Initialize Koin
    startKoin {
        modules(appModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "AnkiDeku",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        App()
    }
}
