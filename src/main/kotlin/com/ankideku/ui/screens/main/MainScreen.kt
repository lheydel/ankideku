package com.ankideku.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelNumber
import com.ankideku.domain.sel.ast.SelOperation
import com.ankideku.domain.sel.ast.SelString
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.usecase.suggestion.BatchConflictStrategy
import com.ankideku.ui.components.AppDialogs
import com.ankideku.ui.components.comparison.ComparisonPanel
import com.ankideku.ui.components.ConnectionBanner
import com.ankideku.ui.components.queue.QueuePanel
import com.ankideku.ui.components.SessionSelector
import com.ankideku.ui.components.sidebar.SidebarPanel
import com.ankideku.ui.components.sel.SelBuilderWindow
import com.ankideku.ui.components.sel.state.ConditionGroupState
import com.ankideku.ui.components.sel.state.ConditionState
import com.ankideku.ui.components.sel.state.GroupItem
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.components.sel.state.OperandType
import com.ankideku.ui.components.sel.state.ScopeValue
import com.ankideku.ui.screens.settings.SettingsDialog
import com.ankideku.ui.screens.settings.SettingsTab
import com.ankideku.ui.theme.AnimationDurations
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.PanelSizes
import com.ankideku.ui.theme.Spacing
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    // State for batch filter builder window
    var showBatchFilterBuilder by remember { mutableStateOf(false) }

    // State for note filter builder window (pre-session filtering)
    var showNoteFilterBuilder by remember { mutableStateOf(false) }

    // Toast notification
    uiState.toastMessage?.let { toast ->
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(toast.duration)
            viewModel.dismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Connection warning banner
            ConnectionBanner(
                isConnected = uiState.ankiConnected,
                onRetry = viewModel::retryConnection,
            )

            // Responsive three-panel layout using BoxWithConstraints
            BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
                val availableWidth = maxWidth

                // Calculate responsive widths based on available space
                val queueWidth = (availableWidth * PanelSizes.queuePanelWeight)
                    .coerceIn(PanelSizes.queuePanelMinWidth, PanelSizes.queuePanelMaxWidth)
                val sidebarWidth = if (uiState.isSidebarVisible) {
                    (availableWidth * PanelSizes.sidebarWeight)
                        .coerceIn(PanelSizes.sidebarMinWidth, PanelSizes.sidebarMaxWidth)
                } else {
                    0.dp
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Queue Panel
                    QueuePanel(
                        suggestions = uiState.displayedSuggestions,
                        currentSuggestionIndex = uiState.currentSuggestionIndex,
                        historyEntries = uiState.historyEntries,
                        activeTab = uiState.activeTab,
                        currentSession = uiState.currentSession,
                        historySearchQuery = uiState.historySearchQuery,
                        historyViewMode = uiState.historyViewMode,
                        noteTypeConfigs = uiState.noteTypeConfigs,
                        // Queue search
                        queueSearchQuery = uiState.queueSearchQuery,
                        queueSearchScope = uiState.currentSession?.id?.let { sessionId ->
                            // Build scope: sessionId == X AND status == "pending"
                            SelOperation("and", SelArray(listOf(
                                SelOperation("==", SelArray(listOf(
                                    SelOperation("prop", SelArray(listOf(SelString("sessionId")))),
                                    SelNumber(sessionId),
                                ))),
                                SelOperation("==", SelArray(listOf(
                                    SelOperation("prop", SelArray(listOf(SelString("status")))),
                                    SelString("pending"),
                                ))),
                            )))
                        },
                        onQueueSearchChanged = viewModel::searchQueue,
                        // Batch filter mode
                        isInBatchFilterMode = uiState.isInBatchFilterMode,
                        isBatchProcessing = uiState.isBatchProcessing,
                        batchProgress = uiState.batchProgress,
                        // Pre-session note browsing
                        notes = uiState.displayedNotes,
                        selectedNoteIndex = uiState.selectedNoteIndex,
                        hasNoteFilter = uiState.hasNoteFilter,
                        onNoteClick = viewModel::selectNoteForPreview,
                        onOpenNoteFilter = { showNoteFilterBuilder = true },
                        onClearNoteFilter = viewModel::clearNoteFilter,
                        onTabChanged = viewModel::setActiveTab,
                        onHistoryViewModeChanged = viewModel::setHistoryViewMode,
                        onSuggestionClick = viewModel::selectSuggestion,
                        onHistoryClick = viewModel::viewHistoryEntry,
                        // Batch actions
                        onOpenBatchFilter = { showBatchFilterBuilder = true },
                        onClearBatchFilter = viewModel::clearBatchFilter,
                        onBatchAcceptAll = viewModel::batchAcceptAll,
                        onBatchRejectAll = viewModel::batchRejectAll,
                        onRefreshBaselines = viewModel::refreshSuggestionBaselines,
                        modifier = Modifier.width(queueWidth),
                    )

                    // Center: Session Selector or Comparison - fills remaining space
                    // Show ComparisonPanel when: viewing history, active session, or previewing a note
                    val showPreviewNote = uiState.currentSession == null && uiState.currentPreviewNote != null
                    if (uiState.currentSession != null || uiState.viewingHistoryEntry != null || showPreviewNote) {
                        ComparisonPanel(
                            suggestion = uiState.currentSuggestion,
                            session = uiState.currentSession,
                            suggestions = uiState.displayedSuggestions,
                            currentIndex = uiState.currentSuggestionIndex,
                            editedFields = uiState.editedFields,
                            isEditMode = uiState.isEditMode,
                            hasManualEdits = uiState.hasManualEdits,
                            showOriginal = uiState.showOriginal,
                            isActionLoading = uiState.isActionLoading,
                            isProcessing = uiState.isProcessing,
                            historyEntry = uiState.viewingHistoryEntry,
                            noteTypeConfigs = uiState.noteTypeConfigs,
                            // Pre-session note preview
                            previewNote = if (showPreviewNote) uiState.currentPreviewNote else null,
                            selectedDeck = uiState.selectedDeck,
                            onAccept = viewModel::acceptSuggestion,
                            onReject = viewModel::rejectSuggestion,
                            onSkip = viewModel::skipSuggestion,
                            onEditField = viewModel::editField,
                            onToggleEditMode = viewModel::toggleEditMode,
                            onToggleOriginal = viewModel::toggleOriginalView,
                            onBackToSessions = viewModel::clearSession,
                            onRevertEdits = viewModel::revertEdits,
                            onCloseHistoryView = viewModel::clearHistoryView,
                            onOpenNoteTypeSettings = viewModel::openNoteTypeSettings,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        SessionSelector(
                            sessions = uiState.sessions,
                            onLoadSession = viewModel::loadSession,
                            onDeleteSession = viewModel::deleteSession,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Right: Sidebar (AI Chat) - collapsible with slide animation
                    AnimatedVisibility(
                        visible = uiState.isSidebarVisible,
                        enter = slideInHorizontally(
                            animationSpec = tween(AnimationDurations.slideIn),
                            initialOffsetX = { it },
                        ) + fadeIn(animationSpec = tween(AnimationDurations.fadeIn)),
                        exit = slideOutHorizontally(
                            animationSpec = tween(AnimationDurations.slideIn),
                            targetOffsetX = { it },
                        ) + fadeOut(animationSpec = tween(AnimationDurations.fadeIn)),
                    ) {
                        SidebarPanel(
                            decks = uiState.decks,
                            selectedDeck = uiState.selectedDeck,
                            chatMessages = uiState.chatMessages,
                            isConnected = uiState.ankiConnected,
                            isSyncing = uiState.isSyncing,
                            syncProgress = uiState.syncProgress,
                            isProcessing = uiState.isProcessing,
                            canStartSession = uiState.canStartSession,
                            currentSession = uiState.currentSession,
                            forceSyncBeforeStart = uiState.forceSyncBeforeStart,
                            llmProvider = uiState.settings.llmProvider,
                            // Pre-session note filter
                            noteFilterCount = if (uiState.hasNoteFilter) uiState.displayedNotes.size else null,
                            totalNoteCount = uiState.deckNotes.size,
                            onDeckSelected = viewModel::selectDeck,
                            onRefreshDecks = viewModel::refreshDecks,
                            onSyncDeck = viewModel::syncDeck,
                            onStartSession = viewModel::startSession,
                            onCancelSession = viewModel::cancelSession,
                            onNewSession = viewModel::clearSession,
                            onDeleteSession = {
                                uiState.currentSession?.id?.let { viewModel.deleteSession(it) }
                            },
                            onForceSyncChanged = viewModel::setForceSyncBeforeStart,
                            onOpenNoteFilter = { showNoteFilterBuilder = true },
                            onClearNoteFilter = viewModel::clearNoteFilter,
                            onCloseSidebar = viewModel::toggleSidebar,
                            modifier = Modifier.width(sidebarWidth),
                        )
                    }
                }
            }
        }

        // Toast notification - top-right with slide-in animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            contentAlignment = Alignment.TopEnd,
        ) {
            val colors = LocalAppColors.current
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = slideInVertically(
                    animationSpec = tween(AnimationDurations.slideIn),
                    initialOffsetY = { -it },
                ) + fadeIn(animationSpec = tween(AnimationDurations.fadeIn)),
                exit = slideOutVertically(
                    animationSpec = tween(AnimationDurations.slideIn),
                    targetOffsetY = { -it },
                ) + fadeOut(animationSpec = tween(AnimationDurations.fadeIn)),
            ) {
                uiState.toastMessage?.let { toast ->
                    val (containerColor, contentColor, icon) = when (toast.type) {
                        ToastType.Success -> Triple(
                            colors.success,
                            colors.onAccent,
                            Icons.Default.Check,
                        )
                        ToastType.Error -> Triple(
                            colors.error,
                            colors.onAccent,
                            Icons.Default.Close,
                        )
                        ToastType.Warning -> Triple(
                            colors.warning,
                            colors.textPrimary,
                            Icons.Default.Warning,
                        )
                        ToastType.Info -> Triple(
                            MaterialTheme.colorScheme.primary,
                            colors.onAccent,
                            Icons.Default.Info,
                        )
                    }

                    Surface(
                        color = containerColor,
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp),
                            )
                            SelectionContainer {
                                Text(
                                    text = toast.message,
                                    color = contentColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        AppDialogs(
            dialogState = uiState.dialogState,
            onDismiss = viewModel::dismissDialog,
            onBatchConflictAction = { strategy ->
                val batchAction = (uiState.dialogState as? DialogState.BatchConflict)?.action
                if (batchAction != null) {
                    viewModel.confirmBatchWithStrategy(batchAction, strategy)
                }
            },
        )

        // Settings Dialog
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                settings = uiState.settings,
                llmHealthStatus = uiState.llmHealthStatus,
                llmHealthChecking = uiState.llmHealthChecking,
                availableNoteTypes = uiState.availableNoteTypes,
                noteTypeConfigs = uiState.noteTypeConfigs,
                noteTypeFields = uiState.noteTypeFields,
                initialTab = if (uiState.settingsInitialNoteType != null) SettingsTab.NOTE_TYPES else null,
                initialNoteType = uiState.settingsInitialNoteType,
                onDismiss = viewModel::hideSettingsDialog,
                onSave = viewModel::updateSettings,
                onSaveNoteTypeConfig = viewModel::saveNoteTypeConfig,
                onTestConnection = viewModel::testLlmConnection,
            )
        }

        // Batch Filter Builder Window
        if (showBatchFilterBuilder && uiState.currentSession != null) {
            // Create initial condition: status = pending
            val statusPendingCondition = ConditionState(initialOperator = "==").apply {
                operands = listOf(
                    OperandState(
                        initialType = OperandType.Property,
                        initialPropertyName = "status",
                    ),
                    OperandState(
                        initialType = OperandType.Value,
                        initialValue = "pending",
                    ),
                )
            }
            val initialConditions = ConditionGroupState().apply {
                items = listOf(GroupItem.Condition(statusPendingCondition))
            }

            SelBuilderWindow(
                onClose = { showBatchFilterBuilder = false },
                onConfirm = { query ->
                    viewModel.executeBatchQuery(query)
                },
                initialTarget = EntityType.Suggestion,
                lockedScopes = mapOf(
                    "session" to ScopeValue(
                        value = uiState.currentSession!!.id,
                        displayLabel = "Session #${uiState.currentSession!!.id}",
                        locked = true,
                    )
                ),
                initialConditions = initialConditions,
            )
        }

        // Note Filter Builder Window (pre-session filtering)
        if (showNoteFilterBuilder && uiState.selectedDeck != null && uiState.currentSession == null) {
            SelBuilderWindow(
                onClose = { showNoteFilterBuilder = false },
                onConfirm = { query ->
                    viewModel.executeNoteFilter(query)
                },
                initialTarget = EntityType.Note,
                lockedScopes = mapOf(
                    "deck" to ScopeValue(
                        value = uiState.selectedDeck!!.id,
                        displayLabel = uiState.selectedDeck!!.name,
                        locked = true,
                    )
                ),
            )
        }
    }
}
