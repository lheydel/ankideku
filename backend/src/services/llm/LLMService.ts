/**
 * LLM Service Interface
 * Provider-agnostic interface for LLM interactions
 * Implementations can use Claude Code, API calls, local models, etc.
 */

import type { Note } from '../../types/index.js';

/**
 * Suggestion returned by LLM analysis
 * Note: `original` is NOT included - backend enriches from cache
 */
export interface LLMSuggestion {
  noteId: number;
  changes: Record<string, string>;
  reasoning: string;
}

/**
 * Response from LLM batch analysis
 */
export interface LLMResponse {
  suggestions: LLMSuggestion[];
  usage?: {
    inputTokens: number;
    outputTokens: number;
  };
}

/**
 * Health check status for LLM provider
 */
export interface LLMHealthStatus {
  available: boolean;
  error?: string;
  info?: string; // e.g., "Claude Code v1.2.3 installed"
}

/**
 * Note type model information for prompt building
 */
export interface NoteTypeInfo {
  modelName: string;
  fieldNames: string[];
}

/**
 * Provider-agnostic interface for LLM interactions
 */
export interface LLMService {
  /**
   * Analyze a batch of cards and return suggestions
   * @param cards Array of notes to analyze
   * @param userPrompt User's instruction for improvements
   * @param noteType Information about the note type (available fields)
   * @param signal Optional AbortSignal for cancellation
   */
  analyzeBatch(
    cards: Note[],
    userPrompt: string,
    noteType: NoteTypeInfo,
    signal?: AbortSignal
  ): Promise<LLMResponse>;

  /**
   * Check if the LLM provider is available and configured
   */
  getHealth(): Promise<LLMHealthStatus>;
}

/**
 * Supported LLM providers
 */
export type LLMProvider = 'claude-code' | 'mock'; //| 'claude-api' | 'openai';

/**
 * LLM configuration stored in settings
 */
export interface LLMConfig {
  provider: LLMProvider;
  apiKey?: string;
  model?: string;
}

/**
 * Default LLM configuration
 */
export const DEFAULT_LLM_CONFIG: LLMConfig = {
  provider: 'claude-code',
};
