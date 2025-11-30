package com.ankideku.data.remote.anki

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * Monitors AnkiConnect connection status using Flow-based polling.
 *
 * Polling automatically starts when connection state is collected and stops
 * when there are no active collectors (after a 5-second grace period).
 */
class AnkiConnectionMonitor(
    private val client: AnkiConnectClient,
    private val pollingIntervalMs: Long = 5_000,
    scope: CoroutineScope,
) {
    /** Trigger for manual refresh via [checkNow] */
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Connection state as a cold flow that polls continuously */
    private val pollingFlow = flow {
        while (true) {
            emit(checkConnection())
            delay(pollingIntervalMs)
        }
    }

    /** Manual refresh flow */
    private val manualRefreshFlow = flow {
        refreshTrigger.collect {
            emit(checkConnection())
        }
    }

    /** Combined connection state - auto-starts polling when collected */
    private val connectionState: StateFlow<ConnectionState> = merge(pollingFlow, manualRefreshFlow)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ConnectionState(isConnected = true, lastError = null),
        )

    /** Whether AnkiConnect is currently reachable */
    val isConnected: StateFlow<Boolean> = connectionState
        .mapState(scope) { it.isConnected }

    /** Last error message, if any */
    val lastError: StateFlow<String?> = connectionState
        .mapState(scope) { it.lastError }

    /**
     * Manually trigger an immediate connection check.
     * @return true if connected, false otherwise
     */
    suspend fun checkNow(): Boolean {
        val state = checkConnection()
        refreshTrigger.tryEmit(Unit)
        return state.isConnected
    }

    private suspend fun checkConnection(): ConnectionState {
        return try {
            val connected = client.ping()
            ConnectionState(
                isConnected = connected,
                lastError = if (connected) null else "AnkiConnect is not responding",
            )
        } catch (e: AnkiConnectException) {
            ConnectionState(isConnected = false, lastError = e.message)
        } catch (e: Exception) {
            ConnectionState(isConnected = false, lastError = "Connection error: ${e.message}")
        }
    }

    private data class ConnectionState(
        val isConnected: Boolean,
        val lastError: String?,
    )
}

/** Helper to map a StateFlow to a derived StateFlow */
private fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    transform: (T) -> R,
): StateFlow<R> = flow { collect { emit(transform(it)) } }
    .stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = transform(value),
    )
