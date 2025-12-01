package com.ankideku.data.mapper

import com.ankideku.data.local.database.Note_type_config as DbNoteTypeConfig
import com.ankideku.domain.model.FontOption
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.util.json
import com.ankideku.util.parseJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

fun DbNoteTypeConfig.toDomain(): NoteTypeConfig = NoteTypeConfig(
    modelName = model_name,
    defaultDisplayField = default_display_field,
    fieldFontConfig = field_font_config.parseJson(),
)
