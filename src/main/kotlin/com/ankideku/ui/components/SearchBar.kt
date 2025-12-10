package com.ankideku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.ast.SelNode
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.clickableWithPointer
import kotlinx.coroutines.delay

/**
 * A search bar with built-in debouncing, styled to match the app's visual design.
 *
 * The text field updates immediately for responsive UI, but the [onSearch] callback
 * is debounced to avoid excessive queries while typing.
 *
 * @param value The current search query (controlled externally)
 * @param onSearch Callback when search should be executed (debounced), receives query and scope
 * @param scope SelNode conditions to AND with the search (e.g., session filter)
 * @param placeholder Placeholder text
 * @param debounceMs Debounce delay in milliseconds (default 300ms)
 * @param modifier Modifier for the search bar
 */
@Composable
fun SearchBar(
    value: String,
    onSearch: (String, SelNode?) -> Unit,
    scope: SelNode? = null,
    placeholder: String = "Search...",
    debounceMs: Long = 300,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var focused by remember { mutableStateOf(false) }

    // Local state for immediate UI updates
    var localValue by remember { mutableStateOf(value) }

    // Sync local state when external value changes (e.g., cleared externally)
    LaunchedEffect(value) {
        if (value != localValue) {
            localValue = value
        }
    }

    // Debounce the search callback
    LaunchedEffect(localValue) {
        if (localValue != value) {
            delay(debounceMs)
            onSearch(localValue, scope)
        }
    }

    Surface(
        modifier = modifier,
        shape = InputShape,
        color = colors.surfaceAlt,
        border = BorderStroke(1.dp, if (focused) colors.accent else colors.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                .heightIn(min = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.textMuted,
            )
            Spacer(Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (localValue.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
                BasicTextField(
                    value = localValue,
                    onValueChange = { localValue = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            if (localValue.isNotEmpty()) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    modifier = Modifier
                        .size(16.dp)
                        .clickableWithPointer {
                            localValue = ""
                            onSearch("", scope)
                        },
                    tint = colors.textMuted,
                )
            }
        }
    }
}
