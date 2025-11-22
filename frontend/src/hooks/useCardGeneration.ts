import { useCallback, useState } from 'react';
import useStore from '../store/useStore';
import { useSessionManagement } from './useSessionManagement';
import { useWebSocket } from './useWebSocket';
import type { CardSuggestion } from '../types';

export function useCardGeneration() {
  const {
    selectedDeck,
    setQueue,
    setProcessing,
    setProgress,
    addToPromptHistory,
  } = useStore();

  const { createSession, currentSession } = useSessionManagement();
  const [suggestionCount, setSuggestionCount] = useState(0);

  // Handle new suggestions from WebSocket
  const handleNewSuggestion = useCallback((suggestion: CardSuggestion) => {
    setQueue((prev: CardSuggestion[]) => [...prev, suggestion]);
    setSuggestionCount(prev => prev + 1);
  }, [setQueue]);

  // Handle session completion
  const handleSessionComplete = useCallback((data: { totalSuggestions: number }) => {
    console.log(`Session complete: ${data.totalSuggestions} suggestions generated`);
    setProcessing(false);
  }, [setProcessing]);

  // Handle session errors
  const handleSessionError = useCallback((error: { error: string }) => {
    console.error('Session error:', error.error);
    setProcessing(false);
  }, [setProcessing]);

  // Set up WebSocket listener
  useWebSocket({
    sessionId: currentSession,
    onSuggestion: handleNewSuggestion,
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
