package com.ankideku.ui.components.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.components.Breadcrumb
import com.ankideku.ui.components.FieldItem
import com.ankideku.ui.components.previewFieldStyle
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

/**
 * Note preview content for pre-session browsing.
 * Shows a single note's fields without comparison columns.
 */
@Composable
fun ColumnScope.NotePreviewContent(
    note: Note,
    deckName: String,
    noteTypeConfig: NoteTypeConfig?,
    onCopy: () -> Unit,
    onOpenNoteTypeSettings: () -> Unit,
    onBackToSessions: () -> Unit,
) {
    val colors = LocalAppColors.current

    // Breadcrumb to go back to session selector
    Breadcrumb(text = "Back to Sessions", onClick = onBackToSessions)
    Spacer(Modifier.height(Spacing.sm))

    // Reuse the header card from comparison view
    ComparisonHeaderCard(
        deckName = deckName,
        modelName = note.modelName,
        onCopy = onCopy,
        onOpenNoteTypeSettings = onOpenNoteTypeSettings,
        trailingBadge = {},
    )

    Spacer(Modifier.height(Spacing.md))

    // Fields card - single card showing all fields (no comparison)
    Card(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(colors.surfaceAlt, MaterialTheme.colorScheme.surface)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, null, Modifier.size(18.dp), tint = colors.accent)
                Spacer(Modifier.width(Spacing.sm))
                Text("Note Fields", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }

            HorizontalDivider(color = colors.divider)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                val sortedFields = note.fields.entries.sortedBy { it.value.order }
                items(sortedFields) { (fieldName, field) ->
                    FieldItem(
                        fieldName = fieldName,
                        value = field.value,
                        isChanged = false,
                        noteTypeConfig = noteTypeConfig,
                        style = previewFieldStyle(),
                    )
                }
            }
        }
    }

    // Help text at the bottom
    Spacer(Modifier.height(Spacing.md))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = colors.textMuted,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "Write a prompt and start a session to get AI suggestions for this note",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
    }
}
