/**
 * Mock LLM Service
 * Test implementation that parses prompts and generates random suggestions
 */

import type { LLMHealthStatus } from './LLMService.js';
import { BaseLLMService } from './BaseLLMService.js';
import { CONFIG } from '../../config.js';

export class MockLLMService extends BaseLLMService {
  async getHealth(): Promise<LLMHealthStatus> {
    return {
      available: true,
      info: 'Mock LLM Service (Testing)',
    };
  }

  protected async callLLM(prompt: string, signal?: AbortSignal): Promise<string> {
    // Simulate response delay
    await this.delay(CONFIG.mock.responseDelay, signal);

    // Parse the prompt to extract noteIds and field names
    const { noteIds, fieldNames } = this.parsePrompt(prompt);

    // Generate suggestions based on suggestionRate
    const suggestions = [];
    for (const noteId of noteIds) {
      if (Math.random() < CONFIG.mock.suggestionRate) {
        // Pick a random field to modify
        const fieldName = fieldNames[Math.floor(Math.random() * fieldNames.length)];
        if (fieldName) {
          suggestions.push({
            noteId,
            changes: {
              [fieldName]: this.generateRandomValue(fieldName),
            },
            reasoning: `Mock: updated ${fieldName} field`,
          });
        }
      }
    }

    return JSON.stringify(suggestions);
  }

  /**
   * Parse the prompt to extract noteIds and field names
   */
  private parsePrompt(prompt: string): { noteIds: number[]; fieldNames: string[] } {
    const noteIds: number[] = [];
    const fieldNames: string[] = [];

    // Extract noteIds from "Card N (ID: XXX):" pattern
    const noteIdMatches = prompt.matchAll(/Card \d+ \(ID: (\d+)\):/g);
    for (const match of noteIdMatches) {
      noteIds.push(parseInt(match[1], 10));
    }

    // Extract field names from "Available fields: [...]" pattern
    const fieldsMatch = prompt.match(/Available fields:\s*\[([^\]]+)\]/);
    if (fieldsMatch) {
      const fieldsStr = fieldsMatch[1];
      const fieldMatches = fieldsStr.matchAll(/"([^"]+)"/g);
      for (const match of fieldMatches) {
        fieldNames.push(match[1]);
      }
    }

    return { noteIds, fieldNames };
  }

  /**
   * Generate a random value for a field
   */
  private generateRandomValue(fieldName: string): string {
    const randomId = Math.random().toString(36).slice(2, 10);
    return `Mock ${fieldName} - ${randomId}`;
  }

  /**
   * Delay helper that respects abort signal
   */
  private delay(ms: number, signal?: AbortSignal): Promise<void> {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(resolve, ms);

      if (signal) {
        signal.addEventListener('abort', () => {
          clearTimeout(timeout);
          const error = new Error('Session cancelled');
          error.name = 'AbortError';
          reject(error);
        }, { once: true });
      }
    });
  }
}
