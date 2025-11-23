import { create } from 'zustand';
import type { StoreState, DeckInfo, CardSuggestion, ActionHistoryEntry, SessionData, SessionMetadata } from '../types/index.js';

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

  // Processing state
  isProcessing: false,
  processingProgress: 0,
  processingTotal: 0,
  setProcessing: (isProcessing: boolean) => set({ isProcessing }),
  setProgress: (progress: number, total: number) => set({ processingProgress: progress, processingTotal: total }),

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
  nextCard: () => set((state) => ({
    currentIndex: Math.min(state.currentIndex + 1, state.queue.length - 1)
  })),
  prevCard: () => set((state) => ({
    currentIndex: Math.max(state.currentIndex - 1, 0)
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
  addToHistory: (action: Omit<ActionHistoryEntry, 'timestamp'>) => set((state) => ({
    actionsHistory: [...state.actionsHistory, { ...action, timestamp: new Date().toISOString() }]
  })),

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
    isProcessing: false,
    processingProgress: 0,
    processingTotal: 0,
    currentSession: null,
    currentSessionData: null,
    queue: [],
    currentIndex: 0,
  }),
}));

export default useStore;
