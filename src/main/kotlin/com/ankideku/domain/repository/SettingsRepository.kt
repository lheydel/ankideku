package com.ankideku.domain.repository

import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings

/**
 * Repository for app settings.
 * Methods are blocking - caller is responsible for dispatcher management via TransactionService.
 */
interface SettingsRepository {
    fun getSettings(): Settings
    fun updateFieldDisplayConfig(config: Map<String, String>)
    fun updateTheme(theme: AppTheme)
    fun updateLlmProvider(provider: LlmProvider)
}
