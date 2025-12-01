package com.ankideku.domain.usecase.settings

import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.util.onIO

class SettingsManager(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun get(): Settings = onIO { settingsRepository.getSettings() }

    suspend fun updateTheme(theme: AppTheme) = onIO { settingsRepository.updateTheme(theme) }

    suspend fun updateLlmProvider(provider: LlmProvider) = onIO { settingsRepository.updateLlmProvider(provider) }

    suspend fun update(settings: Settings) {
        onIO {
            settingsRepository.updateTheme(settings.theme)
            settingsRepository.updateLlmProvider(settings.llmProvider)
        }
    }
}
