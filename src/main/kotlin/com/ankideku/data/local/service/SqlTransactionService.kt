package com.ankideku.data.local.service

import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.domain.service.TransactionService
import com.ankideku.util.onIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlTransactionService(
    private val database: AnkiDekuDb,
) : TransactionService {

    override suspend fun <T> runInTransaction(block: () -> T): T = onIO {
        database.transactionWithResult { block() }
    }
}
