package com.ankideku.ui.screens.main.actions

import com.ankideku.data.remote.anki.AnkiConnectionMonitor
import kotlinx.coroutines.launch

interface UIActions {
    fun dismissToast()
    fun dismissDialog()
    fun retryConnection()
}

class UIActionsImpl(
    private val ctx: ViewModelContext,
    private val connectionMonitor: AnkiConnectionMonitor,
) : UIActions {

    override fun dismissToast() {
        ctx.update { copy(toastMessage = null) }
    }

    override fun dismissDialog() {
        ctx.update { copy(dialogState = null) }
    }

    override fun retryConnection() {
        ctx.scope.launch {
            connectionMonitor.checkNow()
        }
    }
}
