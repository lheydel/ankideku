package com.ankideku.data.remote.anki

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors AnkiConnect connection status with background polling.
 *
 * Per design decision: background polling with warning to user if disconnected
 * and disabled suggestion validation buttons.
 */
class AnkiConnectionMonitor(
    private val client: AnkiConnectClient,
    private val pollingIntervalMs: Long = 5_000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var pollingJob: Job? = null

    /**
     * Start monitoring the connection.
     */
    fun start() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            // Initial check
            checkConnection()

            // Continuous polling
            while (isActive) {
                delay(pollingIntervalMs)
                checkConnection()
            }
        }
    }

    /**
     * Stop monitoring the connection.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Manually trigger a connection check.
     */
    suspend fun checkNow(): Boolean {
        return checkConnection()
    }

    private suspend fun checkConnection(): Boolean {
        return try {
            val connected = client.ping()
            _isConnected.value = connected
            _lastError.value = if (connected) null else "AnkiConnect is not responding"
            connected
        } catch (e: AnkiConnectException) {
            _isConnected.value = false
            _lastError.value = e.message
            false
        } catch (e: Exception) {
            _isConnected.value = false
            _lastError.value = "Connection error: ${e.message}"
            false
        }
    }
}
