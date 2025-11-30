package com.ankideku.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <R> onIO(block: suspend CoroutineScope.() -> R): R = withContext(Dispatchers.IO, block)
