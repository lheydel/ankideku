import { create } from 'zustand';
import type { StoreState, DeckInfo, CardSuggestion, ActionHistoryEntry, SessionData, SessionMetadata, ComparisonCard, SessionStateData } from '../types/index.js';

const useStore = create<StoreState>((set, get) => ({
  // Connection state
  ankiConnected: false,
  setAnkiConnected: (connected: boolean) => set({ ankiConnected: connected }),

  // Decks
  decks: {},
  selectedDeck: null,
  setDecks: (decks: DeckInfo) => set({ decks }),
  selectDeck: (deckName: string) => set({ selectedDeck: deckName }),

  // Prompt
  prompt: '',
  setPrompt: (prompt: string) => set({ prompt }),
  promptHistory: [],
  addToPromptHistory: (prompt: string) => set((state) => ({
    promptHistory: [prompt, ...state.promptHistory.filter(p => p !== prompt)].slice(0, 10)
  })),

  // Force sync option
  forceSync: false,
  setForceSync: (forceSync: boolean) => set({ forceSync }),

  // Session management
  currentSession: null,
  currentSessionData: null,
  sessions: [],
  setCurrentSession: (sessionId: string | null) => set({ currentSession: sessionId }),
  setCurrentSessionData: (data: SessionData | null) => set({ currentSessionData: data }),
  updateSessionState: (state: SessionStateData) => set((store) => {
    // Update currentSessionData
    const updatedSessionData = store.currentSessionData ? {
      ...store.currentSessionData,
      state
    } : null;

    // Update the session in the sessions list
    const updatedSessions = store.sessions.map(session =>
      session.sessionId === store.currentSession
        ? { ...session, state }
        : session
    );

    return {
      currentSessionData: updatedSessionData,
      sessions: updatedSessions
    };
  }),
  setSessions: (sessions: SessionMetadata[]) => set({ sessions }),

  // Queue (notes with suggestions)
  queue: [],
  currentIndex: 0,
  setQueue: (queue: CardSuggestion[]) => set({ queue, currentIndex: 0 }),
  addToQueue: (suggestion: CardSuggestion) => set((state) => ({
    queue: [...state.queue, suggestion]
  })),
  addBatchToQueue: (suggestions: CardSuggestion[]) => set((state) => ({
    queue: [...state.queue, ...suggestions]
  })),
  goToCard: (index: number) => set((state) => ({
    currentIndex: Math.max(0, Math.min(index, state.queue.length - 1))
  })),
  skipCard: () => {
    const state = get();
    const skipped = state.queue[state.currentIndex];
    const newQueue = [
      ...state.queue.slice(0, state.currentIndex),
      ...state.queue.slice(state.currentIndex + 1),
      skipped
    ];
    set({ queue: newQueue });
  },
  removeFromQueue: (index: number) => set((state) => ({
    queue: state.queue.filter((_, i) => i !== index),
    currentIndex: Math.min(state.currentIndex, state.queue.length - 2)
  })),

  // Current card being reviewed
  getCurrentCard: () => {
    const state = get();
    return state.queue[state.currentIndex];
  },

  // Actions history
  actionsHistory: [],
  globalHistory: [],
  historyViewMode: 'session',
  addToHistory: (action: Omit<ActionHistoryEntry, 'timestamp'>) => set((state) => ({
    actionsHistory: [...state.actionsHistory, { ...action, timestamp: new Date().toISOString() }]
  })),
  loadGlobalHistory: async () => {
    try {
      const response = await fetch('http://localhost:3001/api/history/global');
      const data = await response.json();
      set({ globalHistory: data.history || [] });
    } catch (error) {
      console.error('Failed to load global history:', error);
    }
  },
  toggleHistoryView: () => set((state) => ({
    historyViewMode: state.historyViewMode === 'session' ? 'global' : 'session'
  })),
  setSessionHistory: (history: ActionHistoryEntry[]) => set({ actionsHistory: history }),

  // Comparison view
  selectedCard: null,
  setSelectedCard: (card: ComparisonCard | null) => set({ selectedCard: card }),

  // Settings
  fieldDisplayConfig: {},
  setFieldDisplayConfig: (config) => set({ fieldDisplayConfig: config }),
  updateFieldDisplay: (modelName: string, fieldName: string) => set((state) => ({
    fieldDisplayConfig: { ...state.fieldDisplayConfig, [modelName]: fieldName }
  })),

  // Reset
  reset: () => set({
    selectedDeck: null,
    prompt: '',
    currentSession: null,
    currentSessionData: null,
    queue: [],
    currentIndex: 0,
  }),
}));

export default useStore;

// Selector for isProcessing - derived from session state
// Usage: const isProcessing = useStore(selectIsProcessing);
export const selectIsProcessing = (state: StoreState): boolean => {
  const sessionState = state.currentSessionData?.state?.state;
  return sessionState === 'pending' || sessionState === 'running';
};

// Default progress object - stable reference to avoid infinite re-renders
const DEFAULT_PROGRESS = { processed: 0, total: 0, suggestionsCount: 0, inputTokens: 0, outputTokens: 0 };

// Selector for session progress - derived from session state
// Usage: const progress = useStore(selectSessionProgress);
export const selectSessionProgress = (state: StoreState) => {
  return state.currentSessionData?.state?.progress ?? DEFAULT_PROGRESS;
};
