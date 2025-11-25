import { useCallback } from 'react';
import useStore from '../store/useStore';
import { useSessionManagement } from './useSessionManagement';
import { useWebSocket } from './useWebSocket';
import type { CardSuggestion, SessionStateData } from '../types';
import { createComparisonCardFromSuggestion } from '../utils/cardUtils';
import { getErrorMessage } from '../utils/errorUtils';

export function useCardGeneration() {
  const {
    selectedDeck,
    setQueue,
    addToQueue,
    addToPromptHistory,
    currentSession,
    currentSessionData,
    updateSessionState,
    setSelectedCard,
    forceSync,
  } = useStore();

  const { createSession } = useSessionManagement();

  // Handle new suggestions from WebSocket
  // Note: We use a ref to avoid re-creating this callback when selectedCard changes,
  // which would cause useWebSocket to reconnect and miss subsequent suggestions
  const handleNewSuggestion = useCallback((suggestion: CardSuggestion) => {
    addToQueue(suggestion);

    // Auto-select the first suggestion if no card is currently being viewed
    // Use store.getState() to avoid dependency on selectedCard
    const currentSelectedCard = useStore.getState().selectedCard;
    if (currentSelectedCard == null) {
      setSelectedCard(createComparisonCardFromSuggestion(suggestion));
    }
  }, [addToQueue, setSelectedCard]);

  // Handle state changes from WebSocket
  // This updates currentSessionData.state which drives isProcessing and progress
  const handleStateChange = useCallback((state: SessionStateData) => {
    console.log('Session state changed:', state);
    updateSessionState(state);
  }, [updateSessionState]);

  // Handle session completion
  const handleSessionComplete = useCallback((data: { totalSuggestions: number }) => {
    console.log(`Session complete: ${data.totalSuggestions} suggestions generated`);
    // isProcessing is derived from session state, updated via state:change event
  }, []);

  // Handle session errors
  const handleSessionError = useCallback((error: { error: string }) => {
    console.error('Session error:', error.error);
    // isProcessing is derived from session state, updated via state:change event
  }, []);

  // Set up WebSocket listener
  useWebSocket({
    sessionId: currentSession,
    initialSessionState: currentSessionData?.state,
    onSuggestion: handleNewSuggestion,
    onStateChange: handleStateChange,
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
      // This will set currentSessionData with state=PENDING, making isProcessing=true
      const sessionId = await createSession(prompt, selectedDeck, forceSync);

      onSuccess(`Started AI processing session: ${sessionId}`);
      console.log(`Session ${sessionId} created. Waiting for suggestions...`);

      // WebSocket will handle incoming suggestions in real-time
      // Queue will update automatically via handleNewSuggestion callback

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
