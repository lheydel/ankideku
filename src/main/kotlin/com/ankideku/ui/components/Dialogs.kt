package com.ankideku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.usecase.suggestion.BatchConflictStrategy
import com.ankideku.ui.components.batch.BatchConflictDialog
import com.ankideku.ui.screens.main.DialogState
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
            onUseCurrent = dialogState.onUseCurrent,
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            AppButton(
                onClick = onConfirm,
                variant = AppButtonVariant.Text,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
            ) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Dialog shown when a note has been modified since the suggestion was created.
 * Offers options to use AI changes, keep current, or cancel.
 */
@Composable
fun ConflictDialog(
    suggestion: Suggestion,
    currentFields: Map<String, NoteField>,
    onUseAi: () -> Unit,
    onUseCurrent: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Conflict Detected") },
        text = {
            Column {
                Text(
                    "The note has been modified since this suggestion was created. " +
                    "The following fields have changed:"
                )
                Spacer(Modifier.height(Spacing.sm))

                // Show which fields changed
                val changedFields = suggestion.originalFields.filter { (name, original) ->
                    currentFields[name]?.value != original.value
                }

                changedFields.forEach { (name, _) ->
                    Text(
                        text = "• $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Text("How would you like to proceed?")
            }
        },
        confirmButton = {
            AppButton(onClick = onUseAi) {
                Text("Use AI Changes")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(
                    onClick = onUseCurrent,
                    variant = AppButtonVariant.Text,
                ) {
                    Text("Keep Current")
                }
                AppButton(
                    onClick = onCancel,
                    variant = AppButtonVariant.Text,
                ) {
                    Text("Cancel")
                }
            }
        },
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            AppButton(
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
            ) {
                Text("OK")
            }
        },
    )
}
