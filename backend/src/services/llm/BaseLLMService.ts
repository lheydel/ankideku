/**
 * Base LLM Service
 * Abstract class implementing common logic for all LLM providers
 * Subclasses only need to implement callLLM() and getHealth()
 */

import type { Note } from '../../types/index.js';
import type { LLMHealthStatus, LLMResponse, LLMService, NoteTypeInfo } from './LLMService.js';
import { buildSystemPrompt } from './prompts/systemPrompt.js';
import { buildBatchPrompt } from './prompts/batchPrompt.js';
import { ResponseParser } from './ResponseParser.js';
import { countTokens } from '../../utils/tokenizer.js';
import { CONFIG } from '../../config.js';

export abstract class BaseLLMService implements LLMService {
  protected parser: ResponseParser;

  constructor() {
    this.parser = new ResponseParser();
  }

  /**
   * Throw AbortError if signal is aborted
   */
  protected throwIfAborted(signal?: AbortSignal): void {
    if (signal?.aborted) {
      const error = new Error('Session cancelled');
      error.name = 'AbortError';
      throw error;
    }
  }

  /**
   * Analyze a batch of cards and return suggestions
   * Handles prompt building, retry logic, parsing, and token counting
   */
  async analyzeBatch(
    cards: Note[],
    userPrompt: string,
    noteType: NoteTypeInfo,
    signal?: AbortSignal
  ): Promise<LLMResponse> {
    // Check if already aborted
    this.throwIfAborted(signal);

    // Build prompts
    const systemPrompt = buildSystemPrompt();
    const batchPrompt = buildBatchPrompt(cards, userPrompt, noteType);

    // Combine into single prompt
    const fullPrompt = `${systemPrompt}\n\n---\n\n${batchPrompt}`;

    // Estimate input tokens
    const inputTokens = countTokens(fullPrompt);

    // Try with retries on all errors
    let lastError: Error | null = null;
    for (let attempt = 0; attempt <= CONFIG.llm.maxRetries; attempt++) {
      // Check abort before each attempt
      this.throwIfAborted(signal);

      try {
        const rawResponse = await this.callLLM(fullPrompt, signal);
        const result = this.parser.parse(rawResponse, cards);

        // Estimate output tokens from raw response
        const outputTokens = countTokens(rawResponse);

        return {
          ...result,
          usage: {
            inputTokens,
            outputTokens,
          },
        };
      } catch (error) {
        // Re-throw AbortError immediately (don't retry on cancellation)
        if (error instanceof Error && error.name === 'AbortError') {
          throw error;
        }

        lastError = error instanceof Error ? error : new Error(String(error));
        console.log(
          `[${this.constructor.name}] Error on attempt ${attempt + 1}/${CONFIG.llm.maxRetries + 1}: ${lastError.message}`
        );

        if (attempt < CONFIG.llm.maxRetries) {
          console.log(`[${this.constructor.name}] Retrying...`);
        }
      }
    }

    throw lastError || new Error('Failed to analyze batch after max retries');
  }

  /**
   * Provider-specific LLM call
   * @param prompt The full prompt to send to the LLM
   * @param signal Optional AbortSignal for cancellation
   * @returns Raw response string from the LLM
   */
  protected abstract callLLM(prompt: string, signal?: AbortSignal): Promise<string>;

  /**
   * Check if the LLM provider is available and configured
   */
  abstract getHealth(): Promise<LLMHealthStatus>;
}
