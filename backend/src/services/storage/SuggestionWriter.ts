/**
 * Suggestion Writer Service
 * Single responsibility: Write suggestions to files
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import type { Note, CardSuggestion } from '../../types/index.js';
import type { LLMSuggestion } from '../llm/LLMService.js';
import { AI_SESSIONS_DIR } from '../../constants.js';

/**
 * Handles writing LLM suggestions to the filesystem
 */
export class SuggestionWriter {
  /**
   * Write a suggestion to file
   * @returns The complete CardSuggestion that was written
   */
  async writeSuggestion(
    sessionId: string,
    llmSuggestion: LLMSuggestion,
    originalNote: Note
  ): Promise<CardSuggestion> {
    // Build complete CardSuggestion object
    const suggestion: CardSuggestion = {
      noteId: llmSuggestion.noteId,
      original: originalNote,
      changes: llmSuggestion.changes,
      reasoning: llmSuggestion.reasoning,
      accepted: null, // Pending review
    };

    // Write to file
    const suggestionsDir = path.join(AI_SESSIONS_DIR, sessionId, 'suggestions');
    await fs.mkdir(suggestionsDir, { recursive: true });

    const suggestionPath = path.join(
      suggestionsDir,
      `suggestion-${suggestion.noteId}.json`
    );
    await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2), 'utf-8');

    return suggestion;
  }

  /**
   * Write multiple suggestions from a batch
   * @returns Array of written CardSuggestions
   */
  async writeBatch(
    sessionId: string,
    llmSuggestions: LLMSuggestion[],
    notesMap: Map<number, Note>
  ): Promise<CardSuggestion[]> {
    const written: CardSuggestion[] = [];

    for (const llmSuggestion of llmSuggestions) {
      const originalNote = notesMap.get(llmSuggestion.noteId);

      if (!originalNote) {
        console.warn(
          `[SuggestionWriter] Original note not found for noteId ${llmSuggestion.noteId}, skipping`
        );
        continue;
      }

      const suggestion = await this.writeSuggestion(sessionId, llmSuggestion, originalNote);
      written.push(suggestion);
    }

    return written;
  }
}

// Singleton instance
export const suggestionWriter = new SuggestionWriter();
