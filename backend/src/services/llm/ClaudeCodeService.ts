/**
 * Claude Code Service
 * Implements LLMService by spawning Claude Code CLI subprocess
 * Sends clean prompts (no file I/O instructions) and parses JSON responses
 */

import { ChildProcess, execSync } from 'child_process';
import spawn from 'cross-spawn';
import type { Note } from '../../types/index.js';
import type { LLMHealthStatus, LLMResponse, LLMService, NoteTypeInfo } from './LLMService.js';
import { buildSystemPrompt } from './prompts/systemPrompt.js';
import { buildBatchPrompt } from './prompts/batchPrompt.js';
import { ResponseParser } from './ResponseParser.js';
import { countTokens } from '../../utils/tokenizer.js';

const DEFAULT_TIMEOUT = 5 * 60 * 1000; // 5 minutes
const MAX_RETRIES = 2;

/**
 * Check if Claude CLI is installed
 */
function isClaudeInstalled(): boolean {
  try {
    const command = process.platform === 'win32' ? 'where claude' : 'which claude';
    execSync(command, { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

/**
 * Get Claude CLI version
 */
function getClaudeVersion(): string | null {
  try {
    const result = execSync('claude --version', { encoding: 'utf-8' });
    return result.trim();
  } catch {
    return null;
  }
}

export class ClaudeCodeService implements LLMService {
  private activeProcesses: Map<string, ChildProcess> = new Map();
  private parser: ResponseParser;

  constructor() {
    this.parser = new ResponseParser();
  }

  async checkHealth(): Promise<LLMHealthStatus> {
    if (!isClaudeInstalled()) {
      return {
        available: false,
        error: 'Claude Code CLI is not installed or not in PATH',
      };
    }

    const version = getClaudeVersion();
    return {
      available: true,
      info: version ? `Claude Code ${version} installed` : 'Claude Code installed',
    };
  }

  async analyzeBatch(
    cards: Note[],
    userPrompt: string,
    noteType: NoteTypeInfo
  ): Promise<LLMResponse> {
    const health = await this.checkHealth();
    if (!health.available) {
      throw new Error(health.error || 'Claude Code not available');
    }

    // Build prompts
    const systemPrompt = buildSystemPrompt();
    const batchPrompt = buildBatchPrompt(cards, userPrompt, noteType);

    // Combine into single prompt for Claude Code
    const fullPrompt = `${systemPrompt}\n\n---\n\n${batchPrompt}`;

    // Estimate input tokens
    const inputTokens = countTokens(fullPrompt);

    // Try with retries on all errors
    let lastError: Error | null = null;
    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      try {
        const rawResponse = await this.spawnClaudeCode(fullPrompt);
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
        lastError = error instanceof Error ? error : new Error(String(error));
        console.log(
          `[ClaudeCodeService] Error on attempt ${attempt + 1}/${MAX_RETRIES + 1}: ${lastError.message}`
        );

        if (attempt < MAX_RETRIES) {
          console.log('[ClaudeCodeService] Retrying...');
        }
      }
    }

    throw lastError || new Error('Failed to analyze batch after max retries');
  }

  private async spawnClaudeCode(prompt: string): Promise<string> {
    // Generate unique ID for this process
    const processId = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;

    return new Promise((resolve, reject) => {
      // Spawn claude CLI in non-interactive mode
      // Using --print to run non-interactively
      // Using --output-format text to get plain text response
      const claude = spawn(
        'claude',
        [
          '--print',
          '--model', 'sonnet',
          '--output-format', 'text', // We want clean text output, not streaming JSON
        ],
        {
          cwd: process.cwd(),
          env: process.env,
          stdio: ['pipe', 'pipe', 'pipe'],
        }
      );

      this.activeProcesses.set(processId, claude);

      let stdoutData = '';
      let stderrData = '';

      // Pipe the prompt to stdin
      if (claude.stdin) {
        claude.stdin.write(prompt);
        claude.stdin.end();
      }

      // Capture stdout
      claude.stdout?.on('data', (data: Buffer) => {
        stdoutData += data.toString();
      });

      // Capture stderr
      claude.stderr?.on('data', (data: Buffer) => {
        stderrData += data.toString();
      });

      // Handle process completion
      claude.on('close', (code: number | null) => {
        this.activeProcesses.delete(processId);

        if (code === 0) {
          resolve(stdoutData);
        } else {
          reject(
            new Error(
              stderrData || `Claude Code exited with code ${code}`
            )
          );
        }
      });

      // Handle process errors
      claude.on('error', (error: Error) => {
        this.activeProcesses.delete(processId);
        reject(error);
      });

      // Timeout
      setTimeout(() => {
        if (this.activeProcesses.has(processId)) {
          this.killProcess(processId);
          reject(new Error(`Claude Code timed out after ${DEFAULT_TIMEOUT}ms`));
        }
      }, DEFAULT_TIMEOUT);
    });
  }

  /**
   * Kill a specific Claude process by ID
   */
  killProcess(processId: string): void {
    const proc = this.activeProcesses.get(processId);
    if (proc) {
      try {
        proc.kill('SIGTERM');
      } catch {
        // Ignore - process may already be dead
      }
      this.activeProcesses.delete(processId);
    }
  }

  /**
   * Kill all active Claude processes
   */
  killAll(): void {
    for (const [processId, proc] of this.activeProcesses) {
      try {
        proc.kill('SIGTERM');
      } catch {
        // Ignore - process may already be dead
      }
    }
    this.activeProcesses.clear();
  }
}
