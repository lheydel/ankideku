package com.ankideku.domain.usecase.suggestion

import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.util.onIO
import kotlinx.coroutines.flow.Flow

class SuggestionFinder(
    private val suggestionRepository: SuggestionRepository,
) {
    fun observeForSession(sessionId: SessionId): Flow<List<Suggestion>> =
        suggestionRepository.getForSession(sessionId)

    fun observePendingForSession(sessionId: SessionId): Flow<List<Suggestion>> =
        suggestionRepository.getPendingForSession(sessionId)

    suspend fun getById(id: SuggestionId): Suggestion? = onIO { suggestionRepository.getById(id) }
}
