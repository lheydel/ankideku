package com.ankideku.domain.usecase.deck

import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.util.onIO
import kotlinx.coroutines.flow.Flow

class DeckFinder(
    private val deckRepository: DeckRepository,
    private val ankiClient: AnkiConnectClient,
) {
    fun observeAll(): Flow<List<Deck>> = deckRepository.getAllDecks()

    suspend fun getById(id: DeckId): Deck? = onIO { deckRepository.getDeck(id) }

    /**
     * Get a deck with aggregated stats (including all child decks)
     */
    suspend fun getByIdWithAggregatedStats(id: DeckId): Deck? = onIO {
        val deck = deckRepository.getDeck(id) ?: return@onIO null
        val stats = deckRepository.getDeckStats(deck.name)
        deck.copy(
            noteCount = stats.noteCount,
            tokenEstimate = stats.tokenEstimate,
        )
    }

    suspend fun fetchFromAnki(): List<Deck> {
        val deckNamesAndIds = ankiClient.getDeckNamesAndIds()
        return deckNamesAndIds.map { (name, id) ->
            val cached = onIO { deckRepository.getDeck(id) }
            Deck(
                name = name,
                id = id,
                lastSyncTimestamp = cached?.lastSyncTimestamp,
                noteCount = cached?.noteCount ?: 0,
                tokenEstimate = cached?.tokenEstimate ?: 0,
            )
        }.sortedBy { it.name }
    }

    /**
     * Fetch all note type (model) names from Anki
     */
    suspend fun fetchNoteTypes(): List<String> {
        return try {
            ankiClient.getModelNames().sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch field names for a specific note type from Anki
     */
    suspend fun fetchFieldsForNoteType(modelName: String): List<String> {
        return try {
            ankiClient.getModelFieldNames(modelName)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch fields for all note types
     */
    suspend fun fetchAllNoteTypeFields(noteTypes: List<String>): Map<String, List<String>> {
        return noteTypes.associateWith { fetchFieldsForNoteType(it) }
    }
}
