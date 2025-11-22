// AnkiConnect API Types

export interface AnkiConnectRequest {
  action: string;
  version: number;
  params?: Record<string, any>;
}

export interface AnkiConnectResponse<T = any> {
  result: T;
  error: string | null;
}

export interface NoteField {
  value: string;
  order: number;
}

export interface Note {
  noteId: number;
  modelName: string;
  fields: Record<string, NoteField>;
  tags: string[];
  cards: number[];
  mod: number;
  deckName?: string;
}

export interface DeckInfo {
  [deckName: string]: number; // deck name -> deck ID
}

export interface CachedDeckData {
  deckName: string;
  notes: Note[];
  timestamp: string;
  count: number;
  lastSyncTimestamp?: number; // Unix timestamp of last sync for incremental updates
}

export interface NoteUpdate {
  noteId: number;
  fields: Record<string, string>;
}

export interface BatchUpdateRequest {
  updates: NoteUpdate[];
}

// API Response Types
export interface GetNotesResponse {
  notes: Note[];
  fromCache: boolean;
  cachedAt: string;
}

export interface SyncResponse {
  success: boolean;
  count: number;
  timestamp: string;
}

export interface CacheInfo {
  exists: boolean;
  timestamp?: string;
  count?: number;
  deckName?: string;
}

export interface ErrorResponse {
  error: string;
}
