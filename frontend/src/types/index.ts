// Import shared types from contract for use in this file
import type {
  NoteField as NoteFieldType,
  Note as NoteType,
  DeckInfo as DeckInfoType,
  GetNotesResponse as GetNotesResponseType,
  SyncResponse as SyncResponseType,
  CacheInfo as CacheInfoType,
  ErrorResponse as ErrorResponseType,
  PingResponse as PingResponseType,
  SessionRequest as SessionRequestType,
  CardSuggestion as CardSuggestionType,
  SessionData as SessionDataType,
  SessionStateData as SessionStateDataType,
  SessionProgress as SessionProgressType,
  ActionHistoryEntry as ActionHistoryEntryType
} from '../../../contract/types';

// Re-export types for use by other files
export type NoteField = NoteFieldType;
export type Note = NoteType;
export type DeckInfo = DeckInfoType;
export type GetNotesResponse = GetNotesResponseType;
export type SyncResponse = SyncResponseType;
export type CacheInfo = CacheInfoType;
export type ErrorResponse = ErrorResponseType;
export type PingResponse = PingResponseType;
export type SessionRequest = SessionRequestType;
export type CardSuggestion = CardSuggestionType;
export type SessionData = SessionDataType;
export type SessionStateData = SessionStateDataType;
export type SessionProgress = SessionProgressType;
export type ActionHistoryEntry = ActionHistoryEntryType;

// SessionState is both a type and a const object
// The const is for value comparisons (e.g., SessionState.COMPLETED)
// The type is inferred from the const
export { SessionState, SocketEvent } from '../../../contract/types';

// Frontend-specific types

export interface SessionMetadata {
  sessionId: string;
  timestamp: string;
  deckName: string;
  totalCards: number;
  state?: SessionStateDataType;
}

export interface FieldDisplayConfig {
  [modelName: string]: string; // Maps model name to field name to display
}

export interface StoreState {
  // Connection state
  ankiConnected: boolean;
  setAnkiConnected: (connected: boolean) => void;

  // Decks
  decks: DeckInfoType;
  selectedDeck: string | null;
  setDecks: (decks: DeckInfoType) => void;
  selectDeck: (deckName: string) => void;

  // Prompt
  prompt: string;
  setPrompt: (prompt: string) => void;
  promptHistory: string[];
  addToPromptHistory: (prompt: string) => void;

  // Force sync option
  forceSync: boolean;
  setForceSync: (forceSync: boolean) => void;

  // Session management
  currentSession: string | null;
  currentSessionData: SessionDataType | null;
  sessions: SessionMetadata[];
  setCurrentSession: (sessionId: string | null) => void;
  setCurrentSessionData: (data: SessionDataType | null) => void;
  updateSessionState: (state: SessionStateDataType) => void;
  setSessions: (sessions: SessionMetadata[]) => void;

  // Queue
  queue: CardSuggestionType[];
  currentIndex: number;
  setQueue: (queue: CardSuggestionType[]) => void;
  addToQueue: (suggestion: CardSuggestionType) => void;
  goToCard: (index: number) => void;
  skipCard: () => void;
  removeFromQueue: (index: number) => void;
  getCurrentCard: () => CardSuggestionType | undefined;

  // Actions history
  actionsHistory: ActionHistoryEntryType[];
  globalHistory: ActionHistoryEntryType[];
  historyViewMode: 'session' | 'global';
  addToHistory: (action: Omit<ActionHistoryEntryType, 'timestamp'>) => void;
  loadGlobalHistory: () => Promise<void>;
  toggleHistoryView: () => void;
  setSessionHistory: (history: ActionHistoryEntryType[]) => void;

  // Comparison view
  selectedCard: ComparisonCard | null;
  setSelectedCard: (card: ComparisonCard | null) => void;

  // Settings
  fieldDisplayConfig: FieldDisplayConfig;
  setFieldDisplayConfig: (config: FieldDisplayConfig) => void;
  updateFieldDisplay: (modelName: string, fieldName: string) => void;

  // Reset
  reset: () => void;
}

export interface NotificationState {
  message: string;
  type: 'success' | 'error' | 'info';
}

export interface ComparisonCard {
  noteId: number;
  original: NoteType;
  changes: Record<string, string>;
  reasoning?: string;
  readonly: boolean;
  status?: 'accept' | 'reject'; // Only present when readonly
  timestamp?: string; // Only present when readonly
  editedChanges?: Record<string, string>; // Manual edits made by user
}
