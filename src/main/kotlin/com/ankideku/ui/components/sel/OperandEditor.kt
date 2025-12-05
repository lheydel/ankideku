package com.ankideku.ui.components.sel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.components.sel.state.*
import com.ankideku.ui.theme.*

/**
 * Compact chip displaying an operand's value for inline preview.
 */
@Composable
internal fun OperandChip(
    operand: OperandState?,
    entityType: EntityType,
    depth: Int,
) {
    val colors = LocalAppColors.current
    if (operand == null) return

    val accentColor = when (depth % 3) {
        0 -> colors.accent
        1 -> colors.secondary
        else -> colors.warning
    }

    when (operand.type) {
        OperandType.Field -> {
            InlineChip(
                text = if (operand.fieldContext != null)
                    "${operand.fieldName}[${operand.fieldContext}]"
                else
                    operand.fieldName.ifEmpty { "field" },
                color = colors.success,
                prefix = "field.",
            )
        }
        OperandType.Property -> {
            InlineChip(
                text = operand.propertyName.ifEmpty { "prop" },
                color = colors.secondary,
                prefix = "prop.",
            )
        }
        OperandType.Value -> {
            val displayValue = operand.value.ifEmpty { "\"\"" }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.warning.copy(alpha = 0.1f),
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.warning,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                )
            }
        }
        OperandType.Expression -> {
            val expr = operand.expression
            if (expr != null) {
                // Wrap entire expression in a bordered block
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Function name badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = accentColor.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = expr.operator,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 1.dp),
                            )
                        }

                        Text("(", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        // Inline args
                        expr.operands.forEachIndexed { index, arg ->
                            if (index > 0) {
                                Text(", ", color = colors.textMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            OperandChip(arg, entityType, depth + 1)
                        }

                        Text(")", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("expr()", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            }
        }
        OperandType.Subquery -> {
            InlineChip(text = "subquery", color = colors.secondary, prefix = "")
        }
    }
}

@Composable
private fun InlineChip(
    text: String,
    color: Color,
    prefix: String,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (prefix.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.6f),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

/**
 * Full operand editor with type selector and type-specific input.
 *
 * @param parentScopes Available parent scopes for property references (alias to EntityType)
 * @param onSubqueryClick Called when user clicks a subquery to edit it
 */
@Composable
internal fun OperandEditor(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    depth: Int,
    parentScopes: List<Pair<String, EntityType>> = emptyList(),
    noteTypeFields: Map<String, List<String>> = emptyMap(),
    onSubqueryClick: ((SubqueryState) -> Unit)? = null,
) {
    val colors = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // Type selector - more compact
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OperandType.entries.forEach { type ->
                val selected = operand.type == type
                Surface(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { operand.type = type },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) colors.accent.copy(alpha = 0.15f) else Color.Transparent,
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.accent else colors.textMuted,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                    )
                }
            }
        }

        // Type-specific compact input
        when (operand.type) {
            OperandType.Field -> {
                FieldSelector(
                    operand = operand,
                    entityType = entityType,
                    noteTypeFields = noteTypeFields,
                )
            }
            OperandType.Property -> {
                PropertySelector(
                    operand = operand,
                    entityType = entityType,
                    expectedType = expectedType,
                    parentScopes = parentScopes,
                )
            }
            OperandType.Value -> {
                AppTextInput(
                    value = operand.value,
                    onValueChange = { operand.value = it },
                    placeholder = "value",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OperandType.Expression -> {
                ExpressionEditor(operand, entityType, expectedType, depth, parentScopes, noteTypeFields, onSubqueryClick)
            }
            OperandType.Subquery -> {
                SubqueryChip(operand, expectedType, onSubqueryClick)
            }
        }
    }
}

/**
 * Field selector with context dropdown and searchable grouped field name dropdown.
 */
@Composable
private fun FieldSelector(
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
private fun GroupedFieldDropdown(
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
                                .padding(Spacing.sm),
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

/**
 * Property selector with optional scope selector for parent entity references.
 */
@Composable
private fun PropertySelector(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    parentScopes: List<Pair<String, EntityType>>,
) {
    // Build scope options: current entity + parent scopes
    val scopeOptions = buildList {
        add(null to entityType) // Current entity (null scope)
        addAll(parentScopes)
    }

    // Get selected scope's entity type
    val selectedScopeEntity = scopeOptions.find { it.first == operand.propertyScope }?.second ?: entityType

    // Get properties for selected scope
    val schema = SelEntityRegistry[selectedScopeEntity]
    val properties = schema.visiblePropertiesOfType(expectedType)

    if (parentScopes.isEmpty()) {
        // No parent scopes - just show property dropdown
        AppDropdown(
            items = properties,
            selectedItem = properties.find { it.selKey == operand.propertyName },
            onItemSelected = { prop -> operand.propertyName = prop.selKey },
            itemLabel = { prop -> prop.displayName ?: prop.selKey },
            placeholder = "Select property...",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        // Show scope selector + property dropdown
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Scope selector
            AppDropdown(
                items = scopeOptions,
                selectedItem = scopeOptions.find { it.first == operand.propertyScope },
                onItemSelected = { (scope, _) ->
                    operand.propertyScope = scope
                    operand.propertyName = "" // Reset property when scope changes
                },
                itemLabel = { (scope, entity) ->
                    if (scope == null) entity.name else "$scope (${entity.name})"
                },
                modifier = Modifier.width(140.dp),
            )

            // Property dropdown for selected scope
            AppDropdown(
                items = properties,
                selectedItem = properties.find { it.selKey == operand.propertyName },
                onItemSelected = { prop -> operand.propertyName = prop.selKey },
                itemLabel = { prop -> prop.displayName ?: prop.selKey },
                placeholder = "property...",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Clickable chip that shows subquery summary and navigates to edit it.
 *
 * @param expectedType The type expected by the parent operator, used to filter result types
 */
@Composable
private fun SubqueryChip(
    operand: OperandState,
    expectedType: SelType,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current

    // Filter result types based on expected type
    val availableResultTypes = remember(expectedType) {
        SubqueryResultType.entries.filter { resultType ->
            when (expectedType) {
                SelType.Any -> true // All types allowed
                SelType.Boolean -> resultType.returnType == SelType.Boolean
                SelType.Number -> resultType.returnType == SelType.Number || resultType.returnType == null
                SelType.String -> resultType.returnType == null // Only ScalarProperty can return string
                else -> resultType.returnType == expectedType || resultType.returnType == null
            }
        }
    }

    // Initialize subquery if not present, with appropriate default result type
    if (operand.subquery == null) {
        val defaultResultType = availableResultTypes.firstOrNull() ?: SubqueryResultType.Exists
        operand.subquery = SubqueryState(initialResultType = defaultResultType)
    }
    val subquery = operand.subquery!!

    // Ensure current result type is valid for expected type
    if (subquery.resultType !in availableResultTypes && availableResultTypes.isNotEmpty()) {
        subquery.resultType = availableResultTypes.first()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onSubqueryClick != null) {
                    Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onSubqueryClick(subquery) }
                } else Modifier
            ),
        shape = InputShape,
        color = colors.secondary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Result type dropdown (filtered by expected type)
            AppDropdown(
                items = availableResultTypes,
                selectedItem = subquery.resultType,
                onItemSelected = { subquery.resultType = it },
                itemLabel = { it.displayName },
                modifier = Modifier.width(120.dp),
            )

            // Target entity dropdown
            AppDropdown(
                items = listOf(EntityType.Note, EntityType.Suggestion),
                selectedItem = subquery.target,
                onItemSelected = { subquery.target = it },
                itemLabel = { it.name },
                modifier = Modifier.width(120.dp),
            )

            // Condition count indicator
            val conditionCount = subquery.rootGroup.items.size
            Text(
                text = "($conditionCount condition${if (conditionCount != 1) "s" else ""})",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )

            Spacer(Modifier.weight(1f))

            // Edit indicator
            if (onSubqueryClick != null) {
                Text(
                    text = "Edit →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.secondary,
                )
            }
        }
    }
}

/**
 * Editor for expression operands with nested operands.
 */
@Composable
private fun ExpressionEditor(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    depth: Int,
    parentScopes: List<Pair<String, EntityType>>,
    noteTypeFields: Map<String, List<String>>,
    onSubqueryClick: ((SubqueryState) -> Unit)?,
) {
    val colors = LocalAppColors.current

    if (operand.expression == null) {
        operand.expression = ExpressionState()
    }
    val expr = operand.expression!!

    val accentColor = when (depth % 3) {
        0 -> colors.accent
        1 -> colors.secondary
        else -> colors.warning
    }

    // Filter operators to those that return the expected type
    val filteredOperators = remember(expectedType) {
        SelOperatorRegistry.returningType(expectedType)
    }

    // Entire expression in a colored bordered block
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
        color = accentColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Function selector row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Function badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = expr.operator,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }

                AppDropdown(
                    items = filteredOperators,
                    selectedItem = SelOperatorRegistry[expr.operator],
                    onItemSelected = { op ->
                        expr.operator = op.key
                        expr.setOperandCount(op.metadata.signature.minArgs)
                    },
                    itemLabel = { it.metadata.displayName },
                    placeholder = "fn",
                    modifier = Modifier.width(100.dp),
                )
            }

            // Get the expression operator's signature to know what types its args expect
            val exprSignature = expr.operatorMetadata?.signature

            // Arguments in bordered blocks - horizontal if 2 or less
            if (expr.operands.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    expr.operands.forEachIndexed { index, arg ->
                        val argExpectedType = exprSignature?.argTypeAt(index) ?: SelType.Any

                        // Each arg in its own block
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = InputShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Text(
                                    "arg ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor,
                                )
                                OperandEditor(arg, entityType, argExpectedType, depth + 1, parentScopes, noteTypeFields, onSubqueryClick)
                            }
                        }
                    }
                }
            } else {
                // Vertical layout for 3+ args
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    expr.operands.forEachIndexed { index, arg ->
                        val argExpectedType = exprSignature?.argTypeAt(index) ?: SelType.Any

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = InputShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Text(
                                    "arg ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor,
                                )
                                OperandEditor(arg, entityType, argExpectedType, depth + 1, parentScopes, noteTypeFields, onSubqueryClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small clickable text button for adding items.
 */
@Composable
internal fun AddButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
    )
}
