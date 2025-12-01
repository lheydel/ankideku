package com.ankideku.ui.screens.main

import com.ankideku.data.remote.anki.AnkiConnectionMonitor
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.history.HistoryFinder
import com.ankideku.domain.usecase.suggestion.ReviewSuggestionFeature
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SessionOrchestrator
import com.ankideku.domain.usecase.settings.SettingsManager
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.ui.screens.main.actions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel using Kotlin interface delegation.
 */
class MainViewModel(
    private val ctx: ViewModelContext = ViewModelContext(CoroutineScope(Dispatchers.Main + SupervisorJob())),
    syncDeckFeature: SyncDeckFeature,
    sessionOrchestrator: SessionOrchestrator,
    reviewSuggestionFeature: ReviewSuggestionFeature,
    private val deckFinder: DeckFinder,
    private val sessionFinder: SessionFinder,
    private val suggestionFinder: SuggestionFinder,
    private val historyFinder: HistoryFinder,
    private val settingsManager: SettingsManager,
    private val connectionMonitor: AnkiConnectionMonitor,
) : DeckActions by DeckActionsImpl(ctx, syncDeckFeature, deckFinder, sessionFinder, suggestionFinder),
    SessionActions by SessionActionsImpl(ctx, sessionOrchestrator, sessionFinder, suggestionFinder),
    ReviewActions by ReviewActionsImpl(ctx, reviewSuggestionFeature),
    HistoryActions by HistoryActionsImpl(ctx, historyFinder),
    SettingsActions by SettingsActionsImpl(ctx, settingsManager),
    UIActions by UIActionsImpl(ctx, connectionMonitor)
{
    val uiState: StateFlow<MainUiState> = ctx.uiState

    init {
        observeAnkiConnection()
        loadSettings()
    }

    private fun observeAnkiConnection() {
        ctx.scope.launch {
            connectionMonitor.isConnected.collect { isConnected ->
                ctx.update { copy(ankiConnected = isConnected) }
                if (isConnected) {
                    ctx.update { copy(ankiError = null) }
                    refreshDecks()
                }
            }
        }
        ctx.scope.launch {
            connectionMonitor.lastError.collect { error ->
                ctx.update { copy(ankiError = error) }
            }
        }
    }

    private fun loadSettings() {
        ctx.scope.launch {
            val settings = settingsManager.get()
            ctx.update { copy(settings = settings) }
        }
    }

    fun dispose() {
        cancelSync()
        ctx.scope.cancel()
    }
}
