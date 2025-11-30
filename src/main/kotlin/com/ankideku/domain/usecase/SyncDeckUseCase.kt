package com.ankideku.domain.usecase

import com.ankideku.data.mapper.toDomain
import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.Note
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.service.TransactionService
import com.ankideku.util.TokenEstimator
import com.ankideku.util.onIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Synchronizes deck notes from Anki to local cache.
 * Queries each sub-deck separately to avoid slow cardsInfo enrichment.
 */
class SyncDeckUseCase(
    private val ankiClient: AnkiConnectClient,
    private val deckRepository: DeckRepository,
    private val transactionService: TransactionService,
) {
    companion object {
        private const val BATCH_SIZE = 100
        private const val BATCH_DELAY_MS = 100L
        private const val SECONDS_PER_DAY = 86400
    }

    operator fun invoke(deckId: DeckId): Flow<SyncProgress> = flow {
        val deckNamesAndIds = ankiClient.getDeckNamesAndIds()
        val rootDeckName = resolveDeckName(deckId, deckNamesAndIds)

        // Find all decks to sync: root deck + all sub-decks
        val decksToSync = deckNamesAndIds.keys
            .filter { it == rootDeckName || it.startsWith("$rootDeckName::") }
            .sorted()

        val lastSyncTimestamp = onIO { deckRepository.getDeck(deckId)?.lastSyncTimestamp }
        val isIncremental = lastSyncTimestamp != null

        emit(SyncProgress.Starting(deckId, rootDeckName, isIncremental))

        if (decksToSync.isEmpty()) {
            saveDeck(deckId, rootDeckName)
            emit(SyncProgress.Completed(deckId, noteCount = 0, tokenEstimate = 0))
            return@flow
        }

        // Sync each sub-deck separately
        val allNotes = mutableListOf<Note>()
        val totalSteps = decksToSync.size

        decksToSync.forEachIndexed { index, subDeckName ->
            val step = index + 1
            val subDeckId = deckNamesAndIds[subDeckName] ?: deckId

            emit(SyncProgress.SyncingSubDeck(deckId, step, totalSteps, getShortDeckName(subDeckName, rootDeckName)))

            allNotes += syncSingleDeck(subDeckName, subDeckId, lastSyncTimestamp)

            if (index < decksToSync.lastIndex) {
                delay(BATCH_DELAY_MS)
            }
        }

        emit(SyncProgress.SavingToCache(deckId, allNotes.size))

        transactionService.runInTransaction {
            saveDeckSync(deckId, rootDeckName)
            deckRepository.saveNotes(allNotes)
        }

        val totalTokens = allNotes.sumOf { it.estimatedTokens ?: 0 }
        emit(SyncProgress.Completed(deckId, noteCount = allNotes.size, tokenEstimate = totalTokens))
    }

    private suspend fun syncSingleDeck(
        deckName: String,
        deckId: DeckId,
        lastSyncTimestamp: Long?,
    ): List<Note> {
        val query = buildSingleDeckQuery(deckName, lastSyncTimestamp)
        val noteIds = ankiClient.findNotes(query)

        if (noteIds.isEmpty()) return emptyList()

        return noteIds.chunked(BATCH_SIZE).flatMap { batchNoteIds ->
            val notes = fetchNoteBatch(batchNoteIds, deckName, deckId)
            delay(BATCH_DELAY_MS)
            notes
        }
    }

    private fun resolveDeckName(deckId: DeckId, deckNamesAndIds: Map<String, DeckId>): String {
        return deckNamesAndIds.entries.find { it.value == deckId }?.key
            ?: throw SyncException.DeckNotFound(deckId)
    }

    private fun buildSingleDeckQuery(deckName: String, lastSyncTimestamp: Long?): String {
        // Query exact deck only, exclude sub-decks
        var query = "deck:\"$deckName\" -deck:\"$deckName::*\""

        if (lastSyncTimestamp != null) {
            val nowSeconds = System.currentTimeMillis() / 1000
            val daysSinceSync = ((nowSeconds - lastSyncTimestamp / 1000) / SECONDS_PER_DAY).toInt()
            query += " edited:${maxOf(1, daysSinceSync)}"
        }

        return query
    }

    private fun getShortDeckName(deckName: String, rootDeckName: String): String {
        if (deckName == rootDeckName) return deckName
        if (deckName.startsWith("$rootDeckName::")) {
            return deckName.substring(rootDeckName.length + 2)
        }
        return deckName
    }

    private suspend fun fetchNoteBatch(
        noteIds: List<Long>,
        deckName: String,
        deckId: DeckId,
    ): List<Note> {
        val noteInfos = fetchWithRetry(noteIds) { ankiClient.notesInfo(it) }
        return noteInfos.map { noteInfo ->
            val tokens = TokenEstimator.estimate(noteInfo.fields.values.map { it.value })
            noteInfo.toDomain(deckId, deckName, tokens)
        }
    }

    private suspend fun <T, R> fetchWithRetry(
        items: List<T>,
        depth: Int = 0,
        fetch: suspend (List<T>) -> List<R>,
    ): List<R> {
        return try {
            fetch(items)
        } catch (e: Exception) {
            if (items.size == 1) {
                println("${"  ".repeat(depth)}Failed to fetch single item: ${e.message}")
                emptyList()
            } else {
                val mid = items.size / 2
                val firstHalf = items.subList(0, mid)
                val secondHalf = items.subList(mid, items.size)
                println("${"  ".repeat(depth)}Splitting failed batch of ${items.size} into ${firstHalf.size} + ${secondHalf.size}")

                val firstResults = fetchWithRetry(firstHalf, depth + 1, fetch)
                delay(BATCH_DELAY_MS)
                val secondResults = fetchWithRetry(secondHalf, depth + 1, fetch)

                firstResults + secondResults
            }
        }
    }

    private suspend fun saveDeck(deckId: DeckId, deckName: String) {
        onIO { saveDeckSync(deckId, deckName) }
    }

    private fun saveDeckSync(deckId: DeckId, deckName: String) {
        val deck = Deck(name = deckName, id = deckId, lastSyncTimestamp = System.currentTimeMillis())
        deckRepository.saveDeck(deck)
    }
}

/**
 * Progress events emitted during deck sync
 */
sealed class SyncProgress {
    abstract val deckId: DeckId

    data class Starting(override val deckId: DeckId, val deckName: String, val isIncremental: Boolean) : SyncProgress()
    data class SyncingSubDeck(override val deckId: DeckId, val step: Int, val totalSteps: Int, val subDeckName: String) : SyncProgress()
    data class SavingToCache(override val deckId: DeckId, val noteCount: Int) : SyncProgress()
    data class Completed(override val deckId: DeckId, val noteCount: Int, val tokenEstimate: Int) : SyncProgress()
}

/**
 * Exceptions that can occur during sync
 */
sealed class SyncException(message: String) : Exception(message) {
    data class DeckNotFound(val deckId: DeckId) : SyncException("Deck not found: $deckId")
}
