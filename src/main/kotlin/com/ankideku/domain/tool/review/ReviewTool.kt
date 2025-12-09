package com.ankideku.domain.tool.review

import com.ankideku.domain.model.ReviewSessionId
import com.ankideku.domain.model.SessionId
import com.ankideku.domain.model.Suggestion
import com.ankideku.domain.model.SuggestionId
import com.ankideku.domain.tool.AiTool
import com.ankideku.domain.tool.ToolContext

/**
 * Context for review session tools.
 * Provides access to the current review session state.
 */
data class ReviewToolContext(
    val reviewSessionId: ReviewSessionId,
    val sessionId: SessionId,
    val currentSuggestionId: SuggestionId?,  // Currently viewed suggestion
    val allSuggestions: List<Suggestion>,
    val memory: Map<String, String>,
    /** Callback to save a memory entry */
    val onSaveMemory: suspend (key: String, value: String) -> Unit,
    /** Callback to delete a memory entry */
    val onDeleteMemory: suspend (key: String) -> Unit,
    /** Callback to create a review suggestion */
    val onCreateReviewSuggestion: suspend (
        suggestionId: SuggestionId,
        changes: Map<String, String>,
        reasoning: String?,
    ) -> Unit,
) : ToolContext

/**
 * Interface for review session tools.
 */
interface ReviewTool : AiTool<ReviewToolContext>
