// Re-export shared types from contract
export type {
  NoteField,
  Note,
  DeckInfo,
  GetNotesResponse,
  SyncResponse,
  CacheInfo,
  ErrorResponse,
  PingResponse,
  SessionRequest,
  CardSuggestion,
  SessionData
} from '../../../contract/types.js';

// Backend-only types

export interface AnkiConnectRequest {
  action: string;
  version: number;
  params?: Record<string, any>;
}

export interface AnkiConnectResponse<T = any> {
  result: T;
  error: string | null;
}

export interface CachedDeckData {
  deckName: string;
  notes: import('../../../contract/types.js').Note[];
  timestamp: string;
  count: number;
  lastSyncTimestamp?: number; // Unix timestamp of last sync for incremental updates
  estimatedTokens?: number; // Estimated input tokens for all cards
}

export interface NoteUpdate {
  noteId: number;
  fields: Record<string, string>;
}

export interface BatchUpdateRequest {
  updates: NoteUpdate[];
}

// Session state management (re-export from contract for consistency)
export { SessionState } from '../../../contract/types.js';
export type { SessionStateData, SessionProgress } from '../../../contract/types.js';
