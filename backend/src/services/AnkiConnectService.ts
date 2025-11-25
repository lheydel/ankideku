/**
 * AnkiConnect Service
 * Thin wrapper around the AnkiConnect API
 */

import axios, { AxiosError } from 'axios';
import type { Note, DeckInfo, NoteUpdate, AnkiConnectResponse } from '../types/index.js';
import { CONFIG } from '../config.js';

export class AnkiConnectService {
  /**
   * Make a request to AnkiConnect
   */
  async request<T = any>(action: string, params: Record<string, any> = {}, timeout: number = CONFIG.anki.requestTimeout): Promise<T> {
    try {
      const response = await axios.post<AnkiConnectResponse<T>>(CONFIG.anki.url, {
        action,
        version: CONFIG.anki.version,
        params
      }, {
        timeout
      });

      if (response.data.error) {
        throw new Error(`AnkiConnect error: ${response.data.error}`);
      }

      return response.data.result;
    } catch (error) {
      const axiosError = error as AxiosError;
      if (axiosError.code === 'ECONNREFUSED') {
        throw new Error('AnkiConnect is not running. Please start Anki with the AnkiConnect addon installed.');
      }
      if (axiosError.code === 'ECONNRESET') {
        throw new Error('Connection reset by AnkiConnect. The request may be too large.');
      }
      if (axiosError.code === 'ETIMEDOUT') {
        throw new Error('Request timed out. Try reducing the number of cards.');
      }
      throw error;
    }
  }

  /**
   * Check if AnkiConnect is accessible
   */
  async ping(): Promise<boolean> {
    try {
      const version = await this.request<number>('version');
      return version === CONFIG.anki.version;
    } catch {
      return false;
    }
  }

  /**
   * Get all deck names
   */
  async getDeckNames(): Promise<string[]> {
    return await this.request<string[]>('deckNames');
  }

  /**
   * Get deck names with IDs
   */
  async getDeckNamesAndIds(): Promise<DeckInfo> {
    return await this.request<DeckInfo>('deckNamesAndIds');
  }

  /**
   * Find notes matching a query
   */
  async findNotes(query: string): Promise<number[]> {
    return await this.request<number[]>('findNotes', { query });
  }

  /**
   * Get detailed information about notes
   */
  async notesInfo(noteIds: number[]): Promise<Note[]> {
    return await this.request<Note[]>('notesInfo', { notes: noteIds });
  }

  /**
   * Get detailed information about cards (includes deck names)
   */
  async cardsInfo(cardIds: number[]): Promise<Array<{ cardId: number; deckName: string }>> {
    return await this.request<Array<{ cardId: number; deckName: string }>>('cardsInfo', { cards: cardIds });
  }

  /**
   * Update fields of a note
   */
  async updateNoteFields(noteId: number, fields: Record<string, string>): Promise<null> {
    return await this.request<null>('updateNoteFields', {
      note: {
        id: noteId,
        fields
      }
    });
  }

  /**
   * Batch update multiple notes
   */
  async batchUpdateNotes(updates: NoteUpdate[]): Promise<any[]> {
    const actions = updates.map(({ noteId, fields }) => ({
      action: 'updateNoteFields',
      params: {
        note: {
          id: noteId,
          fields
        }
      }
    }));

    return await this.request<any[]>('multi', { actions });
  }
}

// Singleton instance
export const ankiConnectService = new AnkiConnectService();
