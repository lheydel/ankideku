package com.ankideku.domain.sel

import com.ankideku.domain.model.HistoryEntry
import com.ankideku.domain.model.Note
import com.ankideku.domain.model.Session
import com.ankideku.domain.model.Suggestion

/**
 * Result of a SEL query execution.
 *
 * Each variant corresponds to an EntityType and contains a typed list of results.
 * This sealed class allows callers to safely handle query results with exhaustive
 * when expressions.
 */
sealed interface SelResult {
    val count: Int

    data class Notes(val items: List<Note>) : SelResult {
        override val count: Int get() = items.size
    }

    data class Suggestions(val items: List<Suggestion>) : SelResult {
        override val count: Int get() = items.size
    }

    data class HistoryEntries(val items: List<HistoryEntry>) : SelResult {
        override val count: Int get() = items.size
    }

    data class Sessions(val items: List<Session>) : SelResult {
        override val count: Int get() = items.size
    }
}
