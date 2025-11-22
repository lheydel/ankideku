/**
 * Shared types between frontend and backend
 * This ensures type consistency across the full-stack application
 */

// ============================================================================
// Core Anki Types
// ============================================================================

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

// ============================================================================
// API Response Types
// ============================================================================

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

export interface PingResponse {
  connected: boolean;
  error?: string;
}

// ============================================================================
// AI Session Types
// ============================================================================

export interface SessionRequest {
  sessionId: string;
  prompt: string;
  deckName: string;
  deckPaths: string[]; // All deck files to process (parent + subdecks)
  totalCards: number;
  timestamp: string;
}

export interface CardSuggestion {
  noteId: number;
  original: Note;
  changes: Record<string, string>;
  reasoning: string;
}

export interface SessionData {
  sessionId: string;
  request: SessionRequest;
  suggestions: CardSuggestion[];
  cancelled?: {
    cancelled: true;
    timestamp: string;
  };
}
