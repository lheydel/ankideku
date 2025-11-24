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

Analyze Anki flashcards and suggest improvements based on the user's request.

## User's Request
"${request.prompt}"

## Session Info
- Session: ${request.sessionId}
- Deck: ${request.deckName} (${request.totalCards} cards across ${request.deckPaths.length} file${request.deckPaths.length > 1 ? 's' : ''})
- Files: ${deckFilesList}

## File Path Rules
**Your working directory:**
- Suggestions: \`${suggestionsPath}/suggestion-{noteId}.json\`
- Temp/scratch files: \`${suggestionsPath}/../tmp/{anything}\` (scripts, notes, coordination files, etc.)

**NEVER write anywhere outside your working directory** Files outside your working directory will fail the task. THIS IS CRITICAL SECURITY RULE.

## Task

${request.totalCards > 50 ? `### Processing Strategy (${request.totalCards} cards)
${request.totalCards > 1000 ? `**Large deck** - Use parallel processing:
1. Read all deck files to analyze card distribution
2. Divide into ~1000-card chunks
3. Spawn ${Math.max(2, Math.min(8, Math.ceil(request.totalCards / 1000)))} parallel Task agents in ONE message (subagent_type: "general-purpose")
4. Each agent processes its assigned chunk (use \`${suggestionsPath}/../tmp/\` for coordination)
5. If any agent hits token limits, it spawns a continuation agent

` : `**Medium deck** - Spawn 1 Task agent (subagent_type: "general-purpose") to handle processing. Use \`${suggestionsPath}/../tmp/\` for checkpoints.

`}` : ''}1. Read deck files (JSON format: \`{ deckName, notes: [...] }\`)
2. For each note needing changes, write a suggestion file immediately (enables real-time updates)

## Suggestion Format

Write to: \`${suggestionsPath}/suggestion-{noteId}.json\`

**Exact structure:**
\`\`\`json
{
  "noteId": 12345,
  "original": { /* COMPLETE note object from deck file */ },
  "changes": { "FieldName": "new value" },
  "reasoning": "Why you changed it"
}
\`\`\`

**Critical:**
- \`original\`: Full note object with all fields (noteId, modelName, fields, tags, cards, mod, deckName)
- \`changes\`: Simple \`{ field: value }\` - NOT \`{ field: { oldValue, newValue } }\`
- \`reasoning\`: NOT "explanation"
- Write files ONE AT A TIME for real-time updates (file watcher monitors directory)

## Rules
- This structure will be parsed automatically - FOLLOW IT EXACTLY
- Create \`suggestion-{noteId}.json\` for each changed card (ONLY these are read by the user)
- The user will receive ALL suggestion files you create in real-time, so a suggestion file should be the final version for that note
- You can create helper files in \`tmp/\` (scripts, notes, checkpoints, etc.) but they won't be read
- Write suggestions incrementally (one at a time) for real-time updates
- Process ALL ${request.totalCards} cards
- YOU CANNOT WRITE OUTSIDE YOUR WORKING DIRECTORY - doing so will fail the task and this is true for ALL subagents spawned. This is a CRITICAL SECURITY RULE.

${request.totalCards > 100 ? `## Token Limits
If approaching 150k tokens:
1. Save checkpoint to \`${suggestionsPath}/../tmp/checkpoint.json\`
2. Spawn continuation agent: "Continue session ${request.sessionId}. Read checkpoint and resume."
3. Continue until ALL cards processed

` : ''}Start now.`;
  }

}
