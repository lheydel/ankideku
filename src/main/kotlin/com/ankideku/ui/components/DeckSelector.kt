package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ankideku.domain.model.Deck
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.InputPadding
import com.ankideku.ui.theme.InputShape

@Composable
fun DeckSelector(
    decks: List<Deck>,
    selectedDeck: Deck?,
    onDeckSelected: (Deck) -> Unit,
    enabled: Boolean,
    colors: AppColorScheme,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerWidth by remember { mutableIntStateOf(0) }
    var triggerHeight by remember { mutableIntStateOf(0) }
    val displayText = selectedDeck?.name ?: "Choose a deck..."
    val density = LocalDensity.current

    Box(modifier = modifier) {
        // Trigger
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    triggerWidth = it.size.width
                    triggerHeight = it.size.height
                }
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(enabled = enabled) { expanded = !expanded },
            shape = InputShape,
            color = colors.surfaceAlt,
            border = BorderStroke(1.dp, if (expanded) colors.accent else colors.border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(InputPadding)
                    .height(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedDeck != null) colors.textPrimary else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.textMuted,
                )
            }
        }

        val menuWidth = with(density) { triggerWidth.toDp() }
        DeckSelectorMenu(
            open = expanded,
            decks = decks,
            selectedDeck = selectedDeck,
            onDeckSelected = { deck ->
                onDeckSelected(deck)
                expanded = false
            },
            colors = colors,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
            offsetY = triggerHeight,
        )
    }
}

@Composable
private fun DeckSelectorMenu(
    open: Boolean,
    decks: List<Deck>,
    selectedDeck: Deck?,
    onDeckSelected: (Deck) -> Unit,
    colors: AppColorScheme,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offsetY: Int = 0,
) {
    if (!open) return

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        offset = IntOffset(0, offsetY),
    ) {
        Surface(
            modifier = modifier,
            shape = InputShape,
            color = colors.surfaceAlt,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                decks.forEach { deck ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onDeckSelected(deck) }
                            .background(
                                if (deck == selectedDeck) colors.accent.copy(alpha = 0.1f)
                                else colors.surfaceAlt
                            )
                            .padding(InputPadding),
                    ) {
                        Text(
                            text = deck.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
