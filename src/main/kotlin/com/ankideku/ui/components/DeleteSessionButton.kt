package com.ankideku.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.ui.theme.handPointer
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.DestructiveButton

/**
 * Reusable delete session button with confirmation dialog.
 */
@Composable
fun DeleteSessionButton(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDeleteConfirm = true },
        modifier = modifier.handPointer(),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Session",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete this session? This will permanently remove all suggestions.") },
            confirmButton = {
                DestructiveButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    variant = AppButtonVariant.Text,
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                AppButton(
                    onClick = { showDeleteConfirm = false },
                    variant = AppButtonVariant.Text,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
