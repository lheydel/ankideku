package com.ankideku.data.mapper

import com.ankideku.data.local.database.Review_memory
import com.ankideku.data.local.database.Review_message
import com.ankideku.data.local.database.Review_session
import com.ankideku.data.local.database.Review_suggestion
import com.ankideku.domain.model.ReviewMemory
import com.ankideku.domain.model.ReviewMessage
import com.ankideku.domain.model.ReviewMessageRole
import com.ankideku.domain.model.ReviewSession
import com.ankideku.domain.model.ReviewSuggestion
import com.ankideku.domain.model.ReviewSuggestionStatus
import com.ankideku.util.parseJson

fun Review_session.toDomain(): ReviewSession = ReviewSession(
    id = id,
    sessionId = session_id,
    llmProvider = llm_provider,
    startedAt = started_at,
    lastActivityAt = last_activity_at,
    contextConfig = context_config?.parseJson(),
)

fun Review_message.toDomain(): ReviewMessage = ReviewMessage(
    id = id,
    reviewSessionId = review_session_id,
    role = ReviewMessageRole.fromDbString(role),
    content = content,
    actionCalls = action_calls?.parseJson(),
    actionCallId = action_call_id,
    createdAt = created_at,
)

fun Review_memory.toDomain(): ReviewMemory = ReviewMemory(
    id = id,
    reviewSessionId = review_session_id,
    key = key,
    value = value_,
    createdAt = created_at,
)

fun Review_suggestion.toDomain(proposedChanges: Map<String, String>): ReviewSuggestion = ReviewSuggestion(
    id = id,
    reviewSessionId = review_session_id,
    messageId = message_id,
    suggestionId = suggestion_id,
    proposedChanges = proposedChanges,
    proposedReasoning = proposed_reasoning,
    status = ReviewSuggestionStatus.fromDbString(status),
    appliedAt = applied_at,
)
