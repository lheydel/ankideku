package com.ankideku.ui.components.sel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Session
import com.ankideku.domain.sel.ast.SelOrderClause
import com.ankideku.domain.sel.ast.SelOrderDirection
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.EntityScope
import com.ankideku.domain.sel.schema.ScopeType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.sel.state.BreadcrumbEntry
import com.ankideku.ui.components.sel.state.ScopeValue
import com.ankideku.ui.components.sel.state.SelBuilderState
import com.ankideku.ui.components.sel.state.SubqueryResultType
import com.ankideku.ui.components.sel.state.SubqueryState
import com.ankideku.ui.theme.*
import com.ankideku.util.WindowStateManager
import com.ankideku.util.classpathPainterResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.compose.koinInject

/**
 * Separate window for building SEL queries.
 *
 * @param onClose Called when the window is closed
 * @param onConfirm Called with the built query when confirmed
 * @param initialTarget Initial entity type to query
 * @param lockedScopes Scopes that are pre-filled and cannot be changed by the user
 */
private const val WINDOW_STATE_KEY = "search-builder"

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

    // Inject finders for scope data
    val deckFinder: DeckFinder = koinInject()
    val sessionFinder: SessionFinder = koinInject()

    // Observe scope data
    val decks by deckFinder.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionFinder.observeAll().collectAsState(initial = emptyList())

    // Fetch note type fields for field selector
    var noteTypeFields by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        val noteTypes = deckFinder.fetchNoteTypes()
        noteTypeFields = deckFinder.fetchAllNoteTypeFields(noteTypes)
    }

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
            // Header
            Text(
                "Search Builder",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            ) {
                TextButton(onClick = { state.reset() }) {
                    Text("Reset")
                }
                OutlinedButton(onClick = onClose) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(state.toSelQuery()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                    ),
                ) {
                    Text("Search")
                }
            }
        }
    }
}

/**
 * Scope selector dropdown that shows options from the database.
 * Locked scopes are shown as read-only chips.
 */
@Composable
private fun ScopeSelector(
    scope: EntityScope,
    state: SelBuilderState,
    decks: List<Deck>,
    sessions: List<Session>,
) {
    val colors = LocalAppColors.current
    val scopeValue = state.scopes[scope.key]
    val isLocked = scopeValue?.locked == true

    Text(
        "${scope.displayName}:",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textMuted,
    )

    if (isLocked) {
        // Show locked scope as read-only chip
        Surface(
            shape = InputShape,
            color = colors.surfaceAlt,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Text(
                text = scopeValue.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    } else {
        // Show scope selector dropdown
        when (scope.type) {
            ScopeType.Deck -> {
                val selectedDeck = decks.find { it.id == (scopeValue?.value as? Long) }
                AppDropdown(
                    items = listOf(null) + decks,
                    selectedItem = selectedDeck,
                    onItemSelected = { deck ->
                        state.scopes = if (deck != null) {
                            state.scopes + (scope.key to ScopeValue(deck.id, deck.name))
                        } else {
                            state.scopes - scope.key
                        }
                    },
                    itemLabel = { it?.name ?: "All" },
                    modifier = Modifier.width(180.dp),
                )
            }
            ScopeType.Session -> {
                val selectedSession = sessions.find { it.id == (scopeValue?.value as? Long) }
                AppDropdown(
                    items = listOf(null) + sessions,
                    selectedItem = selectedSession,
                    onItemSelected = { session ->
                        state.scopes = if (session != null) {
                            state.scopes + (scope.key to ScopeValue(session.id, "Session #${session.id}"))
                        } else {
                            state.scopes - scope.key
                        }
                    },
                    itemLabel = { it?.let { s -> "Session #${s.id}" } ?: "All" },
                    modifier = Modifier.width(180.dp),
                )
            }
        }
    }
}

/**
 * Header for root query showing target entity and scope selectors.
 */
@Composable
private fun RootQueryHeader(
    state: SelBuilderState,
    decks: List<Deck>,
    sessions: List<Session>,
) {
    val colors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            "Search in:",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )
        AppDropdown(
            items = listOf(EntityType.Note, EntityType.Suggestion),
            selectedItem = state.target,
            onItemSelected = { state.target = it },
            itemLabel = { it.name },
            modifier = Modifier.width(180.dp),
        )

        // Scope selectors based on current target entity
        val schema = SelEntityRegistry[state.target]
        schema.scopes.forEach { scope ->
            ScopeSelector(
                scope = scope,
                state = state,
                decks = decks,
                sessions = sessions,
            )
        }

        Spacer(Modifier.weight(1f))

        // Order by
        val currentOrderField = state.orderBy.firstOrNull()?.field
        val currentOrderDir = state.orderBy.firstOrNull()?.direction ?: SelOrderDirection.Asc
        val selectedOrderProp = schema.visibleProperties.find { it.selKey == currentOrderField }

        Text(
            "Order:",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )
        AppDropdown(
            items = listOf(null) + schema.visibleProperties,
            selectedItem = selectedOrderProp,
            onItemSelected = { prop ->
                state.orderBy = if (prop != null) {
                    listOf(SelOrderClause(prop.selKey, currentOrderDir))
                } else {
                    emptyList()
                }
            },
            itemLabel = { it?.displayName ?: "None" },
            modifier = Modifier.width(120.dp),
        )
        if (currentOrderField != null) {
            AppDropdown(
                items = SelOrderDirection.entries.toList(),
                selectedItem = currentOrderDir,
                onItemSelected = { dir ->
                    state.orderBy = listOf(SelOrderClause(currentOrderField, dir))
                },
                itemLabel = { it.name },
                modifier = Modifier.width(80.dp),
            )
        }

        // Limit input
        Text(
            "Limit:",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )
        AppTextInput(
            value = state.limit?.toString() ?: "",
            onValueChange = { state.limit = it.replace(Regex("[^0-9]"), "").toIntOrNull() },
            placeholder = "∞",
            modifier = Modifier.width(80.dp),
        )
    }
}

/**
 * Header for subquery showing result type, target, and limit.
 * Wrapped in a colored surface to visually distinguish from root query.
 */
@Composable
private fun SubqueryHeader(
    subquery: SubqueryState,
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = colors.secondary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Result type dropdown
            Text(
                "Return:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppDropdown(
                items = SubqueryResultType.entries.toList(),
                selectedItem = subquery.resultType,
                onItemSelected = { subquery.resultType = it },
                itemLabel = { it.displayName },
                modifier = Modifier.width(140.dp),
            )

            // Target entity dropdown
            Text(
                "from:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppDropdown(
                items = listOf(EntityType.Note, EntityType.Suggestion, EntityType.HistoryEntry),
                selectedItem = subquery.target,
                onItemSelected = { subquery.target = it },
                itemLabel = { it.name },
                modifier = Modifier.width(140.dp),
            )

            // Result property (for scalar result type)
            if (subquery.resultType == SubqueryResultType.ScalarProperty) {
                Text(
                    "property:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondary,
                )
                AppTextInput(
                    value = subquery.resultProperty,
                    onValueChange = { subquery.resultProperty = it },
                    placeholder = "property name",
                    modifier = Modifier.width(140.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            // Order by
            val schema = SelEntityRegistry[subquery.target]
            val currentOrderField = subquery.orderBy.firstOrNull()?.field
            val currentOrderDir = subquery.orderBy.firstOrNull()?.direction ?: SelOrderDirection.Asc
            val selectedOrderProp = schema.visibleProperties.find { it.selKey == currentOrderField }

            Text(
                "Order:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppDropdown(
                items = listOf(null) + schema.visibleProperties,
                selectedItem = selectedOrderProp,
                onItemSelected = { prop ->
                    subquery.orderBy = if (prop != null) {
                        listOf(SelOrderClause(prop.selKey, currentOrderDir))
                    } else {
                        emptyList()
                    }
                },
                itemLabel = { it?.displayName ?: "None" },
                modifier = Modifier.width(120.dp),
            )
            if (currentOrderField != null) {
                AppDropdown(
                    items = SelOrderDirection.entries.toList(),
                    selectedItem = currentOrderDir,
                    onItemSelected = { dir ->
                        subquery.orderBy = listOf(SelOrderClause(currentOrderField, dir))
                    },
                    itemLabel = { it.name },
                    modifier = Modifier.width(80.dp),
                )
            }

            // Limit input
            Text(
                "Limit:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary,
            )
            AppTextInput(
                value = subquery.limit?.toString() ?: "",
                onValueChange = { subquery.limit = it.replace(Regex("[^0-9]"), "").toIntOrNull() },
                placeholder = "∞",
                modifier = Modifier.width(80.dp),
            )
        }
    }
}

/**
 * Breadcrumb navigation bar showing path to current subquery.
 */
@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbEntry>,
    onNavigate: (Int) -> Unit,
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = colors.surfaceAlt,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            breadcrumbs.forEachIndexed { index, entry ->
                if (index > 0) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }

                val isLast = index == breadcrumbs.lastIndex
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLast) FontWeight.Medium else FontWeight.Normal,
                    color = if (isLast) colors.textPrimary else colors.accent,
                    modifier = if (!isLast) {
                        Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onNavigate(index) }
                    } else Modifier,
                )
            }
        }
    }
}
