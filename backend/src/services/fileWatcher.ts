import chokidar, { FSWatcher } from 'chokidar';
import * as fs from 'fs/promises';
import * as path from 'path';
import { EventEmitter } from 'events';
import type { CardSuggestion } from '../types/index.js';

export interface SuggestionEvent {
  sessionId: string;
  suggestion: CardSuggestion;
}

export interface SessionCompleteEvent {
  sessionId: string;
  totalSuggestions: number;
}

/**
 * File watcher service for monitoring AI session directories
 * Emits events when new suggestion files are created
 */
export class FileWatcherService extends EventEmitter {
  private watchers: Map<string, FSWatcher> = new Map();
  private suggestionCounts: Map<string, number> = new Map();

  /**
   * Start watching a session directory for new suggestion files
   * @param sessionId - Session ID to watch
   * @param sessionDir - Full path to session directory
   */
  async watchSession(sessionId: string, sessionDir: string): Promise<void> {
    if (this.watchers.has(sessionId)) {
      console.log(`Already watching session: ${sessionId}`);
      return;
    }

    console.log(`Starting file watcher for session: ${sessionId}`);

    // Initialize suggestion count
    this.suggestionCounts.set(sessionId, 0);

    // Watch both the suggestions subdirectory and state.json file
    const suggestionsDir = path.join(sessionDir, 'suggestions');
    const stateFile = path.join(sessionDir, 'state.json');

    // Ensure suggestions directory exists before watching
    await fs.mkdir(suggestionsDir, { recursive: true });

    const watcher = chokidar.watch([suggestionsDir, stateFile], {
      persistent: true,
      ignoreInitial: true, // Don't trigger events for existing files
      awaitWriteFinish: {
        stabilityThreshold: 100,
        pollInterval: 50
      }
    });

    const handleStateChange = async (filePath: string) => {
      try {
        const basename = path.basename(filePath);

        if (basename === 'state.json') {
          const content = await fs.readFile(filePath, 'utf-8');
          const state = JSON.parse(content);
          this.emit('state:change', { sessionId, state });
          console.log(`State changed for session ${sessionId}: ${state.state}`);
        }
      } catch (error) {
        console.error(`Error processing file ${filePath}:`, error);
        this.emit('error', {
          sessionId,
          error: error instanceof Error ? error.message : 'Unknown error',
          filePath
        });
      }
    };

    const handleNewSuggestion = async (filePath: string) => {
      try {
        const basename = path.basename(filePath);

        if (basename.startsWith('suggestion-') && basename.endsWith('.json')) {
          const content = await fs.readFile(filePath, 'utf-8');
          const suggestion: CardSuggestion = JSON.parse(content);

          const count = (this.suggestionCounts.get(sessionId) || 0) + 1;
          this.suggestionCounts.set(sessionId, count);

          this.emit('suggestion:new', { sessionId, suggestion } as SuggestionEvent);
          console.log(`New suggestion for session ${sessionId}: note ${suggestion.noteId} (${count} total)`);
        }
      } catch (error) {
        console.error(`Error processing file ${filePath}:`, error);
        this.emit('error', {
          sessionId,
          error: error instanceof Error ? error.message : 'Unknown error',
          filePath
        });
      }
    };

    watcher.on('add', handleNewSuggestion);
    watcher.on('change', handleStateChange);

    watcher.on('ready', () => {
      console.log(`File watcher ready for session ${sessionId}`);
    });

    watcher.on('error', (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`Watcher error for session ${sessionId}:`, errorMessage);
      this.emit('error', { sessionId, error: errorMessage });
    });

    this.watchers.set(sessionId, watcher);
  }

  /**
   * Stop watching a session directory
   * @param sessionId - Session ID to stop watching
   */
  async unwatchSession(sessionId: string): Promise<void> {
    const watcher = this.watchers.get(sessionId);

    if (watcher) {
      await watcher.close();
      this.watchers.delete(sessionId);

      const count = this.suggestionCounts.get(sessionId) || 0;
      this.suggestionCounts.delete(sessionId);

      console.log(`Stopped watching session: ${sessionId} (${count} suggestions processed)`);

      // Emit session:complete event
      this.emit('session:complete', {
        sessionId,
        totalSuggestions: count
      } as SessionCompleteEvent);
    }
  }

  /**
   * Get the number of suggestions detected for a session
   * @param sessionId - Session ID
   * @returns Suggestion count
   */
  getSuggestionCount(sessionId: string): number {
    return this.suggestionCounts.get(sessionId) || 0;
  }

  /**
   * Check if a session is currently being watched
   * @param sessionId - Session ID
   * @returns True if watching
   */
  isWatching(sessionId: string): boolean {
    return this.watchers.has(sessionId);
  }

  /**
   * Stop watching all sessions
   */
  async unwatchAll(): Promise<void> {
    console.log(`Unwatching all sessions (${this.watchers.size} active)`);

    const promises = Array.from(this.watchers.keys()).map(sessionId =>
      this.unwatchSession(sessionId)
    );

    await Promise.all(promises);
  }

  /**
   * Get list of currently watched sessions
   * @returns Array of session IDs
   */
  getWatchedSessions(): string[] {
    return Array.from(this.watchers.keys());
  }
}
