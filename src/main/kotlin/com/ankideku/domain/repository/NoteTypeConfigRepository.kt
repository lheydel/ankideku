package com.ankideku.domain.repository

import com.ankideku.domain.model.NoteTypeConfig
import kotlinx.coroutines.flow.Flow

interface NoteTypeConfigRepository {
    suspend fun getConfig(modelName: String): NoteTypeConfig?
    suspend fun getAllConfigs(): List<NoteTypeConfig>
    suspend fun saveConfig(config: NoteTypeConfig)
    suspend fun deleteConfig(modelName: String)
    fun observeConfig(modelName: String): Flow<NoteTypeConfig?>
    fun observeAllConfigs(): Flow<List<NoteTypeConfig>>
}
