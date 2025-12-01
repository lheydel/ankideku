package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.mapper.toDomain
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.repository.NoteTypeConfigRepository
import com.ankideku.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlNoteTypeConfigRepository(
    private val database: AnkiDekuDb,
) : NoteTypeConfigRepository {

    override suspend fun getConfig(modelName: String): NoteTypeConfig? {
        return database.noteTypeConfigQueries.getConfigByModelName(modelName)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun getAllConfigs(): List<NoteTypeConfig> {
        return database.noteTypeConfigQueries.getAllConfigs()
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun saveConfig(config: NoteTypeConfig) {
        database.noteTypeConfigQueries.upsertConfig(
            model_name = config.modelName,
            default_display_field = config.defaultDisplayField,
            field_font_config = config.fieldFontConfig.toJson(),
            updated_at = System.currentTimeMillis(),
        )
    }

    override suspend fun deleteConfig(modelName: String) {
        database.noteTypeConfigQueries.deleteConfig(modelName)
    }

    override fun observeConfig(modelName: String): Flow<NoteTypeConfig?> {
        return database.noteTypeConfigQueries.getConfigByModelName(modelName)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
    }

    override fun observeAllConfigs(): Flow<List<NoteTypeConfig>> {
        return database.noteTypeConfigQueries.getAllConfigs()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }
}
