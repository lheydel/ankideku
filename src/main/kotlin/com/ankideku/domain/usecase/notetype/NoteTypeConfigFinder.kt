package com.ankideku.domain.usecase.notetype

import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.repository.NoteTypeConfigRepository
import com.ankideku.util.onIO
import kotlinx.coroutines.flow.Flow

class NoteTypeConfigFinder(
    private val repository: NoteTypeConfigRepository,
) {
    fun observeAllConfigs(): Flow<List<NoteTypeConfig>> = repository.observeAllConfigs()

    fun observeConfig(modelName: String): Flow<NoteTypeConfig?> = repository.observeConfig(modelName)

    suspend fun getConfig(modelName: String): NoteTypeConfig? = onIO { repository.getConfig(modelName) }

    suspend fun getAllConfigs(): List<NoteTypeConfig> = onIO { repository.getAllConfigs() }

    suspend fun saveConfig(config: NoteTypeConfig) = onIO { repository.saveConfig(config) }

    suspend fun deleteConfig(modelName: String) = onIO { repository.deleteConfig(modelName) }

    /**
     * Get the default display field for a model name.
     * Returns null if no config exists or no default field is set.
     */
    suspend fun getDefaultDisplayField(modelName: String): String? {
        return getConfig(modelName)?.defaultDisplayField
    }

    /**
     * Get a map of model name to default display field for all configured note types.
     */
    suspend fun getDefaultDisplayFieldMap(): Map<String, String> {
        return getAllConfigs()
            .filter { it.defaultDisplayField != null }
            .associate { it.modelName to it.defaultDisplayField!! }
    }
}
