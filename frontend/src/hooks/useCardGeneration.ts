import { useCallback } from 'react';
import useStore from '../store/useStore';
import { useSessionManagement } from './useSessionManagement';
import { useWebSocket } from './useWebSocket';
import type { CardSuggestion, SessionStateData, SessionData } from '../types';
import { createComparisonCardFromSuggestion } from '../utils/cardUtils';
import { getErrorMessage } from '../utils/errorUtils';

export function useCardGeneration() {
  const {
    selectedDeck,
    setQueue,
    addBatchToQueue,
    addToPromptHistory,
    currentSession,
    currentSessionData,
    setCurrentSessionData,
    updateSessionState,
    setSelectedCard,
    forceSync,
  } = useStore();

  const { createSession } = useSessionManagement();

  // Handle new suggestion batch from WebSocket
  // Note: We use a ref to avoid re-creating this callback when selectedCard changes,
  // which would cause useWebSocket to reconnect and miss subsequent suggestions
  const handleSuggestionBatch = useCallback((suggestions: CardSuggestion[]) => {
    if (suggestions.length === 0) return;

    addBatchToQueue(suggestions);

    // Auto-select the first suggestion if no card is currently being viewed
    // Use store.getState() to avoid dependency on selectedCard
    const currentSelectedCard = useStore.getState().selectedCard;
    if (currentSelectedCard == null) {
      setSelectedCard(createComparisonCardFromSuggestion(suggestions[0]));
    }
  }, [addBatchToQueue, setSelectedCard]);

  // Handle state changes from WebSocket
  // This updates currentSessionData.state which drives isProcessing and progress
  const handleStateChange = useCallback((state: SessionStateData) => {
    updateSessionState(state);
  }, [updateSessionState]);

  // Handle full session data from WebSocket (sent on subscribe)
  const handleSessionData = useCallback((data: SessionData) => {
    setCurrentSessionData(data);
  }, [setCurrentSessionData]);

  // Handle session completion (isProcessing is derived from session state)
  const handleSessionComplete = useCallback(() => {}, []);

  // Handle session errors (isProcessing is derived from session state)
  const handleSessionError = useCallback(() => {}, []);

  // Set up WebSocket listener
  useWebSocket({
    sessionId: currentSession,
    initialSessionState: currentSessionData?.state,
    onSuggestionBatch: handleSuggestionBatch,
    onStateChange: handleStateChange,
    onSessionData: handleSessionData,
    onSessionComplete: handleSessionComplete,
    onError: handleSessionError
  });

  const generateSuggestions = useCallback(async (
    prompt: string,
    onSuccess: (message: string) => void,
    onError: (message: string) => void
  ) => {
    if (!selectedDeck || !prompt.trim()) return;

    try {
      setQueue([]); // Clear existing queue

      addToPromptHistory(prompt);

      // Create a new AI processing session
      const sessionId = await createSession(prompt, selectedDeck, forceSync);
      onSuccess(`Started AI processing session: ${sessionId}`);

    } catch (err) {
      onError(getErrorMessage(err, 'Failed to create session'));
      // No session was created, so isProcessing stays false (no currentSessionData)
    }
  }, [selectedDeck, setQueue, addToPromptHistory, createSession, forceSync]);

  return {
    generateSuggestions,
    currentSession,
  };
}
