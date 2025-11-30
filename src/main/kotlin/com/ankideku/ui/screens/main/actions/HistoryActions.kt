package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.usecase.history.HistoryFinder
import com.ankideku.ui.screens.main.HistoryViewMode
import com.ankideku.ui.screens.main.QueueTab
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.launch

interface HistoryActions {
    fun setActiveTab(tab: QueueTab)
    fun setHistoryViewMode(mode: HistoryViewMode)
}

class HistoryActionsImpl(
    private val ctx: ViewModelContext,
    private val historyFinder: HistoryFinder,
) : HistoryActions {

    override fun setActiveTab(tab: QueueTab) {
        ctx.update { copy(activeTab = tab) }
        if (tab == QueueTab.History) {
            loadHistory()
        }
    }

    override fun setHistoryViewMode(mode: HistoryViewMode) {
        ctx.update { copy(historyViewMode = mode) }
        loadHistory()
    }

    private fun loadHistory() {
        ctx.scope.launch {
            try {
                val sessionId = ctx.currentState.currentSession?.id
                val history = if (ctx.currentState.historyViewMode == HistoryViewMode.Session && sessionId != null) {
                    historyFinder.getForSession(sessionId)
                } else {
                    historyFinder.getAll()
                }
                ctx.update { copy(historyEntries = history) }
            } catch (e: Exception) {
                ctx.showToast("Failed to load history: ${e.message}", ToastType.Error)
            }
        }
    }
}
