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
  estimatedTokens?: number;
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

export const SessionState = {
  PENDING: 'pending',
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
} as const;

export type SessionState = typeof SessionState[keyof typeof SessionState];

export interface SessionProgress {
  processed: number;
  total: number;
  suggestionsCount: number;
  inputTokens: number;
  outputTokens: number;
}

export interface SessionStateData {
  state: SessionState;
  timestamp: string;
  message?: string; // Optional message for errors or additional context
  exitCode?: number | null; // For completed/failed states
  progress?: SessionProgress; // Processing progress
}

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
  accepted?: boolean | null; // null/undefined = pending, true = accepted, false = rejected
  editedChanges?: Record<string, string>; // Manual edits made by user (if any)
}

export interface SessionData {
  sessionId: string;
  request: SessionRequest;
  suggestions: CardSuggestion[];
  history?: ActionHistoryEntry[]; // Review history for this session
  state?: SessionStateData; // Current session state
  cancelled?: {
    cancelled: true;
    timestamp: string;
  };
}

// ============================================================================
// Review History Types
// ============================================================================

export interface ActionHistoryEntry {
  action: 'accept' | 'reject';
  noteId: number;
  changes: Record<string, string>; // For accept: merged result (AI + user edits). For reject: AI suggestion
  original?: Note;
  reasoning?: string;
  timestamp: string;
  sessionId?: string;
  deckName?: string;
  aiChanges?: Record<string, string>; // Original AI suggestion (for reference when user edited)
  editedChanges?: Record<string, string>; // Manual edits made by user - only fields they modified
}

// ============================================================================
// WebSocket Event Types
// ============================================================================

/** Event names for WebSocket communication */
export const SocketEvent = {
  SUGGESTION_NEW: 'suggestion:new',
  STATE_CHANGE: 'state:change',
  SESSION_COMPLETE: 'session:complete',
  SESSION_ERROR: 'session:error',
  SUBSCRIBE_SESSION: 'subscribe:session',
  UNSUBSCRIBE_SESSION: 'unsubscribe:session',
  // Sync progress events
  SYNC_PROGRESS: 'sync:progress',
  SUBSCRIBE_SYNC: 'subscribe:sync',
  UNSUBSCRIBE_SYNC: 'unsubscribe:sync',
} as const;

export type SocketEvent = typeof SocketEvent[keyof typeof SocketEvent];

/** Payload for 'suggestion:new' event */
export type SuggestionNewPayload = CardSuggestion;

/** Payload for 'state:change' event */
export type StateChangePayload = SessionStateData;

/** Payload for 'session:complete' event */
export interface SessionCompletePayload {
  totalSuggestions: number;
}

/** Payload for 'session:error' event */
export interface SessionErrorPayload {
  error: string;
}

/** Payload for 'sync:progress' event */
export interface SyncProgressPayload {
  deckName: string;
  step: number;
  totalSteps: number;
  stepName: string;
  processed: number;
  total: number;
}
