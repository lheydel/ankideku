import axios from 'axios';
import type {
  PingResponse,
  DeckInfo,
  GetNotesResponse,
  SyncResponse,
  CacheInfo,
  FieldDisplayConfig,
  ActionHistoryEntry,
  Note,
  CardSuggestion,
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

  // Sync/refresh deck cache from Anki (incremental sync)
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

  // Save review action to history
  saveHistoryAction: async (sessionId: string, action: ActionHistoryEntry): Promise<{ success: boolean }> => {
    const response = await api.post<{ success: boolean }>(`/sessions/${sessionId}/history`, action);
    return response.data;
  },

  // Get session history
  getSessionHistory: async (sessionId: string): Promise<ActionHistoryEntry[]> => {
    const response = await api.get<{ history: ActionHistoryEntry[] }>(`/sessions/${sessionId}/history`);
    return response.data.history;
  },

  // Get global history
  getGlobalHistory: async (): Promise<ActionHistoryEntry[]> => {
    const response = await api.get<{ history: ActionHistoryEntry[] }>('/history/global');
    return response.data.history;
  },

  // Search history
  searchHistory: async (query: string): Promise<ActionHistoryEntry[]> => {
    const response = await api.get<{ history: ActionHistoryEntry[] }>(`/history/search?q=${encodeURIComponent(query)}`);
    return response.data.history;
  },

  // Save edited changes to a suggestion
  saveEditedChanges: async (sessionId: string, noteId: number, editedChanges: Record<string, string>): Promise<{ success: boolean }> => {
    const response = await api.put<{ success: boolean }>(`/sessions/${sessionId}/suggestions/${noteId}/edited-changes`, { editedChanges });
    return response.data;
  },

  // Revert/remove all edited changes from a suggestion
  revertEditedChanges: async (sessionId: string, noteId: number): Promise<{ success: boolean }> => {
    const response = await api.delete<{ success: boolean }>(`/sessions/${sessionId}/suggestions/${noteId}/edited-changes`);
    return response.data;
  },

  // Get a single note by ID
  getNote: async (noteId: number): Promise<Note> => {
    const response = await api.get<Note>(`/notes/${noteId}`);
    return response.data;
  },

  // Refresh a suggestion's original fields with current Anki state
  refreshSuggestionOriginal: async (sessionId: string, noteId: number): Promise<CardSuggestion> => {
    const response = await api.put<CardSuggestion>(`/sessions/${sessionId}/suggestions/${noteId}/refresh-original`);
    return response.data;
  },
};

// LLM API
export interface LLMHealthStatus {
  available: boolean;
  error?: string;
  info?: string;
}

export interface LLMConfig {
  provider: string;
  availableProviders: string[];
}

export const llmApi = {
  // Check LLM provider health
  checkHealth: async (): Promise<LLMHealthStatus> => {
    const response = await api.get<LLMHealthStatus>('/llm/health');
    return response.data;
  },

  // Get LLM configuration
  getConfig: async (): Promise<LLMConfig> => {
    const response = await api.get<LLMConfig>('/llm/config');
    return response.data;
  },

  // Update LLM provider
  updateConfig: async (provider: string): Promise<{ success: boolean; provider: string }> => {
    const response = await api.put<{ success: boolean; provider: string }>('/llm/config', { provider });
    return response.data;
  },

  // Get available providers
  getProviders: async (): Promise<{ providers: string[] }> => {
    const response = await api.get<{ providers: string[] }>('/llm/providers');
    return response.data;
  },
};

export default api;
