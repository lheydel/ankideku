package com.ankideku.ui.components.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankideku.domain.model.NoteField
import com.ankideku.domain.model.NoteTypeConfig
import com.ankideku.ui.components.DiffDisplayMode
import com.ankideku.ui.components.DiffHighlightedText
import com.ankideku.ui.components.FieldItem
import com.ankideku.ui.components.originalFieldStyle
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing

@Composable
fun OriginalCard(
    fields: Map<String, NoteField>,
    changes: Map<String, String>,
    noteTypeConfig: NoteTypeConfig?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier.fillMaxHeight(),
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
                Icon(Icons.Default.History, null, Modifier.size(18.dp), tint = colors.textMuted)
                Spacer(Modifier.width(Spacing.sm))
                Text("Original Card", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }

            HorizontalDivider(color = colors.divider)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                val sortedFields = fields.entries.sortedBy { it.value.order }
                items(sortedFields) { (fieldName, field) ->
                    val suggestedValue = changes[fieldName]
                    val isChanged = suggestedValue != null && suggestedValue != field.value

                    FieldItem(
                        fieldName = fieldName,
                        value = field.value,
                        isChanged = isChanged,
                        noteTypeConfig = noteTypeConfig,
                        style = originalFieldStyle(isChanged),
                        diffContent = if (isChanged && field.value.isNotEmpty()) {
                            { DiffHighlightedText(field.value, suggestedValue, DiffDisplayMode.Original) }
                        } else null,
                    )
                }
            }
        }
    }
}
