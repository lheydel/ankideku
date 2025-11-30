package com.ankideku.data.mapper

import com.ankideku.data.local.database.Cached_note
import com.ankideku.data.local.database.Deck_cache
import com.ankideku.data.local.database.Field_value
import com.ankideku.data.remote.anki.AnkiNoteInfo
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteField
import com.ankideku.util.parseJson

fun Deck_cache.toDomain(): Deck = Deck(
    name = name,
    id = anki_id,
    lastSyncTimestamp = last_sync_timestamp,
)

fun Cached_note.toDomain(fieldValues: List<Field_value>): Note = Note(
    id = id,
    deckId = deck_id,
    deckName = deck_name,
    modelName = model_name,
    fields = fieldValues.toNoteFields(),
    tags = tags.parseJson(),
    mod = mod,
    estimatedTokens = estimated_tokens?.toInt(),
)

fun AnkiNoteInfo.toDomain(deckId: Long, deckName: String): Note = Note(
    id = noteId,
    deckId = deckId,
    deckName = deckName,
    modelName = modelName,
    fields = fields.mapValues { (name, field) ->
        NoteField(
            name = name,
            value = field.value,
            order = field.order,
        )
    },
    tags = tags,
    mod = mod,
)
