package com.ankideku.ui.screens.main

import com.ankideku.data.remote.anki.AnkiConnectionMonitor
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.history.HistoryFinder
import com.ankideku.domain.usecase.notetype.NoteTypeConfigFinder
import com.ankideku.domain.sel.SelService
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.usecase.suggestion.BatchReviewFeature
import com.ankideku.domain.usecase.suggestion.ReviewSuggestionFeature
import com.ankideku.domain.usecase.review.ReviewSessionOrchestrator
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SessionOrchestrator
import com.ankideku.domain.usecase.settings.SettingsManager
import com.ankideku.domain.repository.ReviewSessionRepository
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.domain.repository.SuggestionRepository
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
    batchReviewFeature: BatchReviewFeature,
    reviewSessionOrchestrator: ReviewSessionOrchestrator,
    reviewSessionRepository: ReviewSessionRepository,
    selService: SelService,
    suggestionRepository: SuggestionRepository,
    deckRepository: DeckRepository,
    private val deckFinder: DeckFinder,
    private val sessionFinder: SessionFinder,
    private val suggestionFinder: SuggestionFinder,
    private val historyFinder: HistoryFinder,
    private val settingsManager: SettingsManager,
    private val connectionMonitor: AnkiConnectionMonitor,
    private val noteTypeConfigFinder: NoteTypeConfigFinder,
) : DeckActions by DeckActionsImpl(ctx, syncDeckFeature, deckFinder, deckRepository),
    SessionActions by SessionActionsImpl(ctx, sessionOrchestrator, sessionFinder, suggestionFinder, suggestionRepository, syncDeckFeature, deckFinder),
    ReviewActions by ReviewActionsImpl(ctx, reviewSuggestionFeature),
    BatchActions by BatchActionsImpl(ctx, batchReviewFeature, selService),
    HistoryActions by HistoryActionsImpl(ctx, historyFinder),
    SettingsActions by SettingsActionsImpl(ctx, settingsManager),
    UIActions by UIActionsImpl(ctx, connectionMonitor),
    NoteBrowseActions by NoteBrowseActionsImpl(ctx, deckRepository, selService),
    ReviewChatActions by ReviewChatActionsImpl(ctx, reviewSessionOrchestrator, reviewSessionRepository)
{
    val uiState: StateFlow<MainUiState> = ctx.uiState

    init {
        observeAnkiConnection()
        loadSettings()
        loadNoteTypeConfigs()
        observeSessions()
    }

    private fun observeAnkiConnection() {
        ctx.scope.launch {
            connectionMonitor.isConnected.collect { isConnected ->
                ctx.update { copy(ankiConnected = isConnected) }
                if (isConnected) {
                    ctx.update { copy(ankiError = null) }
                    refreshDecks()
                    loadNoteTypesFromAnki()
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

    private fun loadNoteTypeConfigs() {
        // Load initial configs
        ctx.scope.launch {
            val configs = noteTypeConfigFinder.getAllConfigs()
            val configMap = configs.associateBy { it.modelName }
            val fieldMap = configs
                .filter { it.defaultDisplayField != null }
                .associate { it.modelName to it.defaultDisplayField!! }
            ctx.update {
                copy(
                    noteTypeConfigs = configMap,
                    defaultDisplayFieldMap = fieldMap,
                )
            }
        }
        // Observe changes
        ctx.scope.launch {
            noteTypeConfigFinder.observeAllConfigs().collect { configs ->
                val configMap = configs.associateBy { it.modelName }
                val fieldMap = configs
                    .filter { it.defaultDisplayField != null }
                    .associate { it.modelName to it.defaultDisplayField!! }
                ctx.update {
                    copy(
                        noteTypeConfigs = configMap,
                        defaultDisplayFieldMap = fieldMap,
                    )
                }
            }
        }
    }

    private fun loadNoteTypesFromAnki() {
        ctx.scope.launch {
            val noteTypes = deckFinder.fetchNoteTypes()
            ctx.update { copy(availableNoteTypes = noteTypes) }

            // Load fields for all note types
            if (noteTypes.isNotEmpty()) {
                val fields = deckFinder.fetchAllNoteTypeFields(noteTypes)
                ctx.update { copy(noteTypeFields = fields) }
            }
        }
    }

    fun saveNoteTypeConfig(config: NoteTypeConfig) {
        ctx.scope.launch {
            noteTypeConfigFinder.saveConfig(config)
            // Immediately update local state to ensure UI reflects changes
            ctx.update {
                copy(
                    noteTypeConfigs = noteTypeConfigs + (config.modelName to config),
                    defaultDisplayFieldMap = if (config.defaultDisplayField != null) {
                        defaultDisplayFieldMap + (config.modelName to config.defaultDisplayField)
                    } else {
                        defaultDisplayFieldMap - config.modelName
                    }
                )
            }
        }
    }

    fun dispose() {
        cancelSync()
        ctx.scope.cancel()
    }
}
