import * as fs from 'fs/promises';
import * as path from 'path';
import type { Note, SessionRequest } from '../types/index.js';

interface DeckData {
  deckName: string;
  notes: Note[];
  timestamp: string;
  count: number;
}

/**
 * Generates prompts for Claude Code to process cards
 */
export class PromptGenerator {
  /**
   * Generate a comprehensive prompt for Claude Code
   */
  static generatePrompt(request: SessionRequest): string {
    const deckFilesList = request.deckPaths.map((p, i) => `${i + 1}. \`${p}\``).join('\n');

    // Build the full path for suggestions directory
    const suggestionsPath = `database/ai-sessions/${request.sessionId}/suggestions`;

    return `# Anki Card Analysis Task

You are analyzing Anki flashcards to suggest improvements based on the user's request.

## User's Request
"${request.prompt}"

## Session Information
- **Session ID:** ${request.sessionId}
- **Deck:** ${request.deckName}
- **Total Cards:** ${request.totalCards}
- **Number of Deck Files:** ${request.deckPaths.length}

## Deck Files to Process

${deckFilesList}

${request.deckPaths.length > 1 ?
`**Note:** This deck has ${request.deckPaths.length} subdecks. You need to process ALL of them.` :
`**Note:** Single deck file.`}

## Your Task

1. **Read all deck files** listed above
   - Each file contains cards in JSON format
   - Structure: \`{ deckName, notes: [...], timestamp, count }\`

2. **For each deck file:**
   - Parse the JSON and get the \`notes\` array
   - Analyze each card based on the user's request

3. **For each card needing changes**, write a suggestion file

## Output Format

For each card that needs improvement, create a file named:
\`${suggestionsPath}/suggestion-{noteId}.json\`

With this exact JSON structure:
\`\`\`json
{
  "noteId": 12345,
  "original": {
    "noteId": 12345,
    "modelName": "Basic",
    "fields": {
      "Front": { "value": "original text", "order": 0 },
      "Back": { "value": "original text", "order": 1 }
    },
    "tags": ["tag1"],
    "cards": [1234567890],
    "mod": 1234567890,
    "deckName": "JP Voc::JP Voc 01"
  },
  "changes": {
    "Front": "corrected text"
  },
  "reasoning": "Explain what you changed and why"
}
\`\`\`

**IMPORTANT:**
- Only create files for cards that NEED changes
- Include the FULL original card data in "original"
- Only include changed fields in "changes"
- Provide clear, specific reasoning for each change
- Write files to: \`${suggestionsPath}/\`

## Instructions

**IMPORTANT: Write suggestion files incrementally, one at a time!**
There is a file watcher monitoring the suggestions directory in real-time. Each time you write a suggestion file, it will be immediately processed and sent to the user interface for review. This provides real-time progress feedback.

1. For each deck file in the list above:
   - Use the Read tool to load the file
   - Parse the JSON and get the \`notes\` array
   - Process notes in that deck ONE AT A TIME

2. For each note across ALL decks:
   - Analyze based on: "${request.prompt}"
   - **If changes are needed:**
     - IMMEDIATELY use the Write tool to create \`${suggestionsPath}/suggestion-{noteId}.json\`
     - Do NOT wait to analyze other cards first
     - Write the file right away so the file watcher can detect it
   - If no changes needed, skip to the next card

3. Process all ${request.totalCards} cards across all ${request.deckPaths.length} deck file(s)

**Remember**: Write each suggestion file immediately after analyzing that card, not in a batch at the end. This enables real-time progress updates!

Start analyzing now!`;
  }

}
