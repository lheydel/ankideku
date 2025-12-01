package com.ankideku.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NoteField(
    val name: String,
    val value: String,
    val order: Int,
)

data class Note(
    val id: NoteId,
    val deckId: DeckId,
    val deckName: String,
    val modelName: String,
    val fields: Map<String, NoteField>,
    val tags: List<String>,
    val mod: Long,  // Modification timestamp from Anki
    val estimatedTokens: Int? = null,
)

data class Deck(
    val name: String,
    val id: DeckId,
    val parentId: DeckId? = null,
    val lastSyncTimestamp: Long? = null,
    val noteCount: Int = 0,
    val tokenEstimate: Int = 0,
)
