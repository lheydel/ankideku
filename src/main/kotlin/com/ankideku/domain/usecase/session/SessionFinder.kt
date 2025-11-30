package com.ankideku.domain.usecase.session

import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.util.onIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SessionFinder(
    private val sessionRepository: SessionRepository,
) {
    fun observeAll(): Flow<List<Session>> = sessionRepository.getAll()

    fun observeForDeck(deckId: DeckId): Flow<List<Session>> = sessionRepository.getForDeck(deckId)

    suspend fun getById(id: SessionId): Session? = onIO { sessionRepository.getById(id) }

    suspend fun getForDeck(deckId: DeckId): List<Session> =
        onIO { sessionRepository.getForDeck(deckId) }.first()

    suspend fun getLatestForDeck(deckId: DeckId): Session? =
        getForDeck(deckId).maxByOrNull { it.createdAt }

    suspend fun delete(sessionId: SessionId) = onIO {
        sessionRepository.delete(sessionId)
    }
}
