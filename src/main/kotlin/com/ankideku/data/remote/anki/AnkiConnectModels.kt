package com.ankideku.data.remote.anki

import com.ankideku.util.AnySerializer
import kotlinx.serialization.Serializable

@Serializable
data class AnkiConnectRequest(
    val action: String,
    val version: Int = 6,
    val params: Map<String, @Serializable(with = AnySerializer::class) Any?> = emptyMap(),
)

@Serializable
data class AnkiConnectResponse<T>(
    val result: T?,
    val error: String?,
)

// Response models for specific actions
@Serializable
data class AnkiNoteInfo(
    val noteId: Long,
    val modelName: String,
    val tags: List<String>,
    val fields: Map<String, AnkiFieldValue>,
    val cards: List<Long>,
    val mod: Long,
)

@Serializable
data class AnkiFieldValue(
    val value: String,
    val order: Int,
)

@Serializable
data class AnkiCardInfo(
    val cardId: Long,
    val deckName: String,
)

