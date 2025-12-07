package com.ankideku.ui.components.sel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.evaluator.SelSqlEvaluator
import com.ankideku.ui.components.sel.state.SelBuilderState
import com.ankideku.ui.theme.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Preview panel showing the JSON representation of the current SEL query.
 */
@Composable
fun SelPreview(
    state: SelBuilderState,
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current

    // Use derivedStateOf to automatically track all state changes within toJson()
    val json by remember {
        derivedStateOf {
            try {
                formatJson(state.toJson())
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    // Generate SQL for debugging (with params embedded)
    val sql by remember {
        derivedStateOf {
            try {
                val query = state.toSelQuery()
                val sqlQuery = SelSqlEvaluator.buildQuery(query)
                embedParams(sqlQuery.sql, sqlQuery.params)
            } catch (e: Exception) {
                "SQL Error: ${e.message}"
            }
        }
    }

    if (!enabled) {
        return
    }

    Surface(
        modifier = modifier,
        shape = InputShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Query Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
                IconButton(
                    onClick = {
                        Toolkit.getDefaultToolkit().systemClipboard
                            .setContents(StringSelection(json), null)
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        modifier = Modifier.size(16.dp),
                        tint = colors.textMuted,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 200.dp),
                shape = InputShape,
                color = colors.surfaceAlt,
            ) {
                Box(
                    modifier = Modifier
                        .padding(Spacing.sm)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = json,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // SQL Debug section
            Text(
                "SQL (debug)",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 100.dp),
                shape = InputShape,
                color = colors.surfaceAlt,
            ) {
                Box(
                    modifier = Modifier
                        .padding(Spacing.sm)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = sql,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple JSON formatter for readable preview.
 */
private fun formatJson(json: String): String {
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var prevChar = ' '

    for (c in json) {
        when {
            c == '"' && prevChar != '\\' -> {
                inString = !inString
                sb.append(c)
            }
            inString -> sb.append(c)
            c == '{' || c == '[' -> {
                sb.append(c)
                sb.append('\n')
                indent++
                sb.append("  ".repeat(indent))
            }
            c == '}' || c == ']' -> {
                sb.append('\n')
                indent--
                sb.append("  ".repeat(indent))
                sb.append(c)
            }
            c == ',' -> {
                sb.append(c)
                sb.append('\n')
                sb.append("  ".repeat(indent))
            }
            c == ':' -> {
                sb.append(": ")
            }
            !c.isWhitespace() -> sb.append(c)
        }
        prevChar = c
    }

    return sb.toString()
}

/**
 * Embed params into SQL query by replacing ? placeholders.
 */
private fun embedParams(sql: String, params: List<Any?>): String {
    var result = sql
    for (param in params) {
        val replacement = when (param) {
            null -> "NULL"
            is String -> "'${param.replace("'", "''")}'"
            is Number -> param.toString()
            else -> "'$param'"
        }
        result = result.replaceFirst("?", replacement)
    }
    return result
}
