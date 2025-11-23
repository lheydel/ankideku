import * as fs from 'fs/promises';
import * as path from 'path';
import { Note, SessionRequest, CardSuggestion, SessionState, SessionStateData } from '../types/index.js';
import { PromptGenerator } from '../processors/promptGenerator.js';
import { DATABASE_DIR, DECKS_DIR, AI_SESSIONS_DIR } from '../constants.js';

export class SessionService {
  private sessionsDir = AI_SESSIONS_DIR;

  constructor() {
    this.ensureSessionsDir();
  }

  /**
   * Ensure the sessions directory exists
   */
  private async ensureSessionsDir(): Promise<void> {
    try {
      await fs.mkdir(this.sessionsDir, { recursive: true });
    } catch (error) {
      console.error('Failed to create sessions directory:', error);
    }
  }

  /**
   * Create a new session
   * @param prompt - User's natural language prompt
   * @param deckName - Name of the deck to process
   * @returns Session ID
   */
  async createSession(prompt: string, deckName: string): Promise<string> {
    const sessionId = `session-${this.generateTimestamp()}`;
    const sessionDir = path.join(this.sessionsDir, sessionId);

    // Create session directory structure
    await fs.mkdir(sessionDir, { recursive: true });
    await fs.mkdir(path.join(sessionDir, 'logs'), { recursive: true });
    await fs.mkdir(path.join(sessionDir, 'suggestions'), { recursive: true });

    // Find all deck files (parent + subdecks)
    const deckPaths = await this.findDeckFiles(deckName);
    console.log(`Found ${deckPaths.length} deck file(s) for "${deckName}"`);

    // Get total card count across all decks
    const totalCards = await this.getTotalCardCount(deckPaths);

    // Create request file
    const request: SessionRequest = {
      sessionId,
      prompt,
      deckName,
      deckPaths,
      totalCards,
      timestamp: new Date().toISOString()
    };

    await fs.writeFile(
      path.join(sessionDir, 'request.json'),
      JSON.stringify(request, null, 2),
      'utf-8'
    );

    // Generate and write the Claude task file
    const taskPrompt = PromptGenerator.generatePrompt(request);
    await fs.writeFile(
      path.join(sessionDir, 'claude-task.md'),
      taskPrompt,
      'utf-8'
    );

    // Set initial session state to PENDING
    await this.setSessionState(sessionId, SessionState.PENDING);

    console.log(`Created session: ${sessionId} (${deckPaths.length} decks, ${totalCards} total cards)`);
    console.log(`Task file: ${path.join(sessionDir, 'claude-task.md')}`);
    return sessionId;
  }

  /**
   * Load all suggestions from an existing session
   * @param sessionId - Session ID
   * @returns Array of suggestions
   */
  async loadSession(sessionId: string): Promise<CardSuggestion[]> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');

    try {
      const files = await fs.readdir(suggestionsDir);
      const suggestions: CardSuggestion[] = [];

      for (const file of files) {
        if (file.startsWith('suggestion-') && file.endsWith('.json')) {
          const filePath = path.join(suggestionsDir, file);
          const content = await fs.readFile(filePath, 'utf-8');
          const suggestion: CardSuggestion = JSON.parse(content);

          // Only include suggestions that haven't been processed yet
          if (suggestion.accepted === null || suggestion.accepted === undefined) {
            suggestions.push(suggestion);
          }
        }
      }

      return suggestions;
    } catch (error) {
      console.error(`Failed to load session ${sessionId}:`, error);
      throw new Error(`Session ${sessionId} not found`);
    }
  }

  /**
   * Mark a suggestion as accepted or rejected
   * @param sessionId - Session ID
   * @param noteId - Note ID of the suggestion
   * @param accepted - true if accepted, false if rejected
   */
  async markSuggestionStatus(sessionId: string, noteId: number, accepted: boolean): Promise<void> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');
    const suggestionPath = path.join(suggestionsDir, `suggestion-${noteId}.json`);

    try {
      const content = await fs.readFile(suggestionPath, 'utf-8');
      const suggestion: CardSuggestion = JSON.parse(content);

      suggestion.accepted = accepted;

      await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2));
    } catch (error) {
      console.error(`Failed to update suggestion status for note ${noteId}:`, error);
      throw error;
    }
  }

  /**
   * Get request details for a session
   * @param sessionId - Session ID
   * @returns Session request object
   */
  async getSessionRequest(sessionId: string): Promise<SessionRequest> {
    const requestPath = path.join(this.sessionsDir, sessionId, 'request.json');

    try {
      const content = await fs.readFile(requestPath, 'utf-8');
      return JSON.parse(content);
    } catch (error) {
      console.error(`Failed to read request for session ${sessionId}:`, error);
      throw new Error(`Session request not found for ${sessionId}`);
    }
  }

  /**
   * List all sessions (sorted by timestamp, newest first)
   * @returns Array of session IDs
   */
  async listSessions(): Promise<string[]> {
    try {
      const entries = await fs.readdir(this.sessionsDir, { withFileTypes: true });
      const sessions = entries
        .filter(entry => entry.isDirectory() && entry.name.startsWith('session-'))
        .map(entry => entry.name)
        .sort()
        .reverse(); // Newest first

      return sessions;
    } catch (error) {
      console.error('Failed to list sessions:', error);
      return [];
    }
  }

  /**
   * List all sessions with metadata (sorted by timestamp, newest first)
   * @returns Array of session metadata objects
   */
  async listSessionsWithMetadata(): Promise<Array<{ sessionId: string; timestamp: string; deckName: string; totalCards: number; state?: SessionStateData }>> {
    try {
      const sessionIds = await this.listSessions();
      const sessionsWithMetadata = await Promise.all(
        sessionIds.map(async (sessionId) => {
          try {
            const request = await this.getSessionRequest(sessionId);
            const state = await this.getSessionState(sessionId);
            return {
              sessionId: request.sessionId,
              timestamp: request.timestamp,
              deckName: request.deckName,
              totalCards: request.totalCards,
              ...(state && { state })
            };
          } catch (error) {
            // If we can't read the request, return minimal data
            console.error(`Failed to read request for session ${sessionId}:`, error);
            return {
              sessionId,
              timestamp: new Date().toISOString(),
              deckName: 'Unknown',
              totalCards: 0
            };
          }
        })
      );

      return sessionsWithMetadata;
    } catch (error) {
      console.error('Failed to list sessions with metadata:', error);
      return [];
    }
  }

  /**
   * Get the session directory path
   * @param sessionId - Session ID
   * @returns Full path to session directory
   */
  getSessionDir(sessionId: string): string {
    return path.join(this.sessionsDir, sessionId);
  }

  /**
   * Generate timestamp for session ID
   * @returns Timestamp string (YYYY-MM-DD-HHMMSS format)
   */
  private generateTimestamp(): string {
    return new Date()
      .toISOString()
      .replace(/[:.]/g, '-')
      .replace('T', '-')
      .split('.')[0];
  }

  /**
   * Find all deck files for a given deck name (includes parent + subdecks)
   * @param deckName - Deck name (e.g., "JP Voc" or "JP_Voc")
   * @returns Array of full paths to deck files
   */
  private async findDeckFiles(deckName: string): Promise<string[]> {
    const decksDir = DECKS_DIR;

    try {
      const files = await fs.readdir(decksDir);
      const deckPaths: string[] = [];

      // Normalize deck name (replace spaces with underscores for matching)
      const normalizedName = deckName.replace(/\s+/g, '_');

      for (const file of files) {
        if (!file.endsWith('.json')) continue;

        const fileWithoutExt = file.replace('.json', '');

        // Match exact deck or subdecks (e.g., "JP_Voc__JP_Voc_01_Perso")
        if (fileWithoutExt === normalizedName || fileWithoutExt.startsWith(`${normalizedName}__`)) {
          deckPaths.push(path.join(decksDir, file));
        }
      }

      // Sort for consistent ordering
      deckPaths.sort();

      return deckPaths.length > 0 ? deckPaths : [path.join(decksDir, `${normalizedName}.json`)];
    } catch (error) {
      console.error(`Failed to find deck files for ${deckName}:`, error);
      // Fallback to single deck path
      return [path.join(decksDir, `${deckName.replace(/\s+/g, '_')}.json`)];
    }
  }

  /**
   * Get total card count across multiple deck files
   * @param deckPaths - Array of deck file paths
   * @returns Total number of cards
   */
  private async getTotalCardCount(deckPaths: string[]): Promise<number> {
    let total = 0;

    for (const deckPath of deckPaths) {
      try {
        const content = await fs.readFile(deckPath, 'utf-8');
        const deck = JSON.parse(content);
        total += deck.notes?.length || 0;
      } catch (error) {
        console.error(`Failed to count cards in ${deckPath}:`, error);
      }
    }

    return total;
  }

  /**
   * Set the state of a session
   * @param sessionId - Session ID
   * @param state - New session state
   * @param message - Optional message for context
   * @param exitCode - Optional exit code for completed/failed states
   */
  async setSessionState(
    sessionId: string,
    state: SessionState,
    message?: string,
    exitCode?: number | null
  ): Promise<void> {
    const sessionDir = path.join(this.sessionsDir, sessionId);
    const statePath = path.join(sessionDir, 'state.json');

    const stateData: SessionStateData = {
      state,
      timestamp: new Date().toISOString(),
      ...(message && { message }),
      ...(exitCode !== undefined && { exitCode })
    };

    try {
      await fs.writeFile(
        statePath,
        JSON.stringify(stateData, null, 2),
        'utf-8'
      );
      console.log(`Session ${sessionId} state updated to: ${state}`);
    } catch (error) {
      console.error(`Failed to update session ${sessionId} state to ${state}:`, error);
    }
  }

  /**
   * Get the current state of a session
   * @param sessionId - Session ID
   * @returns Session state data or null if not found
   */
  async getSessionState(sessionId: string): Promise<SessionStateData | null> {
    const sessionDir = path.join(this.sessionsDir, sessionId);
    const statePath = path.join(sessionDir, 'state.json');

    try {
      const content = await fs.readFile(statePath, 'utf-8');
      return JSON.parse(content);
    } catch (error) {
      // Migration: Check for old cancelled.json format
      const cancelledPath = path.join(sessionDir, 'cancelled.json');
      try {
        const cancelledContent = await fs.readFile(cancelledPath, 'utf-8');
        const cancelled = JSON.parse(cancelledContent);

        // Migrate old format to new format
        const migratedState: SessionStateData = {
          state: SessionState.CANCELLED,
          timestamp: cancelled.timestamp || new Date().toISOString()
        };

        // Write the new state file
        await this.setSessionState(sessionId, SessionState.CANCELLED);

        return migratedState;
      } catch {
        // No state file found
        return null;
      }
    }
  }

  /**
   * Mark a session as cancelled
   * @param sessionId - Session ID
   * @param message - Optional cancellation message
   */
  async markSessionCancelled(sessionId: string, message?: string): Promise<void> {
    await this.setSessionState(sessionId, SessionState.CANCELLED, message);
  }

  /**
   * Mark a session as running
   * @param sessionId - Session ID
   */
  async markSessionRunning(sessionId: string): Promise<void> {
    await this.setSessionState(sessionId, SessionState.RUNNING);
  }

  /**
   * Mark a session as completed
   * @param sessionId - Session ID
   * @param exitCode - Exit code from Claude process
   */
  async markSessionCompleted(sessionId: string, exitCode?: number | null): Promise<void> {
    await this.setSessionState(sessionId, SessionState.COMPLETED, undefined, exitCode);
  }

  /**
   * Mark a session as failed
   * @param sessionId - Session ID
   * @param errorMessage - Error message
   * @param exitCode - Exit code from Claude process
   */
  async markSessionFailed(sessionId: string, errorMessage?: string, exitCode?: number | null): Promise<void> {
    await this.setSessionState(sessionId, SessionState.FAILED, errorMessage, exitCode);
  }

  /**
   * Check if a session was cancelled (backwards compatibility)
   * @param sessionId - Session ID
   * @returns Cancellation info or null
   */
  async getSessionCancellation(sessionId: string): Promise<{ cancelled: true; timestamp: string } | null> {
    const state = await this.getSessionState(sessionId);

    if (state && state.state === SessionState.CANCELLED) {
      return {
        cancelled: true,
        timestamp: state.timestamp
      };
    }

    return null;
  }

  /**
   * Delete a session (cleanup)
   * @param sessionId - Session ID
   */
  async deleteSession(sessionId: string): Promise<void> {
    const sessionDir = path.join(this.sessionsDir, sessionId);

    try {
      await fs.rm(sessionDir, { recursive: true, force: true });
      console.log(`Deleted session: ${sessionId}`);
    } catch (error) {
      console.error(`Failed to delete session ${sessionId}:`, error);
      throw error;
    }
  }
}

// Singleton instance
export const sessionService = new SessionService();
