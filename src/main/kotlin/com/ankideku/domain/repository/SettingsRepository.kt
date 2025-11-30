package com.ankideku.domain.repository

import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings

interface SettingsRepository {
    suspend fun getSettings(): Settings
    suspend fun updateFieldDisplayConfig(config: Map<String, String>)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateLlmProvider(provider: LlmProvider)
}
