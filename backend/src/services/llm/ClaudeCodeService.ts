/**
 * Claude Code Service
 * Implements LLM by spawning Claude Code CLI subprocess
 */

import { ChildProcess, exec } from 'child_process';
import { promisify } from 'util';
import spawn from 'cross-spawn';
import type { LLMHealthStatus } from './LLMService.js';
import { BaseLLMService } from './BaseLLMService.js';
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

export class ClaudeCodeService extends BaseLLMService {
  private activeProcesses: Map<string, ChildProcess> = new Map();

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

  protected async callLLM(prompt: string, signal?: AbortSignal): Promise<string> {
    // Generate unique ID for this process
    const processId = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;

    return new Promise((resolve, reject) => {
      // Spawn claude CLI in non-interactive mode
      const claude = spawn(
        'claude',
        [
          '--print',
          '--model', 'sonnet',
          '--output-format', 'text',
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
    for (const [, proc] of this.activeProcesses) {
      try {
        proc.kill('SIGTERM');
      } catch {
        // Ignore - process may already be dead
      }
    }
    this.activeProcesses.clear();
  }
}
