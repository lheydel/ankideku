import fs from 'fs/promises';
import path from 'path';
import { ActionHistoryEntry } from '../../../contract';
import { AI_SESSIONS_DIR } from '../constants.js';
import { ensureDir } from '../utils/fs.js';

export class HistoryService {
  /**
   * Get the path to a session's history file
   */
  private getHistoryPath(sessionId: string): string {
    return path.join(AI_SESSIONS_DIR, sessionId, 'history.json');
  }

  /**
   * Save a review action to the session's history
   */
  async saveAction(sessionId: string, action: ActionHistoryEntry): Promise<void> {
    const historyPath = this.getHistoryPath(sessionId);

    // Ensure the session directory exists
    const sessionDir = path.dirname(historyPath);
    await ensureDir(sessionDir);

    // Load existing history or start with empty array
    let history: ActionHistoryEntry[] = [];
    try {
      const data = await fs.readFile(historyPath, 'utf-8');
      history = JSON.parse(data);
    } catch (error) {
      // File doesn't exist yet, that's fine
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
        throw error;
      }
    }

    // Append the new action
    history.push(action);

    // Save back to file
    await fs.writeFile(historyPath, JSON.stringify(history, null, 2), 'utf-8');
  }

  /**
   * Get the review history for a specific session
   */
  async getSessionHistory(sessionId: string): Promise<ActionHistoryEntry[]> {
    const historyPath = this.getHistoryPath(sessionId);

    try {
      const data = await fs.readFile(historyPath, 'utf-8');
      return JSON.parse(data);
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return []; // No history yet
      }
      throw error;
    }
  }

  /**
   * Get all review history from all sessions
   */
  async getAllHistory(): Promise<ActionHistoryEntry[]> {
    try {
      const sessionDirs = await fs.readdir(AI_SESSIONS_DIR);
      const allHistory: ActionHistoryEntry[] = [];

      for (const sessionDir of sessionDirs) {
        if (!sessionDir.startsWith('session-')) continue;

        try {
          const sessionHistory = await this.getSessionHistory(sessionDir);
          allHistory.push(...sessionHistory);
        } catch (error) {
          // Skip sessions with no history or read errors
          console.error(`Failed to load history for ${sessionDir}:`, error);
        }
      }

      // Sort by timestamp (newest first)
      allHistory.sort((a, b) =>
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      );

      return allHistory;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return []; // No sessions directory yet
      }
      throw error;
    }
  }

  /**
   * Search for cards across all history
   * Searches in note fields and changes
   */
  async searchHistory(query: string): Promise<ActionHistoryEntry[]> {
    const allHistory = await this.getAllHistory();
    const lowerQuery = query.toLowerCase();

    return allHistory.filter(entry => {
      // Search in original note fields
      if (entry.original) {
        for (const field of Object.values(entry.original.fields)) {
          if (field.value.toLowerCase().includes(lowerQuery)) {
            return true;
          }
        }
      }

      // Search in changes
      for (const value of Object.values(entry.changes)) {
        if (value.toLowerCase().includes(lowerQuery)) {
          return true;
        }
      }

      return false;
    });
  }
}

export const historyService = new HistoryService();
