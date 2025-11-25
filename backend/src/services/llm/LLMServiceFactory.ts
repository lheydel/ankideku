/**
 * LLM Service Factory
 * Creates the appropriate LLM service based on configuration
 * Currently supports Claude Code, extensible for future providers
 */

import type { LLMService, LLMConfig, LLMProvider } from './LLMService.js';
import { ClaudeCodeService } from './ClaudeCodeService.js';
import { DEFAULT_LLM_CONFIG } from './LLMService.js';

/**
 * Factory for creating LLM service instances
 */
export class LLMServiceFactory {
  private static instance: LLMService | null = null;
  private static currentProvider: LLMProvider | null = null;

  /**
   * Create an LLM service based on the provided configuration
   * @param config LLM configuration
   * @returns LLMService instance
   */
  static create(config: LLMConfig = DEFAULT_LLM_CONFIG): LLMService {
    switch (config.provider) {
      case 'claude-code':
        return new ClaudeCodeService();

      // case 'claude-api':
      //   // Future: Implement ClaudeAPIService
      //   throw new Error(
      //     'Claude API provider is not yet implemented. Please use claude-code for now.'
      //   );
      //
      // case 'openai':
      //   // Future: Implement OpenAIService
      //   throw new Error(
      //     'OpenAI provider is not yet implemented. Please use claude-code for now.'
      //   );

      default:
        throw new Error(`Unknown LLM provider: ${config.provider}`);
    }
  }

  /**
   * Get or create a singleton LLM service instance
   * Recreates if provider changes
   * @param config LLM configuration
   * @returns LLMService instance
   */
  static getInstance(config: LLMConfig = DEFAULT_LLM_CONFIG): LLMService {
    // Recreate if provider changed
    if (this.currentProvider !== config.provider) {
      this.instance = null;
    }

    if (!this.instance) {
      this.instance = this.create(config);
      this.currentProvider = config.provider;
    }

    return this.instance;
  }

  /**
   * Clear the singleton instance
   * Useful for testing or when config changes
   */
  static clearInstance(): void {
    this.instance = null;
    this.currentProvider = null;
  }

  /**
   * Get list of available providers
   */
  static getAvailableProviders(): LLMProvider[] {
    return ['claude-code']; // Add more as they're implemented
  }

  /**
   * Check if a provider is available (implemented)
   */
  static isProviderAvailable(provider: LLMProvider): boolean {
    return this.getAvailableProviders().includes(provider);
  }
}
