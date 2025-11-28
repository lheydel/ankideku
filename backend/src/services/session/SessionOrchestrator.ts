/**
 * Session Orchestrator Service
 * Manages the entire session lifecycle for AI card processing
 * Coordinates between LLM, file writing, state persistence, and WebSocket events
 */

import type { Note, SessionProgress } from '../../types/index.js';
import type { LLMService, NoteTypeInfo } from '../llm';
import { LLMServiceFactory } from '../llm';
import { SuggestionWriter } from '../storage/SuggestionWriter.js';
import { SessionEventEmitter } from './SessionEventEmitter.js';
import { sessionService } from './SessionService.js';
import { cacheService } from '../anki/CacheService.js';
import { settingsService } from '../storage/SettingsService.js';
import { createTokenBasedBatches } from '../../utils/tokenizer.js';
import { CONFIG } from '../../config.js';
import pLimit from 'p-limit';
import { Mutex } from 'async-mutex';

/**
 * Configuration for session orchestration
 */
interface OrchestratorConfig {
  maxInputTokens: number;
}

const DEFAULT_CONFIG: OrchestratorConfig = {
  maxInputTokens: CONFIG.session.maxInputTokens,
};

/**
 * Session execution result
 */
export interface SessionResult {
  sessionId: string;
  totalCards: number;
  processedCards: number;
  suggestionsGenerated: number;
  batchesProcessed: number;
  batchesFailed: number;
  inputTokens: number;
  outputTokens: number;
  errors: string[];
}

/**
 * Orchestrates AI processing sessions
 */
export class SessionOrchestrator {
  private llmService: LLMService;
  private suggestionWriter: SuggestionWriter;
  private eventEmitter: SessionEventEmitter;
  private config: OrchestratorConfig;
  private abortControllers: Map<string, AbortController> = new Map();
  private progressMutexes: Map<string, Mutex> = new Map();

  constructor(
    suggestionWriter: SuggestionWriter,
    eventEmitter: SessionEventEmitter,
    config: Partial<OrchestratorConfig> = {}
  ) {
    // LLM service will be initialized in executeSession based on current settings
    this.llmService = null!;
    this.suggestionWriter = suggestionWriter;
    this.eventEmitter = eventEmitter;
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  /**
   * Execute a session - main entry point
   * Processes batches in parallel with configurable concurrency
   */
  async executeSession(sessionId: string): Promise<SessionResult> {
    if (this.abortControllers.has(sessionId)) {
      throw new Error(`Session ${sessionId} is already running`);
    }

    // Create abort controller and mutex for this session
    const controller = new AbortController();
    this.abortControllers.set(sessionId, controller);
    this.progressMutexes.set(sessionId, new Mutex());

    const result: SessionResult = {
      sessionId,
      totalCards: 0,
      processedCards: 0,
      suggestionsGenerated: 0,
      batchesProcessed: 0,
      batchesFailed: 0,
      inputTokens: 0,
      outputTokens: 0,
      errors: [],
    };

    try {
      // Get LLM service based on current settings
      const llmConfig = await settingsService.getLLMConfig();
      this.llmService = LLMServiceFactory.getInstance(llmConfig);

      // Check LLM health early - fail fast before loading cards
      const health = await this.llmService.getHealth();
      if (!health.available) {
        throw new Error(health.error || 'LLM service not available');
      }
      console.log(`[SessionOrchestrator] LLM health check passed: ${health.info || 'available'}`);

      // Load session request
      const request = await sessionService.getSessionRequest(sessionId);
      console.log(`[SessionOrchestrator] Starting session ${sessionId} for deck "${request.deckName}"`);

      // Mark session as running and emit state
      await sessionService.markSessionRunning(sessionId);
      await this.emitCurrentState(sessionId);

      // Load cards from cache
      const cards = await this.loadDeckCards(request.deckName);
      result.totalCards = cards.length;

      // Emit initial progress
      await this.updateProgress(sessionId, 0, result.totalCards, 0, 0, 0);

      if (cards.length === 0) {
        console.log(`[SessionOrchestrator] No cards found for deck "${request.deckName}"`);
        await this.completeSession(sessionId, result);
        return result;
      }

      // Get note type info from first card
      const noteType = this.extractNoteType(cards[0]);

      // Create token-based batches
      const batches = createTokenBasedBatches(
        cards,
        request.prompt,
        noteType,
        this.config.maxInputTokens
      );
      console.log(`[SessionOrchestrator] Created ${batches.length} batches for ${cards.length} cards (max ${CONFIG.session.maxConcurrentBatches} concurrent)`);

      // Process batches in parallel with concurrency limit
      const limit = pLimit(CONFIG.session.maxConcurrentBatches);
      const batchPromises = batches.map((batch, index) =>
        limit(() => this.processBatchWithTracking(
          sessionId,
          batch,
          index,
          batches.length,
          request.prompt,
          noteType,
          result,
          controller.signal
        ))
      );

      await Promise.allSettled(batchPromises);

      // Check if session was cancelled (state already set by cancel route)
      if (controller.signal.aborted) {
        // Emit state change so frontend receives cancelled status
        await this.emitCurrentState(sessionId);
        console.log(
          `[SessionOrchestrator] Session ${sessionId} cancelled: ` +
          `${result.processedCards}/${result.totalCards} cards processed, ${result.suggestionsGenerated} suggestions`
        );
        return result;
      }

      // Complete session normally
      await this.completeSession(sessionId, result);
      return result;

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`[SessionOrchestrator] Session ${sessionId} failed: ${errorMessage}`);

      await sessionService.markSessionFailed(sessionId, errorMessage);
      await this.emitCurrentState(sessionId);
      this.eventEmitter.emitError(sessionId, errorMessage);

      result.errors.push(errorMessage);
      return result;

    } finally {
      this.abortControllers.delete(sessionId);
      this.progressMutexes.delete(sessionId);
    }
  }

  /**
   * Process a batch with progress tracking (mutex-protected)
   */
  private async processBatchWithTracking(
    sessionId: string,
    batch: Note[],
    batchIndex: number,
    totalBatches: number,
    userPrompt: string,
    noteType: NoteTypeInfo,
    result: SessionResult,
    signal: AbortSignal
  ): Promise<void> {
    // Check if cancelled before starting
    if (signal.aborted) {
      return;
    }

    const batchNoteIds = batch.map(c => c.noteId);
    console.log(`[SessionOrchestrator] Processing batch ${batchIndex + 1}/${totalBatches} (${batch.length} cards)`);

    try {
      // Call LLM service with abort signal
      const response = await this.llmService.analyzeBatch(batch, userPrompt, noteType, signal);

      const inputTokens = response.usage?.inputTokens ?? 0;
      const outputTokens = response.usage?.outputTokens ?? 0;

      // Write suggestions (outside mutex - independent files)
      let suggestionsCount = 0;
      if (response.suggestions.length > 0) {
        const notesMap = new Map<number, Note>();
        for (const card of batch) {
          notesMap.set(card.noteId, card);
        }

        const writtenSuggestions = await this.suggestionWriter.writeBatch(
          sessionId,
          response.suggestions,
          notesMap
        );

        // Emit batch of suggestions via WebSocket
        this.eventEmitter.emitSuggestionBatch(sessionId, writtenSuggestions);

        suggestionsCount = writtenSuggestions.length;
      }

      // Mutex-protected progress update
      const mutex = this.progressMutexes.get(sessionId)!;
      await mutex.runExclusive(async () => {
        result.processedCards += batch.length;
        result.suggestionsGenerated += suggestionsCount;
        result.inputTokens += inputTokens;
        result.outputTokens += outputTokens;
        result.batchesProcessed++;

        await this.updateProgress(
          sessionId,
          result.processedCards,
          result.totalCards,
          result.suggestionsGenerated,
          result.inputTokens,
          result.outputTokens
        );
      });

      console.log(`[SessionOrchestrator] Batch ${batchIndex + 1} completed: ${suggestionsCount} suggestions (${inputTokens} in / ${outputTokens} out tokens)`);

    } catch (error) {
      // Handle AbortError specially - don't count as failure
      if (error instanceof Error && error.name === 'AbortError') {
        console.log(`[SessionOrchestrator] Batch ${batchIndex + 1} cancelled`);
        return;
      }

      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(
        `[SessionOrchestrator] Batch ${batchIndex + 1} failed (noteIds: ${batchNoteIds.slice(0, 3).join(', ')}...): ${errorMessage}`
      );

      // Still update progress on failure (cards were "processed", just unsuccessfully)
      const mutex = this.progressMutexes.get(sessionId)!;
      await mutex.runExclusive(async () => {
        result.processedCards += batch.length;
        result.batchesFailed++;
        result.errors.push(`Batch ${batchIndex + 1} (noteIds: ${batchNoteIds.slice(0, 3).join(', ')}...): ${errorMessage}`);

        await this.updateProgress(
          sessionId,
          result.processedCards,
          result.totalCards,
          result.suggestionsGenerated,
          result.inputTokens,
          result.outputTokens
        );
      });
    }
  }

  /**
   * Update progress - saves to state.json and emits via WebSocket
   */
  private async updateProgress(
    sessionId: string,
    processed: number,
    total: number,
    suggestionsCount: number,
    inputTokens: number,
    outputTokens: number
  ): Promise<void> {
    const progress: SessionProgress = { processed, total, suggestionsCount, inputTokens, outputTokens };
    const updatedState = await sessionService.updateSessionProgress(sessionId, progress);

    if (updatedState) {
      this.eventEmitter.emitStateChange(sessionId, updatedState);
    }
  }

  /**
   * Emit current state from state.json
   */
  private async emitCurrentState(sessionId: string): Promise<void> {
    const state = await sessionService.getSessionState(sessionId);
    if (state) {
      this.eventEmitter.emitStateChange(sessionId, state);
    }
  }

  /**
   * Load cards from cache for a deck
   */
  private async loadDeckCards(deckName: string): Promise<Note[]> {
    const cachedData = await cacheService.getCachedNotes(deckName);

    if (!cachedData) {
      throw new Error(`No cached data found for deck "${deckName}". Please sync the deck first.`);
    }

    console.log(`[SessionOrchestrator] Loaded ${cachedData.notes.length} cards from cache`);
    return cachedData.notes;
  }

  /**
   * Extract note type info from a card
   */
  private extractNoteType(card: Note): NoteTypeInfo {
    return {
      modelName: card.modelName,
      fieldNames: Object.keys(card.fields),
    };
  }

  /**
   * Complete a session successfully
   */
  private async completeSession(
    sessionId: string,
    result: SessionResult
  ): Promise<void> {
    await sessionService.markSessionCompleted(sessionId, 0);
    await this.emitCurrentState(sessionId);
    this.eventEmitter.emitComplete(sessionId, result.suggestionsGenerated);

    console.log(
      `[SessionOrchestrator] Session ${sessionId} completed: ` +
      `${result.suggestionsGenerated} suggestions from ${result.processedCards}/${result.totalCards} cards ` +
      `(${result.batchesProcessed} batches, ${result.batchesFailed} failed)`
    );
  }

  /**
   * Cancel a running session
   * Aborts all in-flight LLM requests for this session
   */
  cancelSession(sessionId: string): boolean {
    const controller = this.abortControllers.get(sessionId);
    if (!controller) {
      return false;
    }
    controller.abort();
    this.abortControllers.delete(sessionId);
    console.log(`[SessionOrchestrator] Session ${sessionId} cancelled`);
    return true;
  }

  /**
   * Check if a session is currently running
   */
  isRunning(sessionId: string): boolean {
    return this.abortControllers.has(sessionId);
  }

  /**
   * Get list of active sessions
   */
  getActiveSessions(): string[] {
    return Array.from(this.abortControllers.keys());
  }
}
