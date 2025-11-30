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

    suspend fun fetchFromAnki(): List<Deck> {
        val deckNamesAndIds = ankiClient.getDeckNamesAndIds()
        return deckNamesAndIds.map { (name, id) ->
            val cached = onIO { deckRepository.getDeck(id) }
            Deck(name = name, id = id, lastSyncTimestamp = cached?.lastSyncTimestamp)
        }.sortedBy { it.name }
    }
}
