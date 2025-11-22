import { spawn, ChildProcess, execSync } from 'child_process';
import * as path from 'path';
import * as fs from 'fs';

export interface SpawnOptions {
  sessionId: string;
  sessionDir: string;
  timeout?: number; // Optional timeout in milliseconds
}

/**
 * Check if Claude CLI is installed
 */
function isClaudeInstalled(): boolean {
  try {
    // On Windows, try 'where claude', on Unix try 'which claude'
    const command = process.platform === 'win32' ? 'where claude' : 'which claude';
    execSync(command, { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

export interface SpawnResult {
  success: boolean;
  sessionId: string;
  exitCode: number | null;
  output: string;
  error?: string;
}

/**
 * Service for spawning Claude Code CLI to process card suggestions
 */
export class ClaudeSpawnerService {
  private activeProcesses: Map<string, ChildProcess> = new Map();

  /**
   * Spawn Claude Code CLI to process a session
   * @param options - Spawn options including session ID and directory
   * @returns Promise that resolves when Claude completes processing
   */
  async spawnClaude(options: SpawnOptions): Promise<SpawnResult> {
    const { sessionId, sessionDir, timeout } = options;

    const taskFilePath = path.join(sessionDir, 'claude-task.md');
    const logsDir = path.join(sessionDir, 'logs');
    const stdoutLogPath = path.join(logsDir, 'claude-stdout.log');
    const stderrLogPath = path.join(logsDir, 'claude-stderr.log');
    const combinedLogPath = path.join(logsDir, 'claude-output.log');

    // Get the database directory (parent of ai-sessions) and project root
    const databaseDir = path.join(sessionDir, '../..');
    const resolvedDatabaseDir = path.resolve(databaseDir);
    const projectRoot = path.join(resolvedDatabaseDir, '..'); // Parent of database/
    const resolvedProjectRoot = path.resolve(projectRoot);

    console.log(`[Claude Spawner] Spawning Claude CLI for session: ${sessionId}`);
    console.log(`[Claude Spawner] Task file: ${taskFilePath}`);
    console.log(`[Claude Spawner] Logs: ${combinedLogPath}`);
    console.log(`[Claude Spawner] Working directory: ${resolvedProjectRoot}`);
    console.log(`[Claude Spawner] Allowed directory: ${resolvedDatabaseDir}`);
    console.log(`[Claude Spawner] Command: claude --permission-mode acceptEdits --add-dir "${resolvedDatabaseDir}" -- ${taskFilePath}`);
    console.log(`[Claude Spawner] Platform: ${process.platform}`);

    // Check if Claude CLI is installed
    const claudeInstalled = isClaudeInstalled();
    console.log(`[Claude Spawner] Claude CLI installed: ${claudeInstalled}`);

    // Create log file streams
    const stdoutStream = fs.createWriteStream(stdoutLogPath, { flags: 'w' });
    const stderrStream = fs.createWriteStream(stderrLogPath, { flags: 'w' });
    const combinedStream = fs.createWriteStream(combinedLogPath, { flags: 'w' });

    // Write header to combined log
    const header = `=== Claude Code Output Log ===\nSession: ${sessionId}\nStarted: ${new Date().toISOString()}\nCommand: claude --permission-mode acceptEdits --add-dir "${resolvedDatabaseDir}" -- ${taskFilePath}\nWorking Directory: ${resolvedProjectRoot}\nAllowed Directory: ${resolvedDatabaseDir}\nPlatform: ${process.platform}\nClaude CLI Installed: ${claudeInstalled}\n${'='.repeat(50)}\n\n`;
    combinedStream.write(header);

    // Early return if Claude is not installed
    if (!claudeInstalled) {
      const errorMsg = `Claude CLI is not installed or not in PATH.\n\nTo install Claude Code, visit: https://docs.anthropic.com/claude-code\n\nAfter installation, make sure the 'claude' command is available in your PATH.`;
      const errorFooter = `\n${'='.repeat(50)}\nERROR: Claude CLI not found\n${errorMsg}\nFailed: ${new Date().toISOString()}\n`;

      return new Promise((_, reject) => {
        combinedStream.write(errorFooter, () => {
          stdoutStream.end();
          stderrStream.end();
          combinedStream.end();

          reject(new Error('Claude CLI is not installed or not in PATH'));
        });
      });
    }

    return new Promise((resolve, reject) => {
      // Spawn claude CLI with the task file
      // --permission-mode acceptEdits: Auto-accept Write/Edit operations for non-interactive execution
      // --add-dir: Restrict file access to only the database directory for security
      // --: Separator to prevent task file from being consumed by --add-dir
      const claude = spawn('claude', [
        '--permission-mode', 'acceptEdits',
        '--add-dir', resolvedDatabaseDir,
        '--',  // End of options
        taskFilePath
      ], {
        cwd: resolvedProjectRoot,  // Run from project root so relative paths work
        env: process.env,
        stdio: ['pipe', 'pipe', 'pipe'],
        shell: true  // Required on Windows for proper stdio capture
      });

      this.activeProcesses.set(sessionId, claude);

      let stdoutData = '';
      let stderrData = '';

      // Capture stdout
      claude.stdout?.on('data', (data: Buffer) => {
        const output = data.toString();
        stdoutData += output;

        // Write to log files
        stdoutStream.write(output);
        combinedStream.write(`[STDOUT] ${output}`);

        console.log(`[Claude ${sessionId}]: ${output.trim()}`);
      });

      // Capture stderr
      claude.stderr?.on('data', (data: Buffer) => {
        const output = data.toString();
        stderrData += output;

        // Write to log files
        stderrStream.write(output);
        combinedStream.write(`[STDERR] ${output}`);

        console.error(`[Claude ${sessionId} Error]: ${output.trim()}`);
      });

      // Handle process completion
      claude.on('close', (code: number | null, signal: string | null) => {
        this.activeProcesses.delete(sessionId);

        console.log(`[Claude ${sessionId}]: Process closed with code=${code}, signal=${signal}, hasStdout=${stdoutData.length > 0}, hasStderr=${stderrData.length > 0}`);

        // Close log streams
        const exitInfo = code !== null ? `Exit Code: ${code}` : `Process terminated (signal: ${signal || 'unknown'})`;
        const footer = `\n${'='.repeat(50)}\nCompleted: ${new Date().toISOString()}\n${exitInfo}\nStdout length: ${stdoutData.length}\nStderr length: ${stderrData.length}\n`;
        combinedStream.write(footer);

        stdoutStream.end();
        stderrStream.end();
        combinedStream.end();

        const result: SpawnResult = {
          success: code === 0,
          sessionId,
          exitCode: code,
          output: stdoutData
        };

        // Windows + shell:true quirk: exit code can be null even for successful completion
        // If we got stdout and no stderr, consider it successful
        const likelySuccessful = code === 0 || (code === null && stdoutData.length > 0 && stderrData.length === 0);

        if (likelySuccessful) {
          result.success = true;
          console.log(`[Claude ${sessionId}]: Completed successfully (exit code: ${code})`);
          resolve(result);
        } else if (code === null) {
          // Process was killed or terminated abnormally
          result.error = stderrData || 'Process was terminated';
          console.log(`[Claude ${sessionId}]: Process terminated abnormally`);
          reject(new Error(result.error));
        } else {
          result.error = stderrData || `Process exited with code ${code}`;
          console.error(`[Claude ${sessionId}]: Failed with code ${code}`);
          reject(new Error(result.error));
        }
      });

      // Handle process errors (e.g., command not found)
      claude.on('error', (error: Error) => {
        this.activeProcesses.delete(sessionId);

        console.error(`[Claude Spawner] Process error for session ${sessionId}:`, error);
        console.error(`[Claude Spawner] Error code: ${(error as any).code}`);
        console.error(`[Claude Spawner] Error message: ${error.message}`);

        // Write error to log and ensure streams are flushed before rejecting
        const errorFooter = `\n${'='.repeat(50)}\nERROR: ${error.message}\nError Code: ${(error as any).code || 'N/A'}\nFailed: ${new Date().toISOString()}\n\nIf you see ENOENT or 'command not found', Claude CLI is not installed or not in PATH.\nInstall Claude Code: https://docs.anthropic.com/claude-code\n`;

        combinedStream.write(errorFooter, () => {
          stdoutStream.end();
          stderrStream.end();
          combinedStream.end();

          const result: SpawnResult = {
            success: false,
            sessionId,
            exitCode: null,
            output: stdoutData,
            error: `${error.message} (code: ${(error as any).code || 'unknown'})`
          };

          reject(error);
        });
      });

      // Optional timeout
      if (timeout) {
        setTimeout(() => {
          if (this.activeProcesses.has(sessionId)) {
            console.log(`[Claude ${sessionId}]: Timeout reached, killing process`);
            this.killProcess(sessionId);

            reject(new Error(`Claude process timed out after ${timeout}ms`));
          }
        }, timeout);
      }
    });
  }

  /**
   * Kill a running Claude process
   * @param sessionId - Session ID
   * @returns True if process was killed
   */
  killProcess(sessionId: string): boolean {
    const claudeProcess = this.activeProcesses.get(sessionId);

    if (claudeProcess) {
      console.log(`Killing Claude process for session: ${sessionId}`);

      // On Windows with shell: true, we need to kill the entire process tree
      if (claudeProcess.pid && process.platform === 'win32') {
        try {
          // Use taskkill to kill the process tree on Windows
          execSync(`taskkill /pid ${claudeProcess.pid} /T /F`, { windowsHide: true });
          console.log(`Killed process tree for PID ${claudeProcess.pid}`);
        } catch (error) {
          console.error(`Failed to kill process tree:`, error);
          // Fallback to regular kill
          claudeProcess.kill();
        }
      } else {
        // On Unix-like systems, use SIGTERM
        claudeProcess.kill('SIGTERM');
      }

      this.activeProcesses.delete(sessionId);
      return true;
    }

    return false;
  }

  /**
   * Check if a session has an active Claude process
   * @param sessionId - Session ID
   * @returns True if process is running
   */
  isRunning(sessionId: string): boolean {
    return this.activeProcesses.has(sessionId);
  }

  /**
   * Get list of active session IDs
   * @returns Array of session IDs with running processes
   */
  getActiveSessions(): string[] {
    return Array.from(this.activeProcesses.keys());
  }

  /**
   * Kill all running Claude processes
   */
  killAll(): void {
    console.log(`Killing all Claude processes (${this.activeProcesses.size} active)`);

    for (const [sessionId, claudeProcess] of this.activeProcesses.entries()) {
      console.log(`Killing process for session: ${sessionId}`);

      // On Windows with shell: true, we need to kill the entire process tree
      if (claudeProcess.pid && process.platform === 'win32') {
        try {
          execSync(`taskkill /pid ${claudeProcess.pid} /T /F`, { windowsHide: true });
        } catch (error) {
          console.error(`Failed to kill process tree for ${sessionId}:`, error);
          claudeProcess.kill('SIGTERM');
        }
      } else {
        claudeProcess.kill('SIGTERM');
      }
    }

    this.activeProcesses.clear();
  }
}
