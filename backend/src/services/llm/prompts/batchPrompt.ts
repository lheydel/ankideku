/**
 * Batch Prompt Builder
 * Formats card data efficiently for LLM analysis
 * Includes note type schema and omits empty fields to save tokens
 */

import type { Note } from '../../../types/index.js';
import type { NoteTypeInfo } from '../LLMService.js';

/**
 * Format a single card for the prompt
 * Omits empty fields to save tokens
 */
function formatCard(card: Note, index: number): string {
  const lines: string[] = [`Card ${index + 1} (ID: ${card.noteId}):`];

  // Add non-empty fields only
  for (const [fieldName, fieldData] of Object.entries(card.fields)) {
    const value = fieldData.value?.trim();
    if (value) {
      // Indent field content and handle multiline values
      const formattedValue = value.includes('\n')
        ? `\n    ${value.split('\n').join('\n    ')}`
        : value;
      lines.push(`  ${fieldName}: ${formattedValue}`);
    }
  }

  return lines.join('\n');
}

/**
 * Build the batch prompt for LLM analysis
 * @param cards Array of notes to analyze
 * @param userPrompt User's instruction for improvements
 * @param noteType Information about the note type (all available fields)
 */
export function buildBatchPrompt(
  cards: Note[],
  userPrompt: string,
  noteType: NoteTypeInfo
): string {
  const formattedCards = cards.map((card, idx) => formatCard(card, idx)).join('\n\n');

  return `Note type: "${noteType.modelName}"
Available fields: [${noteType.fieldNames.map((f) => `"${f}"`).join(', ')}]

User request: "${userPrompt}"

Cards to analyze:
${formattedCards}

Analyze these cards and suggest improvements following the user's request.
Return ONLY the JSON output, no other text.`;
}
