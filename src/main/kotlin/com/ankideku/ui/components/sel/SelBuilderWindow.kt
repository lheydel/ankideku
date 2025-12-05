package com.ankideku.ui.components.sel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.ankideku.domain.model.SelPreset
import com.ankideku.domain.repository.SelPresetRepository
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.session.SessionFinder
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.sel.common.SavePresetDialog
import com.ankideku.ui.components.sel.header.BreadcrumbBar
import com.ankideku.ui.components.sel.header.RootQueryHeader
import com.ankideku.ui.components.sel.header.SubqueryHeader
import com.ankideku.ui.components.sel.state.ScopeValue
import com.ankideku.ui.components.sel.state.SelBuilderState
import com.ankideku.ui.theme.AnkiDekuTheme
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.util.WindowStateManager
import com.ankideku.util.classpathPainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val WINDOW_STATE_KEY = "search-builder"

/**
 * Separate window for building SEL queries.
 *
 * @param onClose Called when the window is closed
 * @param onConfirm Called with the built query when confirmed
 * @param initialTarget Initial entity type to query
 * @param lockedScopes Scopes that are pre-filled and cannot be changed by the user
 */
@OptIn(FlowPreview::class)
@Composable
fun SelBuilderWindow(
    onClose: () -> Unit,
    onConfirm: (SelQuery) -> Unit,
    initialTarget: EntityType = EntityType.Note,
    lockedScopes: Map<String, ScopeValue> = emptyMap(),
) {
    val windowState = remember {
        WindowStateManager.loadOrDefault(
            key = WINDOW_STATE_KEY,
            defaultWidth = 800,
            defaultHeight = 700,
        )
    }

    Window(
        onCloseRequest = {
            WindowStateManager.save(windowState, WINDOW_STATE_KEY)
            onClose()
        },
        title = "Search Builder",
        icon = classpathPainterResource("icons/icon.png"),
        state = windowState,
    ) {
        window.minimumSize = java.awt.Dimension(600, 500)

        // Save window state when it changes (debounced)
        LaunchedEffect(windowState) {
            combine(
                snapshotFlow { windowState.position },
                snapshotFlow { windowState.size },
                snapshotFlow { windowState.placement }
            ) { position, size, placement -> Triple(position, size, placement) }
                .debounce(500)
                .onEach { WindowStateManager.save(windowState, WINDOW_STATE_KEY) }
                .launchIn(this)
        }

        AnkiDekuTheme {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                SelBuilderContent(
                    onClose = {
                        WindowStateManager.save(windowState, WINDOW_STATE_KEY)
                        onClose()
                    },
                    onConfirm = onConfirm,
                    initialTarget = initialTarget,
                    lockedScopes = lockedScopes,
                )
            }
        }
    }
}

@Composable
private fun SelBuilderContent(
    onClose: () -> Unit,
    onConfirm: (SelQuery) -> Unit,
    initialTarget: EntityType,
    lockedScopes: Map<String, ScopeValue>,
) {
    val colors = LocalAppColors.current
    val state = remember { SelBuilderState(initialTarget, lockedScopes) }
    val scope = rememberCoroutineScope()

    // Inject finders for scope data
    val deckFinder: DeckFinder = koinInject()
    val sessionFinder: SessionFinder = koinInject()
    val presetRepository: SelPresetRepository = koinInject()

    // Observe scope data
    val decks by deckFinder.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionFinder.observeAll().collectAsState(initial = emptyList())

    // Observe presets for current target
    val presets by presetRepository.getByTarget(state.target).collectAsState(initial = emptyList())

    // Fetch note type fields for field selector
    var noteTypeFields by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        val noteTypes = deckFinder.fetchNoteTypes()
        noteTypeFields = deckFinder.fetchAllNoteTypeFields(noteTypes)
    }

    // Save preset dialog state
    var showSaveDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Header with preset selector
            HeaderRow(
                state = state,
                presets = presets,
            )

            // Breadcrumb navigation (when in subquery)
            val isInSubquery = state.navigationStack.isNotEmpty()
            if (isInSubquery) {
                BreadcrumbBar(
                    breadcrumbs = state.breadcrumbs,
                    onNavigate = { index -> state.navigateTo(index) },
                )
            }

            // Context-sensitive header: root query vs subquery
            if (isInSubquery) {
                SubqueryHeader(subquery = state.currentSubquery!!)
            } else {
                RootQueryHeader(
                    state = state,
                    decks = decks,
                    sessions = sessions,
                )
            }

            HorizontalDivider(color = colors.border)

            // Scrollable condition builder area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SelBuilder(
                    state = state,
                    noteTypeFields = noteTypeFields,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(color = colors.border)

            // JSON Preview (collapsible)
            SelPreview(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )

            // Action buttons
            ActionButtonRow(
                state = state,
                isInSubquery = isInSubquery,
                onShowSaveDialog = { showSaveDialog = true },
                onReset = { state.reset() },
                onClose = onClose,
                onConfirm = { onConfirm(state.toSelQuery()) },
            )

            // Save preset dialog
            if (showSaveDialog) {
                SavePresetDialog(
                    onDismiss = { showSaveDialog = false },
                    onSave = { name, description ->
                        scope.launch(Dispatchers.IO) {
                            presetRepository.save(
                                SelPreset(
                                    name = name,
                                    description = description.takeIf { it.isNotBlank() },
                                    target = state.target,
                                    queryJson = state.toJson(),
                                )
                            )
                        }
                        showSaveDialog = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    state: SelBuilderState,
    presets: List<SelPreset>,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Search Builder",
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )

        // Preset selector (only at root level)
        if (state.navigationStack.isEmpty() && presets.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Load preset:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                AppDropdown(
                    items = presets,
                    selectedItem = null,
                    onItemSelected = { preset ->
                        state.loadFromJson(preset.queryJson)
                    },
                    itemLabel = { it.name },
                    placeholder = "Select...",
                    modifier = Modifier.width(180.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    state: SelBuilderState,
    isInSubquery: Boolean,
    onShowSaveDialog: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left side: Save preset (only at root level)
        if (!isInSubquery) {
                Text("Save as Preset")
            AppButton(onClick = onShowSaveDialog, variant = AppButtonVariant.Outlined) {
            }
        }

        Spacer(Modifier.weight(1f))

        // Right side: Reset, Cancel, Search
        AppButton(onClick = onReset, variant = AppButtonVariant.Text) {
            Text("Reset")
        }
        AppButton(onClick = onClose, variant = AppButtonVariant.Outlined) {
            Text("Cancel")
        }
        AccentButton(onClick = onConfirm) {
            Text("Search")
        }
    }
}
