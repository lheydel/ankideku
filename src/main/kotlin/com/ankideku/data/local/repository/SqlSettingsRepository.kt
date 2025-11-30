package com.ankideku.data.local.repository

import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.database.withTransaction
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.util.parseJson
import com.ankideku.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlSettingsRepository(
    private val database: AnkiDekuDb,
) : SettingsRepository {

    companion object {
        private const val KEY_FIELD_DISPLAY_CONFIG = "field_display_config"
        private const val KEY_THEME = "theme"
        private const val KEY_LLM_PROVIDER = "llm_provider"
    }

    override suspend fun getSettings(): Settings = withContext(Dispatchers.IO) {
        val fieldDisplayConfig = getSetting(KEY_FIELD_DISPLAY_CONFIG)
            ?.parseJson<Map<String, String>>()
            ?: emptyMap()

        val theme = getSetting(KEY_THEME)?.let {
            try {
                AppTheme.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AppTheme.System
            }
        } ?: AppTheme.System

        val llmProvider = getSetting(KEY_LLM_PROVIDER)
            ?.parseJson<LlmProvider>()
            ?: LlmProvider.MOCK

        Settings(
            fieldDisplayConfig = fieldDisplayConfig,
            theme = theme,
            llmProvider = llmProvider,
        )
    }

    override suspend fun updateFieldDisplayConfig(config: Map<String, String>) = database.withTransaction {
        setSetting(KEY_FIELD_DISPLAY_CONFIG, config.toJson())
    }

    override suspend fun updateTheme(theme: AppTheme) = database.withTransaction {
        setSetting(KEY_THEME, theme.name)
    }

    override suspend fun updateLlmProvider(provider: LlmProvider) = database.withTransaction {
        setSetting(KEY_LLM_PROVIDER, provider.toJson())
    }

    private fun getSetting(key: String): String? {
        return database.settingQueries.getSetting(key)
            .executeAsOneOrNull()
    }

    private fun setSetting(key: String, value: String) {
        database.settingQueries.upsertSetting(
            key = key,
            value_ = value,
            updated_at = System.currentTimeMillis(),
        )
    }
}
