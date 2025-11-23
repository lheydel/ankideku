import { ChildProcess, execSync } from 'child_process';
import spawn from 'cross-spawn';
import kill from 'tree-kill';
import * as path from 'path';
import * as fs from 'fs';
import * as fsPromises from 'fs/promises';

// Note: execSync is still used for checking if Claude CLI is installed

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
    console.log(`[Claude Spawner] Command: cat ${taskFilePath} | claude --print --output-format stream-json --verbose --add-dir "${resolvedDatabaseDir}" --allowedTools "Read,Write,Edit,Bash"`);
    console.log(`[Claude Spawner] Platform: ${process.platform}`);

    // Check if Claude CLI is installed
    const claudeInstalled = isClaudeInstalled();
    console.log(`[Claude Spawner] Claude CLI installed: ${claudeInstalled}`);

    // Create log file streams
    const stdoutStream = fs.createWriteStream(stdoutLogPath, { flags: 'w' });
    const stderrStream = fs.createWriteStream(stderrLogPath, { flags: 'w' });
    const combinedStream = fs.createWriteStream(combinedLogPath, { flags: 'w' });

    // Write header to combined log
    const header = `=== Claude Code Output Log ===\nSession: ${sessionId}\nStarted: ${new Date().toISOString()}\nCommand: cat ${taskFilePath} | claude --print --output-format stream-json --verbose --add-dir "${resolvedDatabaseDir}" --allowedTools "Read,Write,Edit,Bash"\nOutput Format: Streaming JSON (JSONL)\nWorking Directory: ${resolvedProjectRoot}\nAllowed Directory: ${resolvedDatabaseDir}\nPlatform: ${process.platform}\nClaude CLI Installed: ${claudeInstalled}\n${'='.repeat(50)}\n\n`;
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

    return new Promise(async (resolve, reject) => {
      // Read the task file content to pipe to stdin
      let taskContent: string;
      try {
        taskContent = await fsPromises.readFile(taskFilePath, 'utf-8');
      } catch (error) {
        reject(new Error(`Failed to read task file: ${error}`));
        return;
      }

      // Spawn claude CLI in non-interactive mode with streaming JSON output
      // --print: Run in non-interactive mode and print the final result
      // --output-format stream-json: Stream progress as JSONL for real-time updates
      // --verbose: Show full turn-by-turn output for debugging
      // --add-dir: Restrict file access to only the database directory for security
      // --allowedTools: Explicitly allow file operations (Read, Write, Edit, Bash)
      // Task content is piped to stdin (like: cat task.md | claude ...)
      // Using cross-spawn for better Windows compatibility
      const claude = spawn('claude', [
        '--print',
        '--output-format', 'stream-json',
        '--verbose',
        '--add-dir', resolvedDatabaseDir,
        '--allowedTools', 'Read,Write,Edit,Bash'
      ], {
        cwd: resolvedProjectRoot,  // Run from project root so relative paths work
        env: process.env,
        stdio: ['pipe', 'pipe', 'pipe']
        // cross-spawn handles Windows quirks automatically - no need for shell: true
      });

      this.activeProcesses.set(sessionId, claude);

      let stdoutData = '';
      let stderrData = '';
      let partialLine = ''; // Buffer for incomplete JSON lines

      // Pipe the task content to stdin
      if (claude.stdin) {
        claude.stdin.write(taskContent);
        claude.stdin.end();
      }

      // Capture and parse streaming JSON output
      claude.stdout?.on('data', (data: Buffer) => {
        const output = data.toString();
        stdoutData += output;

        // Write raw output to stdout log only (not combined)
        stdoutStream.write(output);

        // Parse JSONL (JSON Lines) stream and write parsed version to combined log
        const lines = (partialLine + output).split('\n');
        partialLine = lines.pop() || ''; // Save incomplete line for next chunk

        for (const line of lines) {
          if (!line.trim()) continue;

          try {
            const message = JSON.parse(line);

            // Log based on message type
            switch (message.type) {
              case 'system':
                // Handle system messages (init, etc.)
                if (message.subtype === 'init') {
                  console.log(`[Claude ${sessionId}] Session initialized - Model: ${message.model || 'N/A'}, Version: ${message.claude_code_version || 'N/A'}`);
                  combinedStream.write(`[INIT] Session: ${message.session_id || 'N/A'}, Model: ${message.model}\n`);
                }
                break;

              case 'user':
                // Check if this is a tool result or actual user message
                const isToolResult = message.message?.content?.[0]?.type === 'tool_result';
                if (isToolResult) {
                  const toolUseId = message.message.content[0].tool_use_id || 'unknown';
                  console.log(`[Claude ${sessionId}] Tool result: ${toolUseId.substring(0, 20)}...`);
                  combinedStream.write(`[TOOL_RESULT] ${toolUseId}\n`);
                } else {
                  console.log(`[Claude ${sessionId}] User prompt received`);
                  combinedStream.write(`[USER] Task prompt\n`);
                }
                break;

              case 'assistant':
                // Log assistant's actions/thoughts
                const contentArray = message.message?.content || [];
                let logText = '';

                for (const item of contentArray) {
                  if (item.type === 'text') {
                    logText += item.text + ' ';
                  } else if (item.type === 'tool_use') {
                    logText += `[Using ${item.name} tool] `;
                  }
                }

                if (logText) {
                  console.log(`[Claude ${sessionId}] ${logText.trim().substring(0, 100)}${logText.length > 100 ? '...' : ''}`);
                  combinedStream.write(`[ASSISTANT] ${logText.trim()}\n`);
                }
                break;

              case 'result':
                // Final result with statistics
                const isSuccess = message.subtype === 'success';
                const duration = message.duration_ms || 0;
                const turns = message.num_turns || 0;
                const cost = message.total_cost_usd || 0;
                const resultText = message.result || '';

                console.log(`[Claude ${sessionId}] ${isSuccess ? 'Completed' : 'Failed'} - Turns: ${turns}, Duration: ${(duration/1000).toFixed(1)}s, Cost: $${cost.toFixed(4)}`);
                combinedStream.write(`[RESULT] Status: ${message.subtype}, Turns: ${turns}, Duration: ${duration}ms, Cost: $${cost}\n`);
                if (resultText) {
                  combinedStream.write(`[RESULT TEXT] ${resultText}\n`);
                }
                break;

              default:
                console.log(`[Claude ${sessionId}] Unknown message type: ${message.type}`);
            }
          } catch (parseError) {
            // Not valid JSON - might be plain text or incomplete
            console.log(`[Claude ${sessionId}]: ${line.trim()}`);
          }
        }
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

        // cross-spawn provides reliable exit codes across platforms
        if (code === 0) {
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

    if (claudeProcess && claudeProcess.pid) {
      console.log(`Killing Claude process for session: ${sessionId}`);

      // Use tree-kill to kill the entire process tree (cross-platform)
      kill(claudeProcess.pid, 'SIGTERM', (error) => {
        if (error) {
          console.error(`Failed to kill process tree for ${sessionId}:`, error);
          // Fallback to direct kill
          try {
            claudeProcess.kill('SIGTERM');
          } catch (fallbackError) {
            console.error(`Fallback kill also failed:`, fallbackError);
          }
        } else {
          console.log(`Killed process tree for PID ${claudeProcess.pid}`);
        }
      });

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

      if (claudeProcess.pid) {
        // Use tree-kill to kill the entire process tree (cross-platform)
        kill(claudeProcess.pid, 'SIGTERM', (error) => {
          if (error) {
            console.error(`Failed to kill process tree for ${sessionId}:`, error);
            try {
              claudeProcess.kill('SIGTERM');
            } catch (fallbackError) {
              console.error(`Fallback kill also failed:`, fallbackError);
            }
          }
        });
      }
    }

    this.activeProcesses.clear();
  }
}
