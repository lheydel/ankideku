package com.ankideku.domain.usecase

import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.util.onIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Search across history entries.
 * Searches reasoning text and deck names.
 */
class SearchHistoryUseCase(
    private val historyRepository: HistoryRepository,
) {
    /**
     * Search history entries by query.
     * @param query Search query (searches reasoning and deck_name)
     * @param limit Maximum number of results to return
     * @return List of matching history entries, ordered by timestamp descending
     */
    suspend operator fun invoke(query: String, limit: Int = 100): List<HistoryEntry> {
        if (query.isBlank()) {
            return emptyList()
        }
        return onIO { historyRepository.search(query.trim(), limit) }
    }
}
