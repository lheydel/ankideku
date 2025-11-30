package com.ankideku.domain.model

data class HistoryEntry(
    val id: HistoryEntryId = 0,
    val sessionId: SessionId,
    val noteId: NoteId,
    val deckId: DeckId,
    val deckName: String,
    val action: ReviewAction,
    val originalFields: Map<String, NoteField>,
    val aiChanges: Map<String, String>,
    val appliedChanges: Map<String, String>?,  // null if rejected
    val userEdits: Map<String, String>?,       // Only fields user manually edited
    val reasoning: String?,
    val timestamp: Long,  // Unix timestamp in ms
)

enum class ReviewAction(val dbString: String) {
    Accept("accept"),
    Reject("reject"),
    Skip("skip");

    companion object {
        private val byDbString = entries.associateBy { it.dbString }
        fun fromDbString(value: String): ReviewAction = byDbString[value] ?: Skip
    }
}
