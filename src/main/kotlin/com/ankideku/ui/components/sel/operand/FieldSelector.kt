package com.ankideku.ui.components.sel.operand

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.theme.InputPadding
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Field selector with context dropdown and searchable grouped field name dropdown.
 */
@Composable
internal fun FieldSelector(
    operand: OperandState,
    entityType: EntityType,
    noteTypeFields: Map<String, List<String>>,
) {
    val colors = LocalAppColors.current
    val schema = SelEntityRegistry[entityType]
    val fieldContexts = schema.fieldContexts

    // If entity has no field access, show a disabled message
    if (fieldContexts.isEmpty()) {
        Text(
            "No field access for ${entityType.name}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
        return
    }

    // Set default context if not set
    if (operand.fieldContext == null && fieldContexts.isNotEmpty()) {
        operand.fieldContext = fieldContexts.first().selKey
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // Context dropdown
        AppDropdown(
            items = fieldContexts,
            selectedItem = fieldContexts.find { it.selKey == operand.fieldContext },
            onItemSelected = { ctx -> operand.fieldContext = ctx.selKey },
            itemLabel = { it.selKey },
            modifier = Modifier.width(100.dp),
        )

        // Searchable grouped field name dropdown
        GroupedFieldDropdown(
            noteTypeFields = noteTypeFields,
            selectedField = operand.fieldName,
            onFieldSelected = { operand.fieldName = it },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Searchable dropdown showing fields grouped by note type.
 */
@Composable
internal fun GroupedFieldDropdown(
    noteTypeFields: Map<String, List<String>>,
    selectedField: String,
    onFieldSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var triggerWidth by remember { mutableIntStateOf(0) }
    var triggerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val searchFocusRequester = remember { FocusRequester() }

    // Auto-focus search field when dropdown opens (delay ensures popup is composed)
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(50)
            searchFocusRequester.requestFocus()
        }
    }

    // Filter note types and fields by search query (case-insensitive)
    val filteredNoteTypes = remember(noteTypeFields, searchQuery) {
        if (searchQuery.isBlank()) {
            noteTypeFields
        } else {
            noteTypeFields.mapValues { (_, fields) ->
                fields.filter { it.contains(searchQuery, ignoreCase = true) }
            }.filterValues { it.isNotEmpty() }
        }
    }

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
                .clickable { expanded = !expanded },
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
                    text = selectedField.ifEmpty { "Select field..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedField.isNotEmpty()) colors.textPrimary else colors.textMuted,
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

        // Dropdown menu
        if (expanded) {
            val menuWidth = with(density) { triggerWidth.toDp() }
            Popup(
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
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
                    Column {
                        // Search input
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm)
                                .focusRequester(searchFocusRequester),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search fields...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textMuted,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        HorizontalDivider(color = colors.border)

                        // Grouped field list
                        Column(
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            if (filteredNoteTypes.isEmpty()) {
                                Text(
                                    "No fields found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(Spacing.sm),
                                )
                            } else {
                                filteredNoteTypes.forEach { (noteType, fields) ->
                                    // Note type header
                                    Text(
                                        text = noteType,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.surface)
                                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                    )

                                    // Fields for this note type
                                    fields.forEach { field ->
                                        val isSelected = field == selectedField
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    onFieldSelected(field)
                                                    expanded = false
                                                    searchQuery = ""
                                                }
                                                .background(
                                                    if (isSelected) colors.accent.copy(alpha = 0.1f)
                                                    else colors.surfaceAlt
                                                )
                                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                        ) {
                                            Text(
                                                text = field,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
