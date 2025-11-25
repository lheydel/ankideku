/**
 * LLM Service Module
 * Provider-agnostic interface for LLM interactions
 */

// Types and interfaces
export type {
  LLMService,
  LLMResponse,
  LLMSuggestion,
  LLMHealthStatus,
  LLMConfig,
  LLMProvider,
  NoteTypeInfo,
} from './LLMService.js';

export { DEFAULT_LLM_CONFIG } from './LLMService.js';

// Factory
export { LLMServiceFactory } from './LLMServiceFactory.js';

// Implementations
export { ClaudeCodeService } from './ClaudeCodeService.js';

// Parser
export { ResponseParser } from './ResponseParser.js';

// Prompts
export { buildSystemPrompt } from './prompts/systemPrompt.js';
export { buildBatchPrompt } from './prompts/batchPrompt.js';
