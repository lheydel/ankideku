package com.ankideku.ui.screens.main.actions

import com.ankideku.data.remote.llm.LlmConfig
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.data.remote.llm.LlmServiceFactory
import com.ankideku.domain.model.Settings
import com.ankideku.domain.usecase.settings.SettingsManager
import kotlinx.coroutines.launch

interface SettingsActions {
    fun showSettingsDialog()
    fun hideSettingsDialog()
    fun openNoteTypeSettings(modelName: String)
    fun updateSettings(settings: Settings)
    fun toggleSidebar()
    fun testLlmConnection()
    fun setForceSyncBeforeStart(enabled: Boolean)
}

class SettingsActionsImpl(
    private val ctx: ViewModelContext,
    private val settingsManager: SettingsManager,
) : SettingsActions {

    override fun showSettingsDialog() {
        ctx.update { copy(showSettingsDialog = true, settingsInitialNoteType = null) }
    }

    override fun hideSettingsDialog() {
        ctx.update { copy(showSettingsDialog = false, settingsInitialNoteType = null) }
    }

    override fun openNoteTypeSettings(modelName: String) {
        ctx.update { copy(showSettingsDialog = true, settingsInitialNoteType = modelName) }
    }

    override fun updateSettings(settings: Settings) {
        val previousProvider = ctx.uiState.value.settings.llmProvider
        val providerChanged = previousProvider != settings.llmProvider

        ctx.scope.launch {
            settingsManager.update(settings)
            // Clear LLM instance if provider changed
            if (providerChanged) {
                LlmServiceFactory.clearInstance()
            }
            ctx.update {
                copy(
                    settings = settings,
                    llmHealthStatus = if (providerChanged) null else llmHealthStatus,
                )
            }
            // Trigger health check after settings are saved if provider changed
            if (providerChanged) {
                testLlmConnection()
            }
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
