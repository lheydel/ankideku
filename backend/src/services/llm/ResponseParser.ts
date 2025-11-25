/**
 * Response Parser for LLM outputs
 * Handles parsing, validation, and error recovery for LLM responses
 */

import type { Note } from '../../types/index.js';
import type { LLMResponse, LLMSuggestion } from './LLMService.js';

/**
 * Raw suggestion from LLM (before validation)
 */
interface RawSuggestion {
  noteId?: unknown;
  changes?: unknown;
  reasoning?: unknown;
}

/**
 * Raw response structure from LLM
 */
interface RawResponse {
  suggestions?: unknown[];
}

export class ResponseParser {
  /**
   * Parse raw LLM response into validated LLMResponse
   * @param rawResponse Raw string response from LLM
   * @param batchCards Cards that were in this batch (for noteId validation)
   */
  parse(rawResponse: string, batchCards: Note[]): LLMResponse {
    // Extract JSON from response
    const jsonObject = this.extractJSON(rawResponse);

    // Validate and filter suggestions
    const validNoteIds = new Set(batchCards.map((c) => c.noteId));
    const suggestions = this.validateSuggestions(jsonObject, validNoteIds);

    return { suggestions };
  }

  /**
   * Extract JSON object from raw text
   * Handles cases where LLM includes extra text around JSON
   */
  private extractJSON(text: string): RawResponse {
    const trimmed = text.trim();

    // Try parsing as-is first
    try {
      return JSON.parse(trimmed);
    } catch {
      // Continue to extraction attempts
    }

    // Try to find JSON object in the text
    const jsonMatch = trimmed.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      try {
        return JSON.parse(jsonMatch[0]);
      } catch {
        // Continue to other attempts
      }
    }

    // Try to find JSON between code fences
    const codeFenceMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/);
    if (codeFenceMatch) {
      try {
        return JSON.parse(codeFenceMatch[1].trim());
      } catch {
        // Continue
      }
    }

    throw new Error(`Failed to parse JSON from LLM response: ${trimmed.substring(0, 200)}...`);
  }

  /**
   * Validate and filter suggestions
   */
  private validateSuggestions(
    response: RawResponse,
    validNoteIds: Set<number>
  ): LLMSuggestion[] {
    if (!response.suggestions || !Array.isArray(response.suggestions)) {
      // Empty response is valid (no suggestions needed)
      if (response.suggestions === undefined) {
        console.warn('[ResponseParser] Response missing suggestions array, treating as empty');
        return [];
      }
      throw new Error('Response suggestions is not an array');
    }

    const validSuggestions: LLMSuggestion[] = [];

    for (const raw of response.suggestions) {
      const suggestion = raw as RawSuggestion;

      // Parse and validate noteId
      let noteId: number;
      if (typeof suggestion.noteId === 'number') {
        noteId = suggestion.noteId;
      } else if (typeof suggestion.noteId === 'string') {
        // Try to parse stringified number
        const parsed = parseInt(suggestion.noteId, 10);
        if (isNaN(parsed)) {
          console.warn('[ResponseParser] Skipping suggestion with non-numeric noteId string:', suggestion.noteId);
          continue;
        }
        noteId = parsed;
      } else {
        console.warn('[ResponseParser] Skipping suggestion with invalid noteId:', suggestion.noteId);
        continue;
      }

      if (!validNoteIds.has(noteId)) {
        console.warn(
          '[ResponseParser] Skipping suggestion with noteId not in batch:',
          noteId
        );
        continue;
      }

      // Validate changes
      if (
        !suggestion.changes ||
        typeof suggestion.changes !== 'object' ||
        Array.isArray(suggestion.changes)
      ) {
        console.warn(
          '[ResponseParser] Skipping suggestion with invalid changes:',
          noteId
        );
        continue;
      }

      // Ensure all change values are strings
      const changes: Record<string, string> = {};
      let hasValidChanges = false;

      for (const [field, value] of Object.entries(suggestion.changes as Record<string, unknown>)) {
        if (typeof value === 'string') {
          changes[field] = value;
          hasValidChanges = true;
        } else if (value !== null && value !== undefined) {
          // Convert non-string values to strings
          changes[field] = String(value);
          hasValidChanges = true;
        }
      }

      if (!hasValidChanges) {
        console.warn(
          '[ResponseParser] Skipping suggestion with no valid changes:',
          noteId
        );
        continue;
      }

      // Validate reasoning (optional but encouraged)
      const reasoning =
        typeof suggestion.reasoning === 'string'
          ? suggestion.reasoning
          : 'No reasoning provided';

      validSuggestions.push({
        noteId,
        changes,
        reasoning,
      });
    }

    return validSuggestions;
  }
}
