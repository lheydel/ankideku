package com.ankideku.domain.repository

import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI processing sessions.
 * Methods are blocking - caller is responsible for dispatcher management.
 */
interface SessionRepository {
    fun getAll(): Flow<List<Session>>
    fun getById(id: SessionId): Session?
    fun create(session: Session): SessionId
    fun updateState(sessionId: SessionId, state: SessionState)
    fun updateProgress(sessionId: SessionId, progress: SessionProgress)
    fun delete(sessionId: SessionId)
}
