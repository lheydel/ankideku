package com.ankideku.ui.components.sel.header

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.Deck
import com.ankideku.domain.model.Session
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.domain.sel.schema.ScopeType
import com.ankideku.domain.sel.schema.EntityScope
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.sel.common.LabeledTextInput
import com.ankideku.ui.components.sel.common.OrderBySelector
import com.ankideku.ui.components.sel.state.ScopeValue
import com.ankideku.ui.components.sel.state.SelBuilderState
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Header for root query showing target entity, scope selectors, order, and limit.
 */
@Composable
internal fun RootQueryHeader(
    state: SelBuilderState,
    decks: List<Deck>,
    sessions: List<Session>,
) {
    val colors = LocalAppColors.current
    val schema = SelEntityRegistry[state.target]

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
        OrderBySelector(
            properties = schema.visibleProperties,
            orderBy = state.orderBy,
            onOrderByChange = { state.orderBy = it },
        )

        // Limit input
        LabeledTextInput(
            label = "Limit",
            value = state.limit?.toString() ?: "",
            onValueChange = { state.limit = it.replace(Regex("[^0-9]"), "").toIntOrNull() },
            placeholder = "∞",
            inputWidth = 80.dp,
        )
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
