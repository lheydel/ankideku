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
// Note: Fields have defaults to handle empty objects returned by AnkiConnect for deleted notes
@Serializable
data class AnkiNoteInfo(
    val noteId: Long = 0,
    val modelName: String = "",
    val tags: List<String> = emptyList(),
    val fields: Map<String, AnkiFieldValue> = emptyMap(),
    val cards: List<Long> = emptyList(),
    val mod: Long = 0,
) {
    /** Returns true if this is a valid note (not an empty placeholder for a deleted note) */
    val isValid: Boolean get() = noteId != 0L
}

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

