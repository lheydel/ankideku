package com.ankideku.ui.components.sel.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import com.ankideku.util.isEnterKey
import com.ankideku.ui.components.AccentButton
import com.ankideku.ui.components.AppAlertDialog
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppTextInput
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog for saving a query as a preset.
 */
@Composable
internal fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    checkNameExists: suspend (String) -> Boolean,
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var nameExists by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Focus the name field on dialog open
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Check name uniqueness when name changes
    LaunchedEffect(name) {
        nameExists = if (name.isNotBlank()) {
            withContext(Dispatchers.IO) {
                checkNameExists(name)
            }
        } else {
            false
        }
    }

    val isValid = name.isNotBlank() && !nameExists

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Save Preset",
        confirmButton = {
            AccentButton(
                onClick = { onSave(name) },
                enabled = isValid,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            AppButton(onClick = onDismiss, variant = AppButtonVariant.Text) {
                Text("Cancel")
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "Name",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            AppTextInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "My search query",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.isEnterKey() && isValid) {
                            onSave(name)
                            true
                        } else {
                            false
                        }
                    },
            )
            if (nameExists) {
                Text(
                    "A preset with this name already exists",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
        }
    }
}
