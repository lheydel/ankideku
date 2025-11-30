package com.ankideku

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ankideku.domain.model.AppTheme
import com.ankideku.ui.screens.main.MainScreen
import com.ankideku.ui.screens.main.MainViewModel
import com.ankideku.ui.theme.AnkiDekuTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val viewModel: MainViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    val darkTheme = when (uiState.settings.theme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System -> isSystemInDarkTheme()
    }

    AnkiDekuTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
