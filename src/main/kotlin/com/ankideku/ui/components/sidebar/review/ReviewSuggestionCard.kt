package com.ankideku.ui.components.sidebar.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankideku.ui.screens.main.ReviewSuggestionUi
import com.ankideku.ui.theme.AppColorScheme
import com.ankideku.ui.theme.Spacing

@Composable
fun ReviewSuggestionCard(
    suggestion: ReviewSuggestionUi,
    colors: AppColorScheme,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceAlt,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI Suggestion",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                )
                Text(
                    text = "#${suggestion.suggestionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            // Proposed changes
            suggestion.proposedChanges.forEach { (field, value) ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = field,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Reasoning
            if (!suggestion.proposedReasoning.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = suggestion.proposedReasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.success,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Apply", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
