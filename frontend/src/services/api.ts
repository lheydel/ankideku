import axios from 'axios';
import type {
  PingResponse,
  DeckInfo,
  GetNotesResponse,
  SyncResponse,
  CacheInfo,
  FieldDisplayConfig,
} from '../types/index.js';

interface UserSettings {
  fieldDisplayConfig: FieldDisplayConfig;
}

const API_BASE_URL = 'http://localhost:3001/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const ankiApi = {
  // Check AnkiConnect connection
  ping: async (): Promise<PingResponse> => {
    const response = await api.get<PingResponse>('/anki/ping');
    return response.data;
  },

  // Get all decks
  getDecks: async (): Promise<DeckInfo> => {
    const response = await api.get<DeckInfo>('/decks');
    return response.data;
  },

  // Get notes from a deck (cache-first)
  getDeckNotes: async (deckName: string, forceRefresh = false): Promise<GetNotesResponse> => {
    const url = `/decks/${encodeURIComponent(deckName)}/notes${forceRefresh ? '?forceRefresh=true' : ''}`;
    const response = await api.get<GetNotesResponse>(url);
    return response.data;
  },

  // Sync/refresh deck cache from Anki
  syncDeck: async (deckName: string): Promise<SyncResponse> => {
    const response = await api.post<SyncResponse>(`/decks/${encodeURIComponent(deckName)}/sync`);
    return response.data;
  },

  // Get cache info for a deck
  getCacheInfo: async (deckName: string): Promise<CacheInfo> => {
    const response = await api.get<CacheInfo>(`/decks/${encodeURIComponent(deckName)}/cache-info`);
    return response.data;
  },

  // Update a note
  updateNote: async (
    noteId: number,
    fields: Record<string, string>,
    deckName: string | null = null
  ): Promise<{ success: boolean }> => {
    const response = await api.put<{ success: boolean }>(`/notes/${noteId}`, { fields, deckName });
    return response.data;
  },

  // Batch update notes
  batchUpdateNotes: async (updates: Array<{ noteId: number; fields: Record<string, string> }>): Promise<{ success: boolean; results: any[] }> => {
    const response = await api.post<{ success: boolean; results: any[] }>('/notes/batch-update', { updates });
    return response.data;
  },

  // Get user settings
  getSettings: async (): Promise<UserSettings> => {
    const response = await api.get<UserSettings>('/settings');
    return response.data;
  },

  // Update field display configuration
  updateFieldDisplayConfig: async (config: FieldDisplayConfig): Promise<{ success: boolean }> => {
    const response = await api.put<{ success: boolean }>('/settings/field-display', { config });
    return response.data;
  },
};

export default api;
