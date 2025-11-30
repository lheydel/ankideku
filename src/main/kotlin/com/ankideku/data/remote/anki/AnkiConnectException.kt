package com.ankideku.data.remote.anki

sealed class AnkiConnectException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotRunning : AnkiConnectException(
        "AnkiConnect is not running. Please start Anki with the AnkiConnect addon installed."
    )

    class ConnectionReset : AnkiConnectException(
        "Connection reset by AnkiConnect. The request may be too large."
    )

    class Timeout : AnkiConnectException(
        "Request timed out. Try reducing the number of cards."
    )

    class ApiError(error: String) : AnkiConnectException(
        "AnkiConnect error: $error"
    )

    class NetworkError(cause: Throwable) : AnkiConnectException(
        "Network error: ${cause.message}",
        cause
    )
}
