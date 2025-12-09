package com.ankideku.ui.screens.main

import com.ankideku.domain.usecase.deck.SyncProgress

/**
 * Maps domain SyncProgress to UI SyncProgressUi.
 *
 * @param fallbackDeckName Used for SavingToCache since it doesn't have the deck name in the progress.
 * @return SyncProgressUi or null for Completed state.
 */
fun SyncProgress.toUi(fallbackDeckName: String): SyncProgressUi? {
    return when (this) {
        is SyncProgress.Starting -> SyncProgressUi(
            deckName = deckName,
            statusText = if (isIncremental) "Incremental sync..." else "Full sync...",
        )
        is SyncProgress.SyncingSubDeck -> SyncProgressUi(
            deckName = subDeckName,
            statusText = "Syncing $subDeckName",
            step = step,
            totalSteps = totalSteps,
        )
        is SyncProgress.SavingToCache -> SyncProgressUi(
            deckName = fallbackDeckName,
            statusText = "Saving $noteCount notes...",
        )
        is SyncProgress.Completed -> null
    }
}
