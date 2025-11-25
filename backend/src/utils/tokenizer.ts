/**
 * Tokenizer Utility
 * Provides accurate token counting for LLM batching using tiktoken
 * Uses cl100k_base encoding (used by Claude and GPT-4)
 */

import { encodingForModel } from 'js-tiktoken';
import type { Note } from '../types/index.js';
import type { NoteTypeInfo } from '../services/llm/LLMService.js';
import { buildSystemPrompt } from '../services/llm/prompts/systemPrompt.js';
import { buildBatchPrompt } from '../services/llm/prompts/batchPrompt.js';

// Use cl100k_base encoding (Claude-compatible)
const encoding = encodingForModel('gpt-4');

/**
 * Count tokens in a string
 */
export function countTokens(text: string): number {
  return encoding.encode(text).length;
}

/**
 * Estimate tokens for a single card (just the card data, not full prompt)
 */
export function estimateCardTokens(card: Note): number {
  let text = `Card (ID: ${card.noteId}):\n`;

  for (const [fieldName, fieldData] of Object.entries(card.fields)) {
    const value = fieldData.value?.trim();
    if (value) {
      text += `  ${fieldName}: ${value}\n`;
    }
  }

  return countTokens(text);
}

/**
 * Calculate the base prompt tokens (system prompt + batch prompt header)
 * This is the fixed overhead for any batch
 */
export function calculateBasePromptTokens(
  userPrompt: string,
  noteType: NoteTypeInfo
): number {
  const systemPrompt = buildSystemPrompt();

  // Build a minimal batch prompt to get the header overhead
  const headerPrompt = `Note type: "${noteType.modelName}"
Available fields: [${noteType.fieldNames.map(f => `"${f}"`).join(', ')}]

User request: "${userPrompt}"

Cards to analyze:

Analyze these cards and suggest improvements following the user's request.
Return ONLY the JSON output, no other text.`;

  return countTokens(systemPrompt) + countTokens(headerPrompt);
}

/**
 * Create token-based batches of cards
 * Ensures each batch fits within the token limit
 */
export function createTokenBasedBatches(
  cards: Note[],
  userPrompt: string,
  noteType: NoteTypeInfo,
  maxInputTokens: number
): Note[][] {
  if (cards.length === 0) {
    return [];
  }

  const batches: Note[][] = [];
  const baseTokens = calculateBasePromptTokens(userPrompt, noteType);
  const availableTokens = maxInputTokens - baseTokens;

  if (availableTokens <= 0) {
    console.warn('[Tokenizer] Base prompt exceeds token limit, using single-card batches');
    return cards.map(card => [card]);
  }

  let currentBatch: Note[] = [];
  let currentBatchTokens = 0;

  for (const card of cards) {
    const cardTokens = estimateCardTokens(card);

    // If single card exceeds limit, it gets its own batch (will likely fail but let LLM try)
    if (cardTokens > availableTokens) {
      if (currentBatch.length > 0) {
        batches.push(currentBatch);
        currentBatch = [];
        currentBatchTokens = 0;
      }
      batches.push([card]);
      console.warn(`[Tokenizer] Card ${card.noteId} exceeds token limit (${cardTokens} > ${availableTokens})`);
      continue;
    }

    // Check if adding this card would exceed the limit
    if (currentBatchTokens + cardTokens > availableTokens) {
      // Start a new batch
      batches.push(currentBatch);
      currentBatch = [card];
      currentBatchTokens = cardTokens;
    } else {
      // Add to current batch
      currentBatch.push(card);
      currentBatchTokens += cardTokens;
    }
  }

  // Don't forget the last batch
  if (currentBatch.length > 0) {
    batches.push(currentBatch);
  }

  // Log batch statistics
  const batchSizes = batches.map(b => b.length);
  const avgBatchSize = batchSizes.reduce((a, b) => a + b, 0) / batches.length;
  console.log(
    `[Tokenizer] Created ${batches.length} batches from ${cards.length} cards ` +
    `(avg ${avgBatchSize.toFixed(1)} cards/batch, base tokens: ${baseTokens}, max: ${maxInputTokens})`
  );

  return batches;
}

/**
 * Estimate total tokens for a full batch prompt
 * Useful for debugging and optimization
 */
export function estimateFullBatchTokens(
  cards: Note[],
  userPrompt: string,
  noteType: NoteTypeInfo
): number {
  const fullPrompt = buildSystemPrompt() + '\n\n---\n\n' + buildBatchPrompt(cards, userPrompt, noteType);
  return countTokens(fullPrompt);
}

/**
 * Estimate total tokens for all cards in a deck using actual prompt formatting
 * Used to give users an estimation before running a session
 */
export function estimateDeckTokens(cards: Note[]): number {
  if (cards.length === 0) return 0;

  // Extract note type from first card
  const noteType: NoteTypeInfo = {
    modelName: cards[0].modelName,
    fieldNames: Object.keys(cards[0].fields),
  };

  // Use actual buildBatchPrompt with placeholder prompt
  const batchPrompt = buildBatchPrompt(cards, '', noteType);
  return countTokens(batchPrompt);
}
