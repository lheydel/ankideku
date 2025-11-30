package com.ankideku.ui.screens.main

import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.domain.model.*

data class MainUiState(
    // Connection
    val ankiConnected: Boolean = true,
    val ankiError: String? = null,

    // Decks
    val decks: List<Deck> = emptyList(),
    val selectedDeck: Deck? = null,
    val isSyncing: Boolean = false,
    val syncProgress: SyncProgressUi? = null,

    // Session
    val currentSession: Session? = null,
    val sessions: List<Session> = emptyList(),
    val sessionsLoading: Boolean = false,

    // Queue
    val suggestions: List<Suggestion> = emptyList(),
    val currentSuggestionIndex: Int = 0,
    val queueSearchQuery: String = "",

    // History
    val historyEntries: List<HistoryEntry> = emptyList(),
    val historyViewMode: HistoryViewMode = HistoryViewMode.Session,
    val historySearchQuery: String = "",

    // Comparison view
    val selectedSuggestion: Suggestion? = null,
    val isEditing: Boolean = false,
    val editedFields: Map<String, String> = emptyMap(),
    val showOriginal: Boolean = false,
    val isActionLoading: Boolean = false,

    // Sidebar / Chat
    val chatMessages: List<ChatMessage> = emptyList(),

    // Settings
    val settings: Settings = Settings(),
    val showSettingsDialog: Boolean = false,
    val llmHealthStatus: LlmHealthStatus? = null,
    val llmHealthChecking: Boolean = false,

    // Session control
    val forceSyncBeforeStart: Boolean = false,

    // UI State
    val activeTab: QueueTab = QueueTab.Queue,
    val toastMessage: ToastMessage? = null,
    val dialogState: DialogState? = null,
    val isSidebarVisible: Boolean = true,
) {
    val currentSuggestion: Suggestion?
        get() = suggestions.getOrNull(currentSuggestionIndex)

    val pendingSuggestions: List<Suggestion>
        get() = suggestions.filter { it.status == SuggestionStatus.Pending }

    val isProcessing: Boolean
        get() = currentSession?.state == SessionState.Running

    val canStartSession: Boolean
        get() = selectedDeck != null && !isProcessing && ankiConnected
}

data class SyncProgressUi(
    val deckName: String,
    val statusText: String,
    val step: Int = 0,
    val totalSteps: Int = 0,
)

enum class QueueTab { Queue, History }
enum class HistoryViewMode { Session, Global }

data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.Info,
    val duration: Long = 3000,
)

enum class ToastType { Info, Success, Warning, Error }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val type: ChatMessageType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ChatMessageType {
    UserPrompt,
    SystemInfo,
    SessionResult,
    Error,
}

sealed class DialogState {
    data class Confirm(
        val title: String,
        val message: String,
        val confirmLabel: String = "Confirm",
        val onConfirm: () -> Unit,
    ) : DialogState()

    data class Conflict(
        val suggestion: Suggestion,
        val currentFields: Map<String, NoteField>,
        val onUseAi: () -> Unit,
        val onUseCurrent: () -> Unit,
        val onCancel: () -> Unit,
    ) : DialogState()

    data class Error(
        val title: String,
        val message: String,
        val onDismiss: () -> Unit,
    ) : DialogState()
}
