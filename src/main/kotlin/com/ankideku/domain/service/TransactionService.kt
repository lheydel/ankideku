package com.ankideku.domain.service

/**
 * Service for executing atomic database operations.
 */
interface TransactionService {
    /**
     * Executes the block within a database transaction on the IO dispatcher.
     * All repository calls inside the block will be atomic.
     */
    suspend fun <T> runInTransaction(block: () -> T): T
}
