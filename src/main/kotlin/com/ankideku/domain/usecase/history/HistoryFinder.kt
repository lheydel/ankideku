package com.ankideku.domain.usecase.history

import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.util.onIO

class HistoryFinder(
    private val historyRepository: HistoryRepository,
) {
    suspend fun getForSession(sessionId: SessionId): List<HistoryEntry> =
        onIO { historyRepository.getForSession(sessionId) }

    suspend fun getAll(limit: Int = 100): List<HistoryEntry> =
        onIO { historyRepository.getAll(limit) }
}
