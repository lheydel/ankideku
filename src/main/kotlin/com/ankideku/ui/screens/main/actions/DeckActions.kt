package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.Deck
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.domain.usecase.deck.SyncException
import com.ankideku.domain.usecase.deck.SyncProgress
import com.ankideku.ui.screens.main.ToastType
import com.ankideku.ui.screens.main.toUi
import com.ankideku.util.onIO
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
    private val deckRepository: DeckRepository,
) : DeckActions {

    private var syncJob: Job? = null

    override fun selectDeck(deck: Deck) {
        ctx.scope.launch {
            // Clear note filter and reset note browsing state
            ctx.update {
                copy(
                    deckNotes = emptyList(),
                    filteredNotes = null,
                    noteFilterQuery = null,
                    selectedNoteIndex = 0,
                )
            }

            // Fetch deck with aggregated stats for display
            val deckWithStats = deckFinder.getByIdWithAggregatedStats(deck.id) ?: deck
            ctx.update { copy(selectedDeck = deckWithStats) }

            // Load notes for the selected deck
            val notes = onIO { deckRepository.getNotesForDeck(deck.id) }
            ctx.update { copy(deckNotes = notes) }
        }
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
                    ctx.update { copy(syncProgress = progress.toUi(deck.name)) }
                    if (progress is SyncProgress.Completed) {
                        ctx.showToast("Synced ${progress.noteCount} notes", ToastType.Success)
                    }
                }

                // Reload deck after sync with aggregated stats
                val updatedDeck = deckFinder.getByIdWithAggregatedStats(deck.id)
                ctx.update {
                    copy(
                        selectedDeck = updatedDeck,
                        decks = decks.map { d -> if (d.id == deck.id) updatedDeck ?: d else d },
                    )
                }

                // Reload notes after sync (clear filter since notes may have changed)
                val notes = onIO { deckRepository.getNotesForDeck(deck.id) }
                ctx.update {
                    copy(
                        deckNotes = notes,
                        filteredNotes = null,
                        noteFilterQuery = null,
                        selectedNoteIndex = 0,
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

}
