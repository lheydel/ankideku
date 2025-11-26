import * as fs from 'fs/promises';
import * as path from 'path';
import { SessionRequest, CardSuggestion, SessionState, SessionStateData, SessionProgress } from '../../types/index.js';
import { DECKS_DIR, AI_SESSIONS_DIR } from '../../constants.js';
import { ensureDir } from '../../utils/fs.js';

export class SessionService {
  private sessionsDir = AI_SESSIONS_DIR;

  constructor() {
    this.ensureSessionsDir();
  }

  /**
   * Ensure the sessions directory exists
   */
  private async ensureSessionsDir(): Promise<void> {
    await ensureDir(this.sessionsDir);
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

    // Set initial session state to PENDING
    await this.setSessionState(sessionId, SessionState.PENDING);

    console.log(`Created session: ${sessionId} (${deckPaths.length} decks, ${totalCards} total cards)`);
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
   * Refresh the original fields of a suggestion with current state from Anki
   * @param sessionId - Session ID
   * @param noteId - Note ID of the suggestion
   * @returns Updated suggestion
   */
  async refreshSuggestionOriginal(sessionId: string, noteId: number): Promise<CardSuggestion> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');
    const suggestionPath = path.join(suggestionsDir, `suggestion-${noteId}.json`);

    try {
      const content = await fs.readFile(suggestionPath, 'utf-8');
      const suggestion: CardSuggestion = JSON.parse(content);

      // Import ankiConnectService here to avoid circular dependency
      const { ankiConnectService } = await import('../anki/AnkiConnectService.js');

      // Fetch current note state from Anki
      const currentNotes = await ankiConnectService.notesInfo([noteId]);
      if (currentNotes.length === 0) {
        throw new Error(`Note ${noteId} not found in Anki`);
      }

      const currentNote = currentNotes[0];

      // Update the original fields with current values from Anki
      for (const [fieldName, fieldData] of Object.entries(suggestion.original.fields)) {
        if (currentNote.fields[fieldName] !== undefined) {
          // currentNote.fields[fieldName] is already a NoteField object with {value, order}
          // We only want to update the value string
          const currentField = currentNote.fields[fieldName];
          if (typeof currentField === 'string') {
            // If it's a plain string (shouldn't happen but handle it)
            fieldData.value = currentField;
          } else {
            // It's a NoteField object, extract just the value
            fieldData.value = currentField.value;
          }
        }
      }

      // Save the updated suggestion back to file
      await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2));

      return suggestion;
    } catch (error) {
      console.error(`Failed to refresh suggestion original for note ${noteId}:`, error);
      throw error;
    }
  }

  /**
   * Mark a suggestion as accepted or rejected
   * @param sessionId - Session ID
   * @param noteId - Note ID of the suggestion
   * @param accepted - true if accepted, false if rejected
   * @param appliedChanges - The actual changes that were applied (may differ from original suggestion if edited)
   */
  async markSuggestionStatus(sessionId: string, noteId: number, accepted: boolean, appliedChanges?: Record<string, string>): Promise<void> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');
    const suggestionPath = path.join(suggestionsDir, `suggestion-${noteId}.json`);

    try {
      const content = await fs.readFile(suggestionPath, 'utf-8');
      const suggestion: CardSuggestion = JSON.parse(content);

      suggestion.accepted = accepted;

      // If accepted and changes were applied, update the original to reflect the new state
      if (accepted && appliedChanges) {
        // Update the original fields with the applied changes
        for (const [fieldName, newValue] of Object.entries(appliedChanges)) {
          if (suggestion.original.fields[fieldName]) {
            suggestion.original.fields[fieldName].value = newValue;
          }
        }
      }

      await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2));
    } catch (error) {
      console.error(`Failed to update suggestion status for note ${noteId}:`, error);
      throw error;
    }
  }

  /**
   * Save edited changes made by the user to a suggestion
   * @param sessionId - Session ID
   * @param noteId - Note ID
   * @param editedChanges - The edited field values
   */
  async saveEditedChanges(sessionId: string, noteId: number, editedChanges: Record<string, string>): Promise<void> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');
    const suggestionPath = path.join(suggestionsDir, `suggestion-${noteId}.json`);

    try {
      const content = await fs.readFile(suggestionPath, 'utf-8');
      const suggestion: CardSuggestion = JSON.parse(content);

      suggestion.editedChanges = editedChanges;

      await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2));
      console.log(`Saved edited changes for note ${noteId} in session ${sessionId}`);
    } catch (error) {
      console.error(`Failed to save edited changes for note ${noteId}:`, error);
      throw error;
    }
  }

  /**
   * Revert/remove all edited changes made by the user to a suggestion
   * @param sessionId - Session ID
   * @param noteId - Note ID
   */
  async revertEditedChanges(sessionId: string, noteId: number): Promise<void> {
    const suggestionsDir = path.join(this.sessionsDir, sessionId, 'suggestions');
    const suggestionPath = path.join(suggestionsDir, `suggestion-${noteId}.json`);

    try {
      const content = await fs.readFile(suggestionPath, 'utf-8');
      const suggestion: CardSuggestion = JSON.parse(content);

      // Delete the editedChanges field entirely
      delete suggestion.editedChanges;

      await fs.writeFile(suggestionPath, JSON.stringify(suggestion, null, 2));
      console.log(`Reverted edited changes for note ${noteId} in session ${sessionId}`);
    } catch (error) {
      console.error(`Failed to revert edited changes for note ${noteId}:`, error);
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
   * @param progress - Optional progress data
   */
  async setSessionState(
    sessionId: string,
    state: SessionState,
    message?: string,
    exitCode?: number | null,
    progress?: SessionProgress
  ): Promise<void> {
    const sessionDir = path.join(this.sessionsDir, sessionId);
    const statePath = path.join(sessionDir, 'state.json');

    // Load existing state and merge with new fields
    const currentState = await this.getSessionState(sessionId);

    const stateData: SessionStateData = {
      ...currentState,
      state,
      timestamp: new Date().toISOString(),
      ...(message && { message }),
      ...(exitCode !== undefined && { exitCode }),
      ...(progress && { progress })
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
   * Update session progress without changing state
   * @param sessionId - Session ID
   * @param progress - Progress data
   */
  async updateSessionProgress(sessionId: string, progress: SessionProgress): Promise<SessionStateData | null> {
    const currentState = await this.getSessionState(sessionId);
    if (!currentState) return null;

    const updatedState: SessionStateData = {
      ...currentState,
      timestamp: new Date().toISOString(),
      progress
    };

    const sessionDir = path.join(this.sessionsDir, sessionId);
    const statePath = path.join(sessionDir, 'state.json');

    try {
      await fs.writeFile(
        statePath,
        JSON.stringify(updatedState, null, 2),
        'utf-8'
      );
      return updatedState;
    } catch (error) {
      console.error(`Failed to update session ${sessionId} progress:`, error);
      return null;
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
    // Don't overwrite cancelled or failed states
    const currentState = await this.getSessionState(sessionId);
    if (currentState?.state === SessionState.CANCELLED || currentState?.state === SessionState.FAILED) {
      return;
    }
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
