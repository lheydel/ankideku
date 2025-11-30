package com.ankideku.ui.screens.main.actions

import com.ankideku.ui.screens.main.MainUiState
import com.ankideku.ui.screens.main.ChatMessage
import com.ankideku.ui.screens.main.ChatMessageType
import com.ankideku.ui.screens.main.ToastMessage
import com.ankideku.ui.screens.main.ToastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared context for MainViewModel and all action implementations.
 */
class ViewModelContext(val scope: CoroutineScope) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val currentState: MainUiState get() = _uiState.value

    fun update(transform: MainUiState.() -> MainUiState) {
        _uiState.update { it.transform() }
    }

    fun showToast(message: String, type: ToastType) {
        update { copy(toastMessage = ToastMessage(message, type)) }
    }

    fun addChatMessage(content: String, type: ChatMessageType) {
        update { copy(chatMessages = chatMessages + ChatMessage(content = content, type = type)) }
    }
}
