/**
 * Session Orchestrator Service
 * Manages the entire session lifecycle for AI card processing
 * Coordinates between LLM, file writing, state persistence, and WebSocket events
 */

import type { Note, SessionProgress } from '../types/index.js';
import { SessionState } from '../types/index.js';
import type { LLMService, NoteTypeInfo } from './llm/LLMService.js';
import { LLMServiceFactory } from './llm/LLMServiceFactory.js';
import { SuggestionWriter } from './SuggestionWriter.js';
import { SessionEventEmitter } from './SessionEventEmitter.js';
import { sessionService } from './SessionService.js';
import { cacheService } from './CacheService.js';
import { createTokenBasedBatches } from '../utils/tokenizer.js';

/**
 * Configuration for session orchestration
 */
interface OrchestratorConfig {
  maxInputTokens: number;
}

const DEFAULT_CONFIG: OrchestratorConfig = {
  maxInputTokens: 8000,
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
  private activeSessions: Set<string> = new Set();

  constructor(
    suggestionWriter: SuggestionWriter,
    eventEmitter: SessionEventEmitter,
    config: Partial<OrchestratorConfig> = {}
  ) {
    this.llmService = LLMServiceFactory.getInstance();
    this.suggestionWriter = suggestionWriter;
    this.eventEmitter = eventEmitter;
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  /**
   * Execute a session - main entry point
   */
  async executeSession(sessionId: string): Promise<SessionResult> {
    if (this.activeSessions.has(sessionId)) {
      throw new Error(`Session ${sessionId} is already running`);
    }

    this.activeSessions.add(sessionId);

    const result: SessionResult = {
      sessionId,
      totalCards: 0,
      processedCards: 0,
      suggestionsGenerated: 0,
      batchesProcessed: 0,
      batchesFailed: 0,
      errors: [],
    };

    try {
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
      await this.updateProgress(sessionId, 0, result.totalCards, 0);

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
      console.log(`[SessionOrchestrator] Created ${batches.length} batches for ${cards.length} cards`);

      // Process each batch
      for (let i = 0; i < batches.length; i++) {
        const batch = batches[i];
        console.log(`[SessionOrchestrator] Processing batch ${i + 1}/${batches.length} (${batch.length} cards)`);

        try {
          const suggestionsCount = await this.processBatch(
            sessionId,
            batch,
            request.prompt,
            noteType
          );

          result.batchesProcessed++;
          result.processedCards += batch.length;
          result.suggestionsGenerated += suggestionsCount;
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : String(error);
          console.error(`[SessionOrchestrator] Batch ${i + 1} failed: ${errorMessage}`);

          result.batchesFailed++;
          result.errors.push(`Batch ${i + 1}: ${errorMessage}`);
          result.processedCards += batch.length;
        }

        // Update progress after each batch
        await this.updateProgress(
          sessionId,
          result.processedCards,
          result.totalCards,
          result.suggestionsGenerated
        );
      }

      // Complete session
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
      this.activeSessions.delete(sessionId);
    }
  }

  /**
   * Update progress - saves to state.json and emits via WebSocket
   */
  private async updateProgress(
    sessionId: string,
    processed: number,
    total: number,
    suggestionsCount: number
  ): Promise<void> {
    const progress: SessionProgress = { processed, total, suggestionsCount };
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
   * Process a single batch of cards
   */
  private async processBatch(
    sessionId: string,
    batch: Note[],
    userPrompt: string,
    noteType: NoteTypeInfo
  ): Promise<number> {
    // Call LLM service
    const response = await this.llmService.analyzeBatch(batch, userPrompt, noteType);

    if (response.suggestions.length === 0) {
      console.log(`[SessionOrchestrator] Batch returned no suggestions`);
      return 0;
    }

    // Create lookup map for original notes
    const notesMap = new Map<number, Note>();
    for (const card of batch) {
      notesMap.set(card.noteId, card);
    }

    // Write suggestions to files
    const writtenSuggestions = await this.suggestionWriter.writeBatch(
      sessionId,
      response.suggestions,
      notesMap
    );

    // Emit each suggestion via WebSocket
    for (const suggestion of writtenSuggestions) {
      this.eventEmitter.emitSuggestion(sessionId, suggestion);
    }

    console.log(`[SessionOrchestrator] Batch generated ${writtenSuggestions.length} suggestions`);
    return writtenSuggestions.length;
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
   */
  cancelSession(sessionId: string): boolean {
    if (!this.activeSessions.has(sessionId)) {
      return false;
    }
    this.activeSessions.delete(sessionId);
    return true;
  }

  /**
   * Check if a session is currently running
   */
  isRunning(sessionId: string): boolean {
    return this.activeSessions.has(sessionId);
  }

  /**
   * Get list of active sessions
   */
  getActiveSessions(): string[] {
    return Array.from(this.activeSessions);
  }
}
