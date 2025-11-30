package com.ankideku.ui.screens.main.actions

import com.ankideku.data.remote.llm.LlmConfig
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.data.remote.llm.LlmServiceFactory
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings
import com.ankideku.domain.usecase.settings.SettingsManager
import kotlinx.coroutines.launch

interface SettingsActions {
    fun showSettingsDialog()
    fun hideSettingsDialog()
    fun updateSettings(settings: Settings)
    fun toggleTheme()
    fun toggleSidebar()
    fun testLlmConnection()
    fun setForceSyncBeforeStart(enabled: Boolean)
}

class SettingsActionsImpl(
    private val ctx: ViewModelContext,
    private val settingsManager: SettingsManager,
) : SettingsActions {

    override fun showSettingsDialog() {
        ctx.update { copy(showSettingsDialog = true) }
    }

    override fun hideSettingsDialog() {
        ctx.update { copy(showSettingsDialog = false) }
    }

    override fun updateSettings(settings: Settings) {
        ctx.scope.launch {
            settingsManager.update(settings)
            // Clear LLM instance if provider changed
            LlmServiceFactory.clearInstance()
            ctx.update {
                copy(
                    settings = settings,
                    llmHealthStatus = null, // Reset health status when settings change
                )
            }
        }
    }

    override fun toggleTheme() {
        ctx.scope.launch {
            val currentTheme = ctx.uiState.value.settings.theme
            val newTheme = when (currentTheme) {
                AppTheme.Light -> AppTheme.Dark
                AppTheme.Dark -> AppTheme.System
                AppTheme.System -> AppTheme.Light
            }
            val newSettings = ctx.uiState.value.settings.copy(theme = newTheme)
            settingsManager.update(newSettings)
            ctx.update { copy(settings = newSettings) }
        }
    }

    override fun toggleSidebar() {
        ctx.update { copy(isSidebarVisible = !isSidebarVisible) }
    }

    override fun testLlmConnection() {
        ctx.scope.launch {
            ctx.update { copy(llmHealthChecking = true) }
            try {
                val config = LlmConfig(provider = ctx.uiState.value.settings.llmProvider)
                val llmService = LlmServiceFactory.getInstance(config)
                val healthStatus = llmService.getHealth()
                ctx.update { copy(llmHealthStatus = healthStatus, llmHealthChecking = false) }
            } catch (e: Exception) {
                ctx.update {
                    copy(
                        llmHealthStatus = LlmHealthStatus(
                            available = false,
                            error = e.message ?: "Connection failed"
                        ),
                        llmHealthChecking = false
                    )
                }
            }
        }
    }

    override fun setForceSyncBeforeStart(enabled: Boolean) {
        ctx.update { copy(forceSyncBeforeStart = enabled) }
    }
}
