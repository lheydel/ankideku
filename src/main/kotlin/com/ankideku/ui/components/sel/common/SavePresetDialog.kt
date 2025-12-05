package com.ankideku.ui.components.sel.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ankideku.ui.components.AppAlertDialog
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Dialog for saving a query as a preset.
 */
@Composable
internal fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Save Preset",
        confirmButton = {
            Button(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                ),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                AppTextInput(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "My search query",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "Description (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                AppTextInput(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "What does this query do?",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
