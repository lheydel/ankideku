import { useCallback, useState } from 'react';
import useStore from '../store/useStore';
import { useSessionManagement } from './useSessionManagement';
import { useWebSocket } from './useWebSocket';
import type { CardSuggestion, SessionStateData } from '../types';

export function useCardGeneration() {
  const {
    selectedDeck,
    setQueue,
    addToQueue,
    setProcessing,
    setProgress,
    addToPromptHistory,
    currentSession,
    currentSessionData,
    updateSessionState,
    selectedCard,
    setSelectedCard,
  } = useStore();

  const { createSession, loadSession } = useSessionManagement();
  const [suggestionCount, setSuggestionCount] = useState(0);

  // Handle new suggestions from WebSocket
  const handleNewSuggestion = useCallback((suggestion: CardSuggestion) => {
    addToQueue(suggestion);
    setSuggestionCount(prev => prev + 1);

    // Auto-select the first suggestion if no card is currently being viewed
    if (selectedCard == null) {
      setSelectedCard({
        noteId: suggestion.noteId,
        original: suggestion.original,
        changes: suggestion.changes,
        reasoning: suggestion.reasoning,
        readonly: false
      });
    }
  }, [addToQueue, selectedCard, setSelectedCard]);

  // Handle state changes from WebSocket
  const handleStateChange = useCallback((state: SessionStateData) => {
    console.log('Session state changed:', state);
    // Update both currentSessionData and sessions list
    updateSessionState(state);
  }, [updateSessionState]);

  // Handle session completion
  const handleSessionComplete = useCallback(async (data: { totalSuggestions: number }) => {
    console.log(`Session complete: ${data.totalSuggestions} suggestions generated`);
    setProcessing(false);
    // State is updated in real-time via state:change WebSocket event
  }, [setProcessing]);

  // Handle session errors
  const handleSessionError = useCallback(async (error: { error: string }) => {
    console.error('Session error:', error.error);
    setProcessing(false);
    // State is updated in real-time via state:change WebSocket event
  }, [setProcessing]);

  // Set up WebSocket listener
  useWebSocket({
    sessionId: currentSession,
    sessionState: currentSessionData?.state,
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
      setProcessing(true);
      setProgress(0, 0);
      setQueue([]); // Clear existing queue
      setSuggestionCount(0);

      addToPromptHistory(prompt);

      // Create a new AI processing session
      const sessionId = await createSession(prompt, selectedDeck);

      onSuccess(`Started AI processing session: ${sessionId}`);
      console.log(`Session ${sessionId} created. Waiting for suggestions...`);

      // WebSocket will handle incoming suggestions in real-time
      // Queue will update automatically via handleNewSuggestion callback

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create session';
      onError(errorMessage);
      setProcessing(false);
    }
  }, [selectedDeck, setQueue, setProcessing, setProgress, addToPromptHistory, createSession]);

  return {
    generateSuggestions,
    currentSession,
    suggestionCount
  };
}
