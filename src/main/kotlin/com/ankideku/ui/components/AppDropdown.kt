package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.ankideku.ui.theme.InputPadding
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors

/**
 * Generic dropdown component for consistent styling across the app.
 *
 * @param items List of items to display
 * @param selectedItem Currently selected item (null for placeholder)
 * @param onItemSelected Callback when an item is selected
 * @param itemLabel Function to get display text for an item
 * @param placeholder Text to show when no item is selected
 * @param enabled Whether the dropdown is interactive
 * @param onOpen Optional callback when dropdown is opened (e.g., to refresh data)
 */
@Composable
fun <T> AppDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    placeholder: String = "Select...",
    enabled: Boolean = true,
    onOpen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var triggerWidth by remember { mutableIntStateOf(0) }
    var triggerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val displayText = selectedItem?.let { itemLabel(it) } ?: placeholder

    Box(modifier = modifier) {
        // Trigger
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    triggerWidth = it.size.width
                    triggerHeight = it.size.height
                }
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .clickable(enabled = enabled) {
                    if (!expanded) onOpen?.invoke()
                    expanded = !expanded
                },
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
                    color = if (selectedItem != null) colors.textPrimary else colors.textMuted,
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

        // Menu
        if (expanded) {
            val menuWidth = with(density) { triggerWidth.toDp() }
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
                offset = IntOffset(0, triggerHeight),
            ) {
                Surface(
                    modifier = Modifier.width(menuWidth),
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
                        items.forEach { item ->
                            val isSelected = item == selectedItem
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        onItemSelected(item)
                                        expanded = false
                                    }
                                    .background(
                                        if (isSelected) colors.accent.copy(alpha = 0.1f)
                                        else colors.surfaceAlt
                                    )
                                    .padding(InputPadding),
                            ) {
                                Text(
                                    text = itemLabel(item),
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
    }
}
