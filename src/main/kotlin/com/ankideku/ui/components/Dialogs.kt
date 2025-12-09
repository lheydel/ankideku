package com.ankideku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.usecase.suggestion.BatchConflictStrategy
import com.ankideku.ui.components.batch.BatchConflictDialog
import com.ankideku.ui.screens.main.DialogState
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Renders the appropriate dialog based on the current dialog state.
 */
@Composable
fun AppDialogs(
    dialogState: DialogState?,
    onDismiss: () -> Unit,
    onBatchConflictAction: ((BatchConflictStrategy) -> Unit)? = null,
) {
    when (dialogState) {
        is DialogState.Confirm -> ConfirmDialog(
            title = dialogState.title,
            message = dialogState.message,
            confirmLabel = dialogState.confirmLabel,
            onConfirm = {
                dialogState.onConfirm()
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        is DialogState.Conflict -> ConflictDialog(
            suggestion = dialogState.suggestion,
            currentFields = dialogState.currentFields,
            onUseAi = dialogState.onUseAi,
            onRefresh = dialogState.onRefresh,
            onCancel = dialogState.onCancel,
        )
        is DialogState.Error -> ErrorDialog(
            title = dialogState.title,
            message = dialogState.message,
            onDismiss = dialogState.onDismiss,
        )
        is DialogState.BatchConflict -> BatchConflictDialog(
            action = dialogState.action,
            conflicts = dialogState.conflicts,
            nonConflicting = dialogState.nonConflicting,
            onForce = { onBatchConflictAction?.invoke(BatchConflictStrategy.Force) },
            onSkipConflicts = { onBatchConflictAction?.invoke(BatchConflictStrategy.SkipConflicts) },
            onCancel = onDismiss,
        )
        null -> { /* No dialog */ }
    }
}

/**
 * Generic confirmation dialog.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 280.dp, max = 400.dp),
    ) {
        DialogContent(
            title = title,
            message = message,
            buttons = {
                AppButton(onClick = onDismiss, variant = AppButtonVariant.Text) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(Spacing.sm))
                if (isDestructive) {
                    DestructiveButton(onClick = onConfirm) { Text(confirmLabel) }
                } else {
                    AppButton(onClick = onConfirm) { Text(confirmLabel) }
                }
            },
        )
    }
}

/**
 * Dialog shown when a note has been modified since the suggestion was created.
 * Offers options to use AI changes, refresh the card with current state, or cancel.
 */
@Composable
fun ConflictDialog(
    suggestion: Suggestion,
    currentFields: Map<String, NoteField>,
    onUseAi: () -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalAppColors.current

    val changedFields = suggestion.originalFields.filter { (name, original) ->
        currentFields[name]?.value != original.value
    }

    AppDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.widthIn(min = 300.dp, max = 450.dp),
    ) {
        DialogContent(
            title = "Conflict Detected",
            message = "The note has been modified since this suggestion was created. The following fields have changed:",
            content = {
                Spacer(Modifier.height(Spacing.sm))
                changedFields.forEach { (name, _) ->
                    Text(
                        text = "• $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "How would you like to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            },
            buttons = {
                AppButton(onClick = onCancel, variant = AppButtonVariant.Text) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(Spacing.sm))
                AppButton(onClick = onRefresh, variant = AppButtonVariant.Outlined) {
                    Text("Refresh Card")
                }
                Spacer(Modifier.width(Spacing.sm))
                AppButton(onClick = onUseAi) {
                    Text("Use AI Changes")
                }
            },
        )
    }
}

/**
 * Error display dialog with title and message.
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 280.dp, max = 400.dp),
    ) {
        DialogContent(
            title = title,
            titleColor = colors.error,
            message = message,
            buttons = {
                AppButton(onClick = onDismiss) { Text("OK") }
            },
        )
    }
}
