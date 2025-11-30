package com.ankideku.data.mapper

import com.ankideku.data.local.database.Session as DbSession
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.SessionProgress
import com.ankideku.domain.model.SessionState

fun DbSession.toDomain(): Session = Session(
    id = id,
    deckId = deck_id,
    deckName = deck_name,
    prompt = prompt,
    state = SessionState.fromDbString(
        state,
        state_message,
        exit_code?.toInt()
    ),
    progress = SessionProgress(
        processedBatches = progress_processed_batches.toInt(),
        totalBatches = progress_total_batches.toInt(),
        suggestionsCount = progress_suggestions_count.toInt(),
        inputTokens = progress_input_tokens.toInt(),
        outputTokens = progress_output_tokens.toInt(),
        failedBatches = progress_failed_batches.toInt(),
    ),
    createdAt = created_at,
    updatedAt = updated_at,
)
