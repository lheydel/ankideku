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
import com.ankideku.ui.components.AppDialogs
import com.ankideku.ui.components.ComparisonPanel
import com.ankideku.ui.components.ConnectionBanner
import com.ankideku.ui.components.Header
import com.ankideku.ui.components.QueuePanel
import com.ankideku.ui.components.SessionSelector
import com.ankideku.ui.components.SidebarPanel
import com.ankideku.ui.screens.settings.SettingsDialog
import com.ankideku.ui.theme.AnimationDurations
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.PanelSizes
import com.ankideku.ui.theme.Spacing
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Toast notification
    uiState.toastMessage?.let { toast ->
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(toast.duration)
            viewModel.dismissToast()
        }
    }

    // Determine if dark theme is active
    val isDarkTheme = when (uiState.settings.theme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System -> isSystemInDarkTheme()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Header(
                isConnected = uiState.ankiConnected,
                onThemeToggle = viewModel::toggleTheme,
                onSettingsClick = viewModel::showSettingsDialog,
                onSidebarToggle = viewModel::toggleSidebar,
                isSidebarVisible = uiState.isSidebarVisible,
                isDarkTheme = isDarkTheme,
            )

            // Connection warning banner
            ConnectionBanner(
                isConnected = uiState.ankiConnected,
                onRetry = viewModel::retryConnection,
            )

            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                // Left: Queue Panel
                QueuePanel(
                    suggestions = uiState.suggestions,
                    currentSuggestionIndex = uiState.currentSuggestionIndex,
                    historyEntries = uiState.historyEntries,
                    activeTab = uiState.activeTab,
                    currentSession = uiState.currentSession,
                    historySearchQuery = uiState.historySearchQuery,
                    historyViewMode = uiState.historyViewMode,
                    onTabChanged = viewModel::setActiveTab,
                    onHistoryViewModeChanged = viewModel::setHistoryViewMode,
                    modifier = Modifier.width(PanelSizes.queuePanelWidth),
                )

                // Center: Session Selector or Comparison
                if (uiState.currentSession != null) {
                    ComparisonPanel(
                        suggestion = uiState.currentSuggestion,
                        session = uiState.currentSession,
                        suggestions = uiState.suggestions,
                        currentIndex = uiState.currentSuggestionIndex,
                        editedFields = uiState.editedFields,
                        isEditing = uiState.isEditing,
                        showOriginal = uiState.showOriginal,
                        isActionLoading = uiState.isActionLoading,
                        isProcessing = uiState.isProcessing,
                        onAccept = viewModel::acceptSuggestion,
                        onReject = viewModel::rejectSuggestion,
                        onSkip = viewModel::skipSuggestion,
                        onEditField = viewModel::editField,
                        onToggleOriginal = viewModel::toggleOriginalView,
                        onBackToSessions = viewModel::clearSession,
                        onRevertEdits = viewModel::revertEdits,
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

                // Right: Sidebar (AI Chat) - collapsible, slides from right
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
                        onDeckSelected = viewModel::selectDeck,
                        onSyncDeck = viewModel::syncDeck,
                        onStartSession = viewModel::startSession,
                        onCancelSession = viewModel::cancelSession,
                        onNewSession = viewModel::clearSession,
                        onDeleteSession = {
                            uiState.currentSession?.id?.let { viewModel.deleteSession(it) }
                        },
                        onForceSyncChanged = viewModel::setForceSyncBeforeStart,
                        onCloseSidebar = viewModel::toggleSidebar,
                        modifier = Modifier.width(PanelSizes.sidebarWidth),
                    )
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

        // Dialogs
        AppDialogs(
            dialogState = uiState.dialogState,
            onDismiss = viewModel::dismissDialog,
        )

        // Settings Dialog
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                settings = uiState.settings,
                suggestions = uiState.suggestions,
                llmHealthStatus = uiState.llmHealthStatus,
                llmHealthChecking = uiState.llmHealthChecking,
                onDismiss = viewModel::hideSettingsDialog,
                onSave = viewModel::updateSettings,
                onTestConnection = viewModel::testLlmConnection,
            )
        }
    }
}
