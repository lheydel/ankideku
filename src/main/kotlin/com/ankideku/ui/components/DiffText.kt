package com.ankideku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.ankideku.ui.theme.Spacing
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation

/**
 * Renders a visual diff between original and modified text.
 * Uses diff-match-patch for character-level diffing.
 *
 * @param original The original text
 * @param modified The modified text
 * @param mode How to display the diff
 * @param modifier Modifier for the component
 */
@Composable
fun DiffText(
    original: String,
    modified: String,
    mode: DiffMode = DiffMode.Inline,
    modifier: Modifier = Modifier,
) {
    val diffs = remember(original, modified) {
        val dmp = DiffMatchPatch()
        val diffs = dmp.diffMain(original, modified)
        dmp.diffCleanupSemantic(diffs)
        diffs
    }

    when (mode) {
        DiffMode.Inline -> InlineDiff(diffs, modifier)
        DiffMode.SideBySide -> SideBySideDiff(original, modified, diffs, modifier)
        DiffMode.Unified -> UnifiedDiff(diffs, modifier)
    }
}

enum class DiffMode {
    Inline,      // Show deletions and additions inline
    SideBySide,  // Show original and modified side by side
    Unified,     // Show only the result with change highlights
}

@Composable
private fun InlineDiff(
    diffs: List<DiffMatchPatch.Diff>,
    modifier: Modifier = Modifier,
) {
    val annotatedString = remember(diffs) {
        buildAnnotatedString {
            diffs.forEach { diff ->
                when (diff.operation) {
                    Operation.DELETE -> {
                        withStyle(
                            SpanStyle(
                                background = Color(0xFFFFCDD2), // Light red
                                textDecoration = TextDecoration.LineThrough,
                            )
                        ) {
                            append(diff.text)
                        }
                    }
                    Operation.INSERT -> {
                        withStyle(
                            SpanStyle(
                                background = Color(0xFFC8E6C9), // Light green
                            )
                        ) {
                            append(diff.text)
                        }
                    }
                    Operation.EQUAL -> {
                        append(diff.text)
                    }
                    null -> append(diff.text)
                }
            }
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}

@Composable
private fun SideBySideDiff(
    original: String,
    modified: String,
    diffs: List<DiffMatchPatch.Diff>,
    modifier: Modifier = Modifier,
) {
    val (originalAnnotated, modifiedAnnotated) = remember(diffs) {
        val origBuilder = AnnotatedString.Builder()
        val modBuilder = AnnotatedString.Builder()

        diffs.forEach { diff ->
            when (diff.operation) {
                Operation.DELETE -> {
                    origBuilder.withStyle(
                        SpanStyle(background = Color(0xFFFFCDD2))
                    ) {
                        append(diff.text)
                    }
                }
                Operation.INSERT -> {
                    modBuilder.withStyle(
                        SpanStyle(background = Color(0xFFC8E6C9))
                    ) {
                        append(diff.text)
                    }
                }
                Operation.EQUAL -> {
                    origBuilder.append(diff.text)
                    modBuilder.append(diff.text)
                }
                null -> {
                    origBuilder.append(diff.text)
                    modBuilder.append(diff.text)
                }
            }
        }

        origBuilder.toAnnotatedString() to modBuilder.toAnnotatedString()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Original
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = "Original",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                SelectionContainer {
                    Text(
                        text = originalAnnotated,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Modified
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = "Modified",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                SelectionContainer {
                    Text(
                        text = modifiedAnnotated,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedDiff(
    diffs: List<DiffMatchPatch.Diff>,
    modifier: Modifier = Modifier,
) {
    // Show only the result (modified) with insertion highlights
    val annotatedString = remember(diffs) {
        buildAnnotatedString {
            diffs.forEach { diff ->
                when (diff.operation) {
                    Operation.DELETE -> {
                        // Skip deletions in unified view
                    }
                    Operation.INSERT -> {
                        withStyle(
                            SpanStyle(
                                background = Color(0xFFC8E6C9),
                            )
                        ) {
                            append(diff.text)
                        }
                    }
                    Operation.EQUAL -> {
                        append(diff.text)
                    }
                    null -> append(diff.text)
                }
            }
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}

/**
 * Mode for DiffHighlightedText display
 */
enum class DiffDisplayMode {
    /** Show deletions highlighted (for original card) */
    Original,
    /** Show additions highlighted (for suggested card) */
    Suggested,
}

/**
 * Displays text with diff highlighting between original and modified versions.
 * Used in comparison panels.
 *
 * - Original mode: Shows deletions in red, hides additions
 * - Suggested mode: Shows additions in green, hides deletions
 */
@Composable
fun DiffHighlightedText(
    original: String,
    modified: String,
    mode: DiffDisplayMode,
) {
    val colors = com.ankideku.ui.theme.LocalAppColors.current
    val diffs = remember(original, modified) {
        val dmp = DiffMatchPatch()
        val diffs = dmp.diffMain(original, modified)
        dmp.diffCleanupSemantic(diffs)
        diffs
    }

    // Capture colors for remember block
    val diffRemovedBg = colors.diffRemoved
    val diffRemovedText = colors.diffRemovedText
    val diffAddedBg = colors.diffAdded
    val diffAddedText = colors.diffAddedText

    val annotatedString = remember(diffs, mode, diffRemovedBg, diffRemovedText, diffAddedBg, diffAddedText) {
        buildAnnotatedString {
            diffs.forEach { diff ->
                when (diff.operation) {
                    Operation.DELETE -> {
                        if (mode == DiffDisplayMode.Original) {
                            // Show deletions in red on original side
                            withStyle(SpanStyle(
                                background = diffRemovedBg,
                                color = diffRemovedText,
                            )) {
                                append(diff.text)
                            }
                        }
                        // Hide deletions on suggested side
                    }
                    Operation.INSERT -> {
                        if (mode == DiffDisplayMode.Suggested) {
                            // Show additions in green on suggested side
                            withStyle(SpanStyle(
                                background = diffAddedBg,
                                color = diffAddedText,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            )) {
                                append(diff.text)
                            }
                        }
                        // Hide additions on original side
                    }
                    Operation.EQUAL -> append(diff.text)
                    null -> append(diff.text)
                }
            }
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
    }
}

/**
 * Simple diff stats for display
 */
data class DiffStats(
    val additions: Int,
    val deletions: Int,
    val unchanged: Int,
) {
    val hasChanges: Boolean get() = additions > 0 || deletions > 0
}

/**
 * Calculate diff statistics
 */
fun calculateDiffStats(original: String, modified: String): DiffStats {
    val dmp = DiffMatchPatch()
    val diffs = dmp.diffMain(original, modified)

    var additions = 0
    var deletions = 0
    var unchanged = 0

    diffs.forEach { diff ->
        when (diff.operation) {
            Operation.INSERT -> additions += diff.text.length
            Operation.DELETE -> deletions += diff.text.length
            Operation.EQUAL -> unchanged += diff.text.length
            null -> unchanged += diff.text.length
        }
    }

    return DiffStats(additions, deletions, unchanged)
}
