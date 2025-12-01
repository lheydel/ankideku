package com.ankideku.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.domain.model.Deck

@Composable
fun DeckSelector(
    decks: List<Deck>,
    selectedDeck: Deck?,
    onDeckSelected: (Deck) -> Unit,
    onOpen: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    AppDropdown(
        items = decks,
        selectedItem = selectedDeck,
        onItemSelected = onDeckSelected,
        itemLabel = { it.name },
        placeholder = "Choose a deck...",
        enabled = enabled,
        onOpen = onOpen,
        modifier = modifier,
    )
}
