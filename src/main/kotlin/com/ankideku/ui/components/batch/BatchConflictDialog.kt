package com.ankideku.ui.components.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.usecase.suggestion.BatchConflictStrategy
import com.ankideku.domain.usecase.suggestion.ConflictInfo
import com.ankideku.ui.components.AccentButton
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.screens.main.BatchAction
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Dialog shown when batch operation detects conflicts.
 * Offers options to force, skip conflicts, or cancel.
 */
@Composable
fun BatchConflictDialog(
    action: BatchAction,
    conflicts: List<ConflictInfo>,
    nonConflicting: List<Suggestion>,
    onForce: () -> Unit,
    onSkipConflicts: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalAppColors.current
    val totalCount = conflicts.size + nonConflicting.size
    val actionVerb = when (action) {
        BatchAction.Accept -> "accept"
        BatchAction.Reject -> "reject"
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Conflicts Detected") },
        text = {
            Column {
                Text(
                    text = "${conflicts.size} of $totalCount suggestions have conflicts.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(Spacing.sm))

                Text(
                    text = "A conflict occurs when the note has been modified in Anki since " +
                        "the suggestion was created.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "How would you like to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(Spacing.sm))

                // Option explanations
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "• Force: Overwrite all notes with AI suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "• Skip Conflicts: Only $actionVerb ${nonConflicting.size} non-conflicting suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {
            AccentButton(onClick = onForce) {
                Text("Force All")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(
                    onClick = onSkipConflicts,
                    variant = AppButtonVariant.Outlined,
                    enabled = nonConflicting.isNotEmpty(),
                ) {
                    Text("Skip Conflicts (${nonConflicting.size})")
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
