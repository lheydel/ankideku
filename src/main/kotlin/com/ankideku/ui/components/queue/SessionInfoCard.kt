package com.ankideku.ui.components.queue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ankideku.domain.model.Session
import com.ankideku.ui.components.SessionStateChip
import com.ankideku.ui.theme.Spacing

@Composable
fun SessionInfoCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Session ${session.id}",
                    style = MaterialTheme.typography.labelMedium,
                )
                SessionStateChip(session.state, small = true)
            }

            Spacer(Modifier.height(Spacing.sm))

            // Progress
            val progress = session.progress
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${progress.processedCards}/${progress.totalCards} cards",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "${progress.suggestionsCount} suggestions",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
