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
  SessionData,
  SessionStateData,
  SessionState,
  ActionHistoryEntry
} from '../../../contract/types';

// Re-export SessionState const object for value access
export { SessionState } from '../../../contract/types';

// Frontend-specific types

export interface SessionMetadata {
  sessionId: string;
  timestamp: string;
  deckName: string;
  totalCards: number;
  state?: SessionStateData;
}

export interface FieldDisplayConfig {
  [modelName: string]: string; // Maps model name to field name to display
}

export interface StoreState {
  // Connection state
  ankiConnected: boolean;
  setAnkiConnected: (connected: boolean) => void;

  // Decks
  decks: DeckInfo;
  selectedDeck: string | null;
  setDecks: (decks: DeckInfo) => void;
  selectDeck: (deckName: string) => void;

  // Prompt
  prompt: string;
  setPrompt: (prompt: string) => void;
  promptHistory: string[];
  addToPromptHistory: (prompt: string) => void;

  // Force sync option
  forceSync: boolean;
  setForceSync: (forceSync: boolean) => void;

  // Processing state
  isProcessing: boolean;
  processingProgress: number;
  processingTotal: number;
  setProcessing: (isProcessing: boolean) => void;
  setProgress: (progress: number, total: number) => void;

  // Session management
  currentSession: string | null;
  currentSessionData: SessionData | null;
  sessions: SessionMetadata[];
  setCurrentSession: (sessionId: string | null) => void;
  setCurrentSessionData: (data: SessionData | null) => void;
  updateSessionState: (state: SessionStateData) => void;
  setSessions: (sessions: SessionMetadata[]) => void;

  // Queue
  queue: CardSuggestion[];
  currentIndex: number;
  setQueue: (queue: CardSuggestion[]) => void;
  addToQueue: (suggestion: CardSuggestion) => void;
  goToCard: (index: number) => void;
  skipCard: () => void;
  removeFromQueue: (index: number) => void;
  getCurrentCard: () => CardSuggestion | undefined;

  // Actions history
  actionsHistory: ActionHistoryEntry[];
  globalHistory: ActionHistoryEntry[];
  historyViewMode: 'session' | 'global';
  addToHistory: (action: Omit<ActionHistoryEntry, 'timestamp'>) => void;
  loadGlobalHistory: () => Promise<void>;
  toggleHistoryView: () => void;
  setSessionHistory: (history: ActionHistoryEntry[]) => void;

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
  original: Note;
  changes: Record<string, string>;
  reasoning?: string;
  readonly: boolean;
  status?: 'accept' | 'reject'; // Only present when readonly
  timestamp?: string; // Only present when readonly
  editedChanges?: Record<string, string>; // Manual edits made by user
}
