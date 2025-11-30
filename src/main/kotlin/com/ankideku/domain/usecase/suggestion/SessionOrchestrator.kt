package com.ankideku.domain.usecase.suggestion

import com.ankideku.data.remote.llm.LlmConfig
import com.ankideku.data.remote.llm.LlmResponse
import com.ankideku.data.remote.llm.LlmService
import com.ankideku.data.remote.llm.LlmServiceFactory
import com.ankideku.data.remote.llm.NoteTypeInfo
import com.ankideku.domain.model.*
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.util.TokenBatcher
import com.ankideku.util.onIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates AI processing sessions for deck analysis.
 * Manages session lifecycle: creation, batch processing, completion/failure.
 */
class SessionOrchestrator(
    private val sessionRepository: SessionRepository,
    private val suggestionRepository: SuggestionRepository,
    private val deckRepository: DeckRepository,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val MAX_CONCURRENT_BATCHES = 8
        private const val MAX_INPUT_TOKENS = 8_000
    }

    private val activeJobs = ConcurrentHashMap<SessionId, Job>()

    /**
     * Start a new AI processing session for a deck.
     */
    fun startSession(deckId: DeckId, prompt: String): Flow<SessionEvent> = flow {
        val context = prepareSession(deckId, prompt)
        emit(SessionEvent.Created(context.sessionId, context.deck.id, context.notes.size))

        trackJobForCancellation(context.sessionId)

        try {
            onIO { sessionRepository.updateState(context.sessionId, SessionState.Running) }
            val totalSuggestions = processBatches(context)
            onIO { sessionRepository.updateState(context.sessionId, SessionState.Completed) }
            emit(SessionEvent.Completed(context.sessionId, totalSuggestions))
        } catch (e: CancellationException) {
            onIO { sessionRepository.updateState(context.sessionId, SessionState.Cancelled) }
            emit(SessionEvent.Cancelled(context.sessionId))
            throw e
        } catch (e: Exception) {
            onIO { sessionRepository.updateState(context.sessionId, SessionState.Failed(e.message ?: "Unknown error")) }
            emit(SessionEvent.Failed(context.sessionId, e.message ?: "Unknown error"))
        } finally {
            activeJobs.remove(context.sessionId)
        }
    }

    /**
     * Prepare session: validate, load data, create batches, persist session.
     */
    private suspend fun prepareSession(deckId: DeckId, prompt: String): SessionContext {
        // Get LLM service with current settings
        val provider = onIO { settingsRepository.getSettings().llmProvider }
        val llmService = LlmServiceFactory.getInstance(LlmConfig(provider))

        // Health check - fail fast
        val health = llmService.getHealth()
        if (!health.available) {
            throw SessionException.LlmNotAvailable(health.error ?: "LLM service not available")
        }

        // Load deck and notes
        val deck = onIO { deckRepository.getDeck(deckId) }
            ?: throw SessionException.DeckNotFound(deckId)
        val notes = onIO { deckRepository.getNotesForDeck(deckId) }
        if (notes.isEmpty()) {
            throw SessionException.EmptyDeck(deckId)
        }

        // Extract note type and create batches
        val noteType = extractNoteType(notes.first())
        val batches = TokenBatcher(MAX_INPUT_TOKENS).createBatches(notes, prompt, noteType)

        // Persist session
        val session = Session(
            deckId = deck.id,
            deckName = deck.name,
            prompt = prompt,
            state = SessionState.Pending,
            progress = SessionProgress(totalCards = notes.size, totalBatches = batches.size),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val sessionId = onIO { sessionRepository.create(session) }

        return SessionContext(sessionId, deck, notes, batches, noteType, prompt, llmService)
    }

    private fun extractNoteType(note: Note) = NoteTypeInfo(
        name = note.modelName,
        fields = note.fields.values.sortedBy { it.order }.map { it.name },
    )

    private suspend fun trackJobForCancellation(sessionId: SessionId) {
        currentCoroutineContext()[Job]?.let { activeJobs[sessionId] = it }
    }

    /**
     * Process all batches in parallel with concurrency limit.
     */
    private suspend fun FlowCollector<SessionEvent>.processBatches(context: SessionContext): Int {
        val state = BatchProcessingState(context.notes.size, context.batches.size)
        val semaphore = Semaphore(MAX_CONCURRENT_BATCHES)

        coroutineScope {
            context.batches.mapIndexed { index, batchNotes ->
                async {
                    semaphore.withPermit {
                        processSingleBatch(context, batchNotes, index, state)
                    }
                }
            }.awaitAll()
        }

        return state.totalSuggestions
    }

    /**
     * Process a single batch of notes.
     */
    private suspend fun FlowCollector<SessionEvent>.processSingleBatch(
        context: SessionContext,
        batchNotes: List<Note>,
        batchIndex: Int,
        state: BatchProcessingState,
    ) {
        currentCoroutineContext().ensureActive()
        emit(SessionEvent.BatchStarted(context.sessionId, batchIndex + 1, context.batches.size))

        try {
            val response = context.llmService.analyzeBatch(batchNotes, context.prompt, context.noteType)
            val suggestions = saveSuggestions(context.sessionId, batchNotes, response)

            state.recordSuccess(batchNotes.size, suggestions.size, response.usage.inputTokens, response.usage.outputTokens)
            persistProgress(context.sessionId, state)

            emit(SessionEvent.BatchCompleted(context.sessionId, batchIndex + 1, suggestions.size))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.recordFailure(batchNotes.size)
            persistProgress(context.sessionId, state)
            emit(SessionEvent.BatchFailed(context.sessionId, batchIndex + 1, e.message ?: "Unknown error"))
        }
    }

    /**
     * Map LLM response to domain suggestions and save them.
     */
    private suspend fun saveSuggestions(
        sessionId: SessionId,
        batchNotes: List<Note>,
        response: LlmResponse,
    ): List<Suggestion> {
        val suggestions = response.suggestions.mapNotNull { llmSuggestion ->
            val note = batchNotes.find { it.id == llmSuggestion.noteId } ?: return@mapNotNull null
            Suggestion(
                sessionId = sessionId,
                noteId = llmSuggestion.noteId,
                originalFields = note.fields,
                changes = llmSuggestion.changes,
                reasoning = llmSuggestion.reasoning,
                status = SuggestionStatus.Pending,
                createdAt = System.currentTimeMillis(),
            )
        }

        if (suggestions.isNotEmpty()) {
            onIO { suggestionRepository.saveAll(suggestions) }
        }

        return suggestions
    }

    private suspend fun persistProgress(sessionId: SessionId, state: BatchProcessingState) {
        onIO { sessionRepository.updateProgress(sessionId, state.toProgress()) }
    }

    suspend fun cancelSession(sessionId: SessionId) {
        activeJobs[sessionId]?.cancel()
    }

    suspend fun markIncompleteSessionsOnStartup() {
        // Query sessions in "running" state and mark them as incomplete
    }
}

/**
 * Context for a session being processed.
 */
private data class SessionContext(
    val sessionId: SessionId,
    val deck: Deck,
    val notes: List<Note>,
    val batches: List<List<Note>>,
    val noteType: NoteTypeInfo,
    val prompt: String,
    val llmService: LlmService,
)

/**
 * Mutable state for batch processing progress (thread-safe via mutex).
 */
private class BatchProcessingState(
    private val totalCards: Int,
    private val totalBatches: Int,
) {
    private val mutex = Mutex()
    private var processedCards = 0
    private var processedBatches = 0
    var totalSuggestions = 0
        private set
    private var inputTokens = 0
    private var outputTokens = 0
    private var failedBatches = 0

    suspend fun recordSuccess(cards: Int, suggestions: Int, inTokens: Int, outTokens: Int) = mutex.withLock {
        processedCards += cards
        processedBatches++
        totalSuggestions += suggestions
        inputTokens += inTokens
        outputTokens += outTokens
    }

    suspend fun recordFailure(cards: Int) = mutex.withLock {
        processedCards += cards
        failedBatches++
    }

    suspend fun toProgress() = mutex.withLock {
        SessionProgress(
            processedCards = processedCards,
            totalCards = totalCards,
            processedBatches = processedBatches,
            totalBatches = totalBatches,
            suggestionsCount = totalSuggestions,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            failedBatches = failedBatches,
        )
    }
}

/**
 * Events emitted during session processing.
 */
sealed class SessionEvent {
    abstract val sessionId: SessionId

    data class Created(override val sessionId: SessionId, val deckId: DeckId, val noteCount: Int) : SessionEvent()
    data class BatchStarted(override val sessionId: SessionId, val batch: Int, val totalBatches: Int) : SessionEvent()
    data class BatchCompleted(override val sessionId: SessionId, val batch: Int, val suggestionsCount: Int) : SessionEvent()
    data class BatchFailed(override val sessionId: SessionId, val batch: Int, val error: String) : SessionEvent()
    data class Completed(override val sessionId: SessionId, val totalSuggestions: Int) : SessionEvent()
    data class Failed(override val sessionId: SessionId, val error: String) : SessionEvent()
    data class Cancelled(override val sessionId: SessionId) : SessionEvent()
}

/**
 * Exceptions that can occur during session processing.
 */
sealed class SessionException(message: String) : Exception(message) {
    data class LlmNotAvailable(val reason: String) : SessionException("LLM not available: $reason")
    data class DeckNotFound(val deckId: DeckId) : SessionException("Deck not found: $deckId")
    data class EmptyDeck(val deckId: DeckId) : SessionException("Deck has no notes: $deckId")
}
