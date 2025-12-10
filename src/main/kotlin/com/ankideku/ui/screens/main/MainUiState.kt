package com.ankideku.ui.screens.main

import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.domain.model.ActionCall
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.domain.model.ReviewMessage
import com.ankideku.domain.model.ReviewMessageRole
import com.ankideku.domain.model.ReviewSuggestion
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionState
import com.ankideku.domain.model.Settings
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.usecase.suggestion.ConflictInfo

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

    // Batch Filter Mode
    val batchFilteredSuggestions: List<Suggestion>? = null,
    val batchQuery: SelQuery? = null,
    val isBatchProcessing: Boolean = false,
    val batchProgress: BatchProgress? = null,

    // Pre-session note browsing & filtering
    val deckNotes: List<Note> = emptyList(),
    val noteFilterQuery: SelQuery? = null,
    val filteredNotes: List<Note>? = null,
    val selectedNoteIndex: Int = 0,

    // History
    val historyEntries: List<HistoryEntry> = emptyList(),
    val historyViewMode: HistoryViewMode = HistoryViewMode.Session,
    val historySearchQuery: String = "",

    // Comparison view
    val selectedSuggestion: Suggestion? = null,
    val isEditMode: Boolean = false,  // Whether user is in edit mode (Edit/Done toggle)
    val hasManualEdits: Boolean = false,  // Whether edits have been saved to DB
    val editedFields: Map<String, String> = emptyMap(),
    val showOriginal: Boolean = false,
    val isActionLoading: Boolean = false,
    val viewingHistoryEntry: HistoryEntry? = null,

    // Sidebar / Chat
    val chatMessages: List<ChatMessage> = emptyList(),
    val reviewSessionState: ReviewSessionState = ReviewSessionState(),

    // Settings
    val settings: Settings = Settings(),
    val showSettingsDialog: Boolean = false,
    val settingsInitialNoteType: String? = null,  // When opening settings from note type button
    val llmHealthStatus: LlmHealthStatus? = null,
    val llmHealthChecking: Boolean = false,

    // Note Type Config
    val defaultDisplayFieldMap: Map<String, String> = emptyMap(),  // modelName -> fieldName
    val availableNoteTypes: List<String> = emptyList(),
    val noteTypeConfigs: Map<String, NoteTypeConfig> = emptyMap(),
    val noteTypeFields: Map<String, List<String>> = emptyMap(),

    // Session control
    val forceSyncBeforeStart: Boolean = false,

    // UI State
    val activeTab: QueueTab = QueueTab.Notes,
    val toastMessage: ToastMessage? = null,
    val dialogState: DialogState? = null,
    val isSidebarVisible: Boolean = true,
) {
    val currentSuggestion: Suggestion?
        get() = displayedSuggestions.getOrNull(currentSuggestionIndex)

    val isProcessing: Boolean
        get() = currentSession?.state == SessionState.Running

    val canStartSession: Boolean
        get() = selectedDeck != null && !isProcessing && ankiConnected

    val isInBatchFilterMode: Boolean
        get() = batchFilteredSuggestions != null

    /** Suggestions to display in queue - filtered if in batch mode, otherwise all */
    val displayedSuggestions: List<Suggestion>
        get() = batchFilteredSuggestions ?: suggestions

    /** Notes to display in pre-session mode - filtered if filter active, otherwise all deck notes */
    val displayedNotes: List<Note>
        get() = filteredNotes ?: deckNotes

    val hasNoteFilter: Boolean
        get() = noteFilterQuery != null

    val currentPreviewNote: Note?
        get() = displayedNotes.getOrNull(selectedNoteIndex)
}

data class SyncProgressUi(
    val deckName: String,
    val statusText: String,
    val step: Int = 0,
    val totalSteps: Int = 0,
)

enum class QueueTab { Notes, Queue, History }
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
        val onRefresh: () -> Unit,
        val onCancel: () -> Unit,
    ) : DialogState()

    data class Error(
        val title: String,
        val message: String,
        val onDismiss: () -> Unit,
    ) : DialogState()

    data class BatchConflict(
        val action: BatchAction,
        val conflicts: List<ConflictInfo>,
        val nonConflicting: List<Suggestion>,
    ) : DialogState()
}

enum class BatchAction { Accept, Reject }

data class BatchProgress(
    val current: Int,
    val total: Int,
)

// --- Review Session State ---

data class ReviewSessionState(
    val isActive: Boolean = false,
    val messages: List<ReviewChatMessage> = emptyList(),
    val pendingSuggestions: List<ReviewSuggestionUi> = emptyList(),
    val memory: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ReviewChatMessage(
    val id: Long,
    val role: ReviewChatRole,
    val content: String,
    val actionCalls: List<ReviewActionCallUi>? = null,
    val timestamp: Long,
)

enum class ReviewChatRole {
    User,
    Assistant,
    ActionResult,
}

data class ReviewActionCallUi(
    val id: String,
    val action: String,
    val params: Map<String, String>,
)

data class ReviewSuggestionUi(
    val id: Long,
    val suggestionId: Long,
    val proposedChanges: Map<String, String>,
    val proposedReasoning: String?,
)

// --- Mappers ---

fun ReviewMessage.toUi(): ReviewChatMessage = ReviewChatMessage(
    id = id,
    role = role.toUi(),
    content = content,
    actionCalls = actionCalls?.map { it.toUi() },
    timestamp = createdAt,
)

fun ReviewMessageRole.toUi(): ReviewChatRole = when (this) {
    ReviewMessageRole.User -> ReviewChatRole.User
    ReviewMessageRole.Assistant -> ReviewChatRole.Assistant
    ReviewMessageRole.ActionResult -> ReviewChatRole.ActionResult
}

fun ActionCall.toUi(): ReviewActionCallUi = ReviewActionCallUi(
    id = id,
    action = action,
    params = params,
)

fun ReviewSuggestion.toUi(): ReviewSuggestionUi = ReviewSuggestionUi(
    id = id,
    suggestionId = suggestionId,
    proposedChanges = proposedChanges,
    proposedReasoning = proposedReasoning,
)
