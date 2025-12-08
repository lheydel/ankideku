package com.ankideku.ui.screens.main.actions

import com.ankideku.domain.model.DeckId
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.sel.SelResult
import com.ankideku.domain.sel.SelService
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.ui.screens.main.ToastType
import com.ankideku.util.onIO
import kotlinx.coroutines.launch

interface NoteBrowseActions {
    fun loadDeckNotes(deckId: DeckId)
    fun selectNoteForPreview(index: Int)
    fun clearNotePreview()
    fun executeNoteFilter(query: SelQuery)
    fun clearNoteFilter()
}

class NoteBrowseActionsImpl(
    private val ctx: ViewModelContext,
    private val deckRepository: DeckRepository,
    private val selService: SelService,
) : NoteBrowseActions {

    override fun loadDeckNotes(deckId: DeckId) {
        ctx.scope.launch {
            val notes = onIO { deckRepository.getNotesForDeck(deckId) }
            ctx.update { copy(deckNotes = notes, selectedNoteIndex = 0) }
        }
    }

    override fun selectNoteForPreview(index: Int) {
        ctx.update { copy(selectedNoteIndex = index) }
    }

    override fun clearNotePreview() {
        ctx.update { copy(selectedNoteIndex = 0) }
    }

    override fun executeNoteFilter(query: SelQuery) {
        ctx.scope.launch {
            try {
                val result = selService.execute(query)
                if (result !is SelResult.Notes) {
                    ctx.showToast("Query must target Notes", ToastType.Error)
                    return@launch
                }
                // Filter to only notes in current deck (including subdecks)
                val deckName = ctx.currentState.selectedDeck?.name
                val filtered = result.items.filter { note ->
                    note.deckName == deckName || note.deckName.startsWith("$deckName::")
                }
                ctx.update {
                    copy(
                        filteredNotes = filtered,
                        noteFilterQuery = query,
                        selectedNoteIndex = 0,
                    )
                }
                if (filtered.isEmpty()) {
                    ctx.showToast("No notes match your filter", ToastType.Info)
                }
            } catch (e: Exception) {
                ctx.showToast("Filter failed: ${e.message}", ToastType.Error)
            }
        }
    }

    override fun clearNoteFilter() {
        ctx.update { copy(filteredNotes = null, noteFilterQuery = null, selectedNoteIndex = 0) }
    }
}
