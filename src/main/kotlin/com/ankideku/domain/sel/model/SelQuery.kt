package com.ankideku.domain.sel.model

import kotlinx.serialization.Serializable

/**
 * The type of entity being queried.
 */
@Serializable
enum class EntityType {
    Note,
    Suggestion,
    HistoryEntry,
    Session
}
