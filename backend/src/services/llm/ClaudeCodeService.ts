/**
 * Claude Code Service
 * Implements LLMService by spawning Claude Code CLI subprocess
 * Sends clean prompts (no file I/O instructions) and parses JSON responses
 */

import { ChildProcess, exec } from 'child_process';
import { promisify } from 'util';
import spawn from 'cross-spawn';
import type { Note } from '../../types/index.js';
import type { LLMHealthStatus, LLMResponse, LLMService, NoteTypeInfo } from './LLMService.js';
import { buildSystemPrompt } from './prompts/systemPrompt.js';
import { buildBatchPrompt } from './prompts/batchPrompt.js';
import { ResponseParser } from './ResponseParser.js';
import { countTokens } from '../../utils/tokenizer.js';
import { CONFIG } from '../../config.js';

const execAsync = promisify(exec);

/**
 * Check if Claude CLI is installed (async - doesn't block event loop)
 */
async function isClaudeInstalled(): Promise<boolean> {
  try {
    const command = process.platform === 'win32' ? 'where claude' : 'which claude';
    await execAsync(command);
    return true;
  } catch {
    return false;
  }
}

/**
 * Get Claude CLI version (async - doesn't block event loop)
 */
async function getClaudeVersion(): Promise<string | null> {
  try {
    const { stdout } = await execAsync('claude --version');
    return stdout.trim();
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

  /**
   * Throw AbortError if signal is aborted
   */
  private throwIfAborted(signal?: AbortSignal): void {
    if (signal?.aborted) {
      const error = new Error('Session cancelled');
      error.name = 'AbortError';
      throw error;
    }
  }

  async getHealth(): Promise<LLMHealthStatus> {
    if (!(await isClaudeInstalled())) {
      return {
        available: false,
        error: 'Claude Code CLI is not installed or not in PATH',
      };
    }

    const version = await getClaudeVersion();
    return {
      available: true,
      info: version ? `Claude Code ${version} installed` : 'Claude Code installed',
    };
  }

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

    // Combine into single prompt for Claude Code
    const fullPrompt = `${systemPrompt}\n\n---\n\n${batchPrompt}`;

    // Estimate input tokens
    const inputTokens = countTokens(fullPrompt);

    // Try with retries on all errors
    let lastError: Error | null = null;
    for (let attempt = 0; attempt <= CONFIG.llm.maxRetries; attempt++) {
      // Check abort before each attempt
      this.throwIfAborted(signal);

      try {
        const rawResponse = await this.spawnClaudeCode(fullPrompt, signal);
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
          `[ClaudeCodeService] Error on attempt ${attempt + 1}/${CONFIG.llm.maxRetries + 1}: ${lastError.message}`
        );

        if (attempt < CONFIG.llm.maxRetries) {
          console.log('[ClaudeCodeService] Retrying...');
        }
      }
    }

    throw lastError || new Error('Failed to analyze batch after max retries');
  }

  private async spawnClaudeCode(prompt: string, signal?: AbortSignal): Promise<string> {
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
      let aborted = false;

      // Handle abort signal
      const abortHandler = () => {
        if (!aborted) {
          aborted = true;
          this.killProcess(processId);
          const error = new Error('Session cancelled');
          error.name = 'AbortError';
          reject(error);
        }
      };
      signal?.addEventListener('abort', abortHandler, { once: true });

      // Cleanup function to remove abort listener
      const cleanup = () => {
        signal?.removeEventListener('abort', abortHandler);
      };

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
        cleanup();
        this.activeProcesses.delete(processId);

        if (aborted) {
          return; // Already rejected via abort handler
        }

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
        cleanup();
        this.activeProcesses.delete(processId);
        if (!aborted) {
          reject(error);
        }
      });

      // Timeout
      setTimeout(() => {
        if (this.activeProcesses.has(processId) && !aborted) {
          this.killProcess(processId);
          cleanup();
          reject(new Error(`Claude Code timed out after ${CONFIG.llm.timeout}ms`));
        }
      }, CONFIG.llm.timeout);
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
