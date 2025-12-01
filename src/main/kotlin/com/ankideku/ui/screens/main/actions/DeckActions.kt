package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.Deck
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.domain.usecase.deck.SyncException
import com.ankideku.domain.usecase.deck.SyncProgress
import com.ankideku.ui.screens.main.SyncProgressUi
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface DeckActions {
    fun selectDeck(deck: Deck)
    fun refreshDecks()
    fun syncDeck()
    fun cancelSync()
}

class DeckActionsImpl(
    private val ctx: ViewModelContext,
    private val syncDeckFeature: SyncDeckFeature,
    private val deckFinder: DeckFinder,
    private val sessionFinder: SessionFinder,
    private val suggestionFinder: SuggestionFinder,
) : DeckActions {

    private var syncJob: Job? = null

    override fun selectDeck(deck: Deck) {
        ctx.update { copy(selectedDeck = deck) }
        loadSuggestionsForDeck(deck)
    }

    override fun refreshDecks() {
        ctx.scope.launch {
            try {
                val decks = deckFinder.fetchFromAnki()
                ctx.update { copy(decks = decks) }
            } catch (e: Exception) {
                // Silently fail - decks will remain as-is
            }
        }
    }

    override fun syncDeck() {
        val deck = ctx.currentState.selectedDeck ?: return
        if (ctx.currentState.isSyncing) return

        syncJob?.cancel()
        syncJob = ctx.scope.launch {
            ctx.update { copy(isSyncing = true) }

            try {
                syncDeckFeature(deck.id).collect { progress ->
                    val uiProgress = when (progress) {
                        is SyncProgress.Starting -> SyncProgressUi(
                            deckName = progress.deckName,
                            statusText = if (progress.isIncremental) "Incremental sync..." else "Full sync...",
                        )
                        is SyncProgress.SyncingSubDeck -> SyncProgressUi(
                            deckName = progress.subDeckName,
                            statusText = "Syncing ${progress.subDeckName}",
                            step = progress.step,
                            totalSteps = progress.totalSteps,
                        )
                        is SyncProgress.SavingToCache -> SyncProgressUi(
                            deckName = deck.name,
                            statusText = "Saving ${progress.noteCount} notes...",
                        )
                        is SyncProgress.Completed -> {
                            ctx.showToast("Synced ${progress.noteCount} notes", ToastType.Success)
                            null
                        }
                    }
                    ctx.update { copy(syncProgress = uiProgress) }
                }

                // Reload deck after sync
                val updatedDeck = deckFinder.getById(deck.id)
                ctx.update {
                    copy(
                        selectedDeck = updatedDeck,
                        decks = decks.map { d -> if (d.id == deck.id) updatedDeck ?: d else d },
                    )
                }
            } catch (e: CancellationException) {
                // Ignore - expected on cancel
            } catch (e: SyncException) {
                ctx.showToast(e.message ?: "Sync error", ToastType.Error)
            } catch (e: Exception) {
                ctx.showToast("Sync: ${e.message ?: "Unknown error"}", ToastType.Error)
            } finally {
                ctx.update { copy(isSyncing = false, syncProgress = null) }
            }
        }
    }

    override fun cancelSync() {
        syncJob?.cancel()
    }

    private fun loadSuggestionsForDeck(deck: Deck) {
        ctx.scope.launch {
            try {
                val sessions = sessionFinder.getForDeck(deck.id)
                val latestSession = sessions.maxByOrNull { it.createdAt }

                ctx.update {
                    copy(
                        sessions = sessions,
                        currentSession = latestSession,
                    )
                }

                latestSession?.let { session ->
                    suggestionFinder.observePendingForSession(session.id).collect { suggestions ->
                        ctx.update {
                            copy(
                                suggestions = suggestions,
                                currentSuggestionIndex = 0,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                ctx.showToast("Failed to load suggestions: ${e.message}", ToastType.Error)
            }
        }
    }
}
