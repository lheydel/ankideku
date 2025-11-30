package com.ankideku.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.ankideku.ui.theme.InputShape
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun ComparisonPanel(
    suggestion: Suggestion?,
    session: Session?,
    suggestions: List<Suggestion>,
    currentIndex: Int,
    editedFields: Map<String, String>,
    isEditing: Boolean,
    showOriginal: Boolean,
    isActionLoading: Boolean,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    onEditField: (String, String) -> Unit,
    onToggleOriginal: () -> Unit,
    onBackToSessions: () -> Unit,
    onRevertEdits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
        ) {
            // Breadcrumb navigation
            Breadcrumb(
                onBackToSessions = onBackToSessions,
            )

            Spacer(Modifier.height(Spacing.sm))

            if (suggestion == null) {
                EmptyState(
                    isProcessing = isProcessing,
                    processedCards = session?.progress?.processedCards ?: 0,
                    totalCards = session?.progress?.totalCards ?: 0,
                )
            } else {
                // Header card with deck info, progress, and copy button
                HeaderCard(
                    deckName = session?.deckName ?: "Unknown Deck",
                    currentIndex = currentIndex,
                    totalSuggestions = suggestions.size,
                    suggestion = suggestion,
                    editedFields = editedFields,
                    onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) },
                )

                Spacer(Modifier.height(Spacing.md))

                // Reasoning card with gradient styling
                ReasoningCard(
                    reasoning = suggestion.reasoning,
                )

                Spacer(Modifier.height(Spacing.md))

                // Suggested Changes Card with EditControls toolbar
                SuggestedChangesCard(
                    suggestion = suggestion,
                    editedFields = editedFields,
                    isEditing = isEditing,
                    showOriginal = showOriginal,
                    onToggleOriginal = onToggleOriginal,
                    onRevertEdits = onRevertEdits,
                    onEditField = onEditField,
                )

                Spacer(Modifier.height(Spacing.md))

                // Field comparisons
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    val fieldsWithChanges = suggestion.originalFields.filter { (name, _) ->
                        suggestion.changes.containsKey(name)
                    }

                    if (fieldsWithChanges.isEmpty()) {
                        item {
                            NoChangesMessage()
                        }
                    } else {
                        items(fieldsWithChanges.toList()) { (fieldName, originalField) ->
                            val aiValue = suggestion.changes[fieldName] ?: originalField.value
                            val editedValue = editedFields[fieldName]

                            FieldComparison(
                                fieldName = fieldName,
                                originalValue = originalField.value,
                                aiValue = aiValue,
                                editedValue = editedValue,
                                showOriginal = showOriginal,
                                onEdit = { onEditField(fieldName, it) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Action buttons (reordered: Reject+Skip left, Accept right)
                ActionButtons(
                    onAccept = onAccept,
                    onReject = onReject,
                    onSkip = onSkip,
                    isEdited = isEditing,
                    isLoading = isActionLoading,
                )
            }
        }
    }
}

@Composable
private fun Breadcrumb(
    onBackToSessions: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clickable(onClick = onBackToSessions)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.size(18.dp),
            tint = colors.accent,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "Back to Sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
        )
    }
}

@Composable
private fun HeaderCard(
    deckName: String,
    currentIndex: Int,
    totalSuggestions: Int,
    suggestion: Suggestion,
    editedFields: Map<String, String>,
    onCopy: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(colors.surface, colors.surfaceAlt),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.borderMuted),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Deck name with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.accentStrong,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(colors.divider),
                )

                // Field count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textMuted,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "${suggestion.changes.size} field(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Copy button
                OutlinedButton(
                    onClick = {
                        val copyText = buildCopyText(suggestion, editedFields)
                        onCopy(copyText)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surface,
                        contentColor = colors.textSecondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "Copy Suggestion",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                // Progress badge (pill style)
                Surface(
                    color = colors.accentMuted,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.accentStrong,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = "${currentIndex + 1} / $totalSuggestions",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

private fun buildCopyText(suggestion: Suggestion, editedFields: Map<String, String>): String {
    val sb = StringBuilder()
    sb.appendLine("## AI Reasoning")
    sb.appendLine(suggestion.reasoning)
    sb.appendLine()
    sb.appendLine("## Original Card")
    suggestion.originalFields.forEach { (name, field) ->
        sb.appendLine("**$name:** ${field.value}")
    }
    sb.appendLine()
    sb.appendLine("## Suggested Changes")
    suggestion.changes.forEach { (name, value) ->
        val finalValue = editedFields[name] ?: value
        val isEdited = editedFields.containsKey(name)
        val marker = if (isEdited) " *(edited)*" else " *(modified)*"
        sb.appendLine("**$name:**$marker $finalValue")
    }
    return sb.toString()
}

@Composable
private fun ReasoningCard(
    reasoning: String,
) {
    val colors = LocalAppColors.current
    // Gradient: accentMuted -> secondaryMuted -> accentMuted (green-blue-green)
    val gradientBrush = Brush.linearGradient(
        colors = listOf(colors.accentMuted, colors.secondaryMuted, colors.accentMuted),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Icon container (green background)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.accentStrong, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onAccent,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Reasoning",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,
                )
            }
        }
    }
}

@Composable
private fun NoChangesMessage() {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "No changes detected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(
    isProcessing: Boolean,
    processedCards: Int,
    totalCards: Int,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = colors.accent,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Analyzing Cards...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalCards > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "$processedCards / $totalCards cards processed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    LinearProgressIndicator(
                        progress = { if (totalCards > 0) processedCards.toFloat() / totalCards else 0f },
                        modifier = Modifier.width(200.dp),
                        color = colors.accent,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.success,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "No card to review",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "All suggestions have been reviewed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SuggestedChangesCard(
    suggestion: Suggestion,
    editedFields: Map<String, String>,
    isEditing: Boolean,
    showOriginal: Boolean,
    onToggleOriginal: () -> Unit,
    onRevertEdits: () -> Unit,
    onEditField: (String, String) -> Unit,
) {
    var showRevertDialog by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    // Dynamic title and colors based on edit state
    val (title, borderColor, headerColor) = when {
        isEditing && showOriginal -> Triple(
            "AI Suggested Changes",
            colors.secondary,        // Blue for viewing original AI
            colors.secondaryMuted,
        )
        isEditing -> Triple(
            "Manually Edited Changes",
            colors.warning,          // Amber for editing
            colors.warningMuted,
        )
        else -> Triple(
            "Suggested Changes",
            colors.accent,           // Green for default
            colors.accentMuted,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, borderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            // Header with dynamic color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isEditing && showOriginal -> Icons.Default.Visibility
                            isEditing -> Icons.Default.Edit
                            else -> Icons.Default.AutoAwesome
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = borderColor,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = borderColor,
                    )
                }

                // EditControls toolbar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Show Original / Show Edited toggle
                    FilterChip(
                        selected = showOriginal,
                        onClick = onToggleOriginal,
                        label = {
                            Text(
                                text = if (showOriginal) "AI" else "Edited",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showOriginal) Icons.Default.SmartToy else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        enabled = isEditing,
                    )

                    // Revert edits button (only visible when editing)
                    if (isEditing) {
                        IconButton(
                            onClick = { showRevertDialog = true },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Revert edits",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    // Revert confirmation dialog
    if (showRevertDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = { Text("Revert Edits?") },
            text = { Text("This will discard all your manual edits and restore the original AI suggestions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRevertEdits()
                        showRevertDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Revert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private enum class FieldViewMode { Diff, Edit }

@Composable
private fun FieldComparison(
    fieldName: String,
    originalValue: String,
    aiValue: String,
    editedValue: String?,
    showOriginal: Boolean,
    onEdit: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val displayValue = editedValue ?: aiValue
    var isExpanded by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(FieldViewMode.Diff) }
    val hasChanges = originalValue != aiValue

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fieldName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (hasChanges) {
                        Spacer(Modifier.width(Spacing.sm))
                        Surface(
                            color = colors.successMuted,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = "Changed",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.success,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                            )
                        }
                    }
                }
                Row {
                    // Toggle between Diff and Edit view
                    if (hasChanges) {
                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == FieldViewMode.Diff) FieldViewMode.Edit else FieldViewMode.Diff
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = if (viewMode == FieldViewMode.Diff) Icons.Default.Edit else Icons.Default.Compare,
                                contentDescription = if (viewMode == FieldViewMode.Diff) "Edit" else "View Diff",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    when {
                        // Show diff view
                        viewMode == FieldViewMode.Diff && hasChanges && editedValue == null -> {
                        if (showOriginal) {
                            // Side-by-side diff
                            DiffText(
                                original = originalValue,
                                modified = aiValue,
                                mode = DiffMode.SideBySide,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            // Inline diff
                            Text(
                                text = "Changes:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                DiffText(
                                    original = originalValue,
                                    modified = aiValue,
                                    mode = DiffMode.Inline,
                                    modifier = Modifier.padding(Spacing.sm),
                                )
                            }
                        }
                    }
                    // Edit view or no changes
                    else -> {
                        if (showOriginal) {
                            Text(
                                text = "Original:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = originalValue,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        colors.diffRemoved,
                                        MaterialTheme.shapes.small,
                                    )
                                    .padding(Spacing.sm),
                            )
                            Spacer(Modifier.height(Spacing.sm))
                        }

                        Text(
                            text = when {
                                editedValue != null -> "Your Edit:"
                                hasChanges -> "AI Suggestion:"
                                else -> "Value (unchanged):"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Determine container color based on edit state
                        val containerColor = when {
                            editedValue != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            hasChanges -> colors.diffAdded
                            else -> colors.surfaceAlt
                        }

                        // Use TextFieldState for modern API with contentPadding support
                        val fieldState = rememberTextFieldState(displayValue)

                        // Sync external changes to local state
                        LaunchedEffect(displayValue) {
                            if (fieldState.text.toString() != displayValue) {
                                fieldState.setTextAndPlaceCursorAtEnd(displayValue)
                            }
                        }

                        // Report local changes to parent
                        LaunchedEffect(fieldState) {
                            snapshotFlow { fieldState.text.toString() }
                                .drop(1) // Skip initial value
                                .collectLatest { onEdit(it) }
                        }

                        OutlinedTextField(
                            state = fieldState,
                            modifier = Modifier.fillMaxWidth(),
                            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = containerColor,
                                focusedContainerColor = containerColor,
                                unfocusedBorderColor = colors.border,
                                focusedBorderColor = colors.accentStrong,
                                unfocusedTextColor = colors.textPrimary,
                                focusedTextColor = colors.textPrimary,
                                cursorColor = colors.accent,
                            ),
                            shape = InputShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
    isEdited: Boolean,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Left side: Reject + Skip (grouped together, equal width)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Reject
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Reject")
            }

            // Skip
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Skip")
            }
        }

        // Right side: Accept (larger/prominent)
        Button(
            onClick = onAccept,
            modifier = Modifier.weight(0.8f),
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = 12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (isEdited) "Apply Edits" else "Accept",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
