import * as fs from 'fs/promises';
import * as path from 'path';
import { PromptGenerator } from './promptGenerator.js';
import type { SessionRequest } from '../types/index.js';

/**
 * Processor that generates prompts for Claude Code to analyze cards
 *
 * This processor:
 * 1. Reads the session request
 * 2. Loads card data from the deck
 * 3. Generates a prompt for Claude Code
 * 4. Writes the prompt to claude-task.md
 * 5. Outputs instructions for invoking Claude Code
 */
class ClaudeProcessor {
  private sessionPath: string;
  private sessionId: string;

  constructor(sessionPath: string) {
    this.sessionPath = sessionPath;
    this.sessionId = path.basename(sessionPath);
  }

  /**
   * Main processing function
   */
  async process(): Promise<void> {
    console.log(`[${this.sessionId}] Preparing task for Claude Code...`);

    try {
      // Read request
      const request = await this.readRequest();
      console.log(`[${this.sessionId}] User prompt: "${request.prompt}"`);
      console.log(`[${this.sessionId}] Deck: ${request.deckName} (${request.totalCards} cards)`);

      // Generate prompt for Claude Code
      const prompt = PromptGenerator.generatePrompt(request);

      // Write prompt file
      const promptPath = path.join(this.sessionPath, 'claude-task.md');
      await fs.writeFile(promptPath, prompt, 'utf-8');

      console.log(`[${this.sessionId}] Prompt written to: ${promptPath}`);
      console.log(`\n${'='.repeat(80)}`);
      console.log(`READY FOR CLAUDE CODE PROCESSING`);
      console.log(`${'='.repeat(80)}`);
      console.log(`\nSession: ${this.sessionId}`);
      console.log(`Task file: ${promptPath}`);
      console.log(`Deck: ${request.deckName}`);
      console.log(`Deck files: ${request.deckPaths.length}`);
      request.deckPaths.forEach((p, i) => {
        console.log(`  ${i + 1}. ${p}`);
      });
      console.log(`\nClaude Code will:`);
      console.log(`  1. Read the task file: ${promptPath}`);
      console.log(`  2. Read all ${request.deckPaths.length} deck file(s)`);
      console.log(`  3. Analyze ${request.totalCards} cards total`);
      console.log(`  4. Write suggestion files to: ${this.sessionPath}/`);
      console.log(`\nThe backend is watching for suggestion files and will stream them to the frontend.`);
      console.log(`${'='.repeat(80)}\n`);

    } catch (error) {
      console.error(`[${this.sessionId}] Failed to prepare task:`, error);
      throw error;
    }
  }

  /**
   * Read request.json
   */
  private async readRequest(): Promise<SessionRequest> {
    const requestPath = path.join(this.sessionPath, 'request.json');
    const content = await fs.readFile(requestPath, 'utf-8');
    return JSON.parse(content);
  }
}

// CLI entry point
async function main() {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    console.error('Usage: node claudeProcessor.js <sessionPath>');
    process.exit(1);
  }

  const sessionPath = args[0];

  // Validate session path
  try {
    await fs.access(sessionPath);
  } catch (error) {
    console.error(`Error: Session path does not exist: ${sessionPath}`);
    process.exit(1);
  }

  const processor = new ClaudeProcessor(sessionPath);

  try {
    await processor.process();
    process.exit(0);
  } catch (error) {
    console.error('Fatal error:', error);
    process.exit(1);
  }
}

// Run if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}

export { ClaudeProcessor };
