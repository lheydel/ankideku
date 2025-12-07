package com.ankideku.data.local.repository

import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.util.parseJson
import com.ankideku.util.toJson

class SqlSettingsRepository(
    private val database: AnkiDekuDb,
) : SettingsRepository {

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_LLM_PROVIDER = "llm_provider"
    }

    override fun getSettings(): Settings {
        val theme = getSetting(KEY_THEME)?.let {
            try {
                AppTheme.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AppTheme.Dark
            }
        } ?: AppTheme.Dark

        val llmProvider = getSetting(KEY_LLM_PROVIDER)
            ?.parseJson<LlmProvider>()
            ?: LlmProvider.MOCK

        return Settings(
            theme = theme,
            llmProvider = llmProvider,
        )
    }

    override fun updateTheme(theme: AppTheme) {
        setSetting(KEY_THEME, theme.name)
    }

    override fun updateLlmProvider(provider: LlmProvider) {
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
