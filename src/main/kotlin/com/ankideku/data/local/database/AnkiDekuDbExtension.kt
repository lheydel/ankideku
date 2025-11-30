package com.ankideku.data.local.database

import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.TransactionWithoutReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun AnkiDekuDb.withTransaction(block: TransactionWithoutReturn.() -> Unit) = withContext(Dispatchers.IO) {
    transaction {
        block()
    }
}

suspend fun <R> AnkiDekuDb.withTransactionResult(block: TransactionWithReturn<R>.() -> R) = withContext(Dispatchers.IO) {
    transactionWithResult {
        block()
    }
}
