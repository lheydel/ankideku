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
} from '../../../contract/types';

// Frontend-specific types

export interface SessionMetadata {
  sessionId: string;
  timestamp: string;
  deckName: string;
  totalCards: number;
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

  // Processing state
  isProcessing: boolean;
  processingProgress: number;
  processingTotal: number;
  setProcessing: (isProcessing: boolean) => void;
  setProgress: (progress: number, total: number) => void;

  // Queue
  queue: CardSuggestion[];
  currentIndex: number;
  setQueue: (queue: CardSuggestion[]) => void;
  nextCard: () => void;
  prevCard: () => void;
  goToCard: (index: number) => void;
  skipCard: () => void;
  removeFromQueue: (index: number) => void;
  getCurrentCard: () => CardSuggestion | undefined;

  // Actions history
  actionsHistory: ActionHistoryEntry[];
  addToHistory: (action: Omit<ActionHistoryEntry, 'timestamp'>) => void;

  // Settings
  fieldDisplayConfig: FieldDisplayConfig;
  setFieldDisplayConfig: (config: FieldDisplayConfig) => void;
  updateFieldDisplay: (modelName: string, fieldName: string) => void;

  // Reset
  reset: () => void;
}

export interface ActionHistoryEntry {
  action: 'accept' | 'reject' | 'skip';
  noteId: number;
  changes: Record<string, string>;
  original?: Note;
  timestamp: string;
}

export interface NotificationState {
  message: string;
  type: 'success' | 'error' | 'info';
}
