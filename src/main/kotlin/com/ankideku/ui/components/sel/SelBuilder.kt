package com.ankideku.ui.components.sel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.ui.components.sel.state.*
import com.ankideku.ui.theme.*

/**
 * Visual query builder for SEL expressions.
 *
 * Provides an interactive UI for building complex nested queries
 * with conditions, expressions, and subqueries.
 */
@Composable
fun SelBuilder(
    state: SelBuilderState,
    noteTypeFields: Map<String, List<String>> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val isInSubquery = state.navigationStack.isNotEmpty()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Natural language preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = InputShape,
            color = colors.surfaceAlt,
        ) {
            Text(
                text = state.toPreviewText(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(Spacing.sm),
            )
        }

        // Query expression in compact inline format
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = InputShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ConditionGroupEditor(
                    group = state.currentGroup,
                    entityType = state.currentTarget,
                    depth = 0,
                    parentScopes = state.parentScopes,
                    noteTypeFields = noteTypeFields,
                    onSubqueryClick = { subquery -> state.navigateToSubquery(subquery) },
                )
            }
        }

        // Back button when in subquery
        if (isInSubquery) {
            TextButton(
                onClick = { state.navigateBack() },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text("Back")
            }
        }
    }
}

