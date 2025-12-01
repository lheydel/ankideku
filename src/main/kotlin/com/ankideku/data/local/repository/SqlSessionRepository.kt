package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.DeckId
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlSessionRepository(
    private val database: AnkiDekuDb,
) : SessionRepository {

    override fun getAll(): Flow<List<Session>> {
        return database.sessionQueries.getAllSessionsWithPendingCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getForDeck(deckId: DeckId): Flow<List<Session>> {
        return database.sessionQueries.getSessionsForDeck(deckId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: SessionId): Session? {
        return database.sessionQueries.getSessionById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun create(session: Session): SessionId {
        val now = System.currentTimeMillis()
        var sessionId: SessionId = 0
        database.transaction {
            database.sessionQueries.insertSession(
                deck_id = session.deckId,
                deck_name = session.deckName,
                prompt = session.prompt,
                state = session.state.dbString,
                progress_total_cards = session.progress.totalCards.toLong(),
                progress_total_batches = session.progress.totalBatches.toLong(),
                created_at = now,
                updated_at = now,
            )
            sessionId = database.sessionQueries.lastInsertedSessionId().executeAsOne()
        }
        return sessionId
    }

    override fun updateState(sessionId: SessionId, state: SessionState) {
        val (stateMessage, exitCode) = when (state) {
            is SessionState.Failed -> state.message to state.exitCode?.toLong()
            else -> null to null
        }

        database.sessionQueries.updateSessionState(
            state = state.dbString,
            state_message = stateMessage,
            exit_code = exitCode,
            updated_at = System.currentTimeMillis(),
            id = sessionId,
        )
    }

    override fun updateProgress(sessionId: SessionId, progress: SessionProgress) {
        database.sessionQueries.updateSessionProgress(
            progress_processed_cards = progress.processedCards.toLong(),
            progress_total_cards = progress.totalCards.toLong(),
            progress_processed_batches = progress.processedBatches.toLong(),
            progress_total_batches = progress.totalBatches.toLong(),
            progress_suggestions_count = progress.suggestionsCount.toLong(),
            progress_input_tokens = progress.inputTokens.toLong(),
            progress_output_tokens = progress.outputTokens.toLong(),
            progress_failed_batches = progress.failedBatches.toLong(),
            updated_at = System.currentTimeMillis(),
            id = sessionId,
        )
    }

    override fun delete(sessionId: SessionId) {
        database.sessionQueries.deleteSession(sessionId)
    }
}
