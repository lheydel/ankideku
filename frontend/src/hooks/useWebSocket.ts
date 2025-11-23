import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import type { CardSuggestion, SessionStateData, SessionState } from '../types';
import { SessionState as SessionStateEnum } from '../types';

interface UseWebSocketParams {
  sessionId: string | null;
  sessionState?: SessionStateData; // Current session state
  onSuggestion: (suggestion: CardSuggestion) => void;
  onStateChange?: (state: SessionStateData) => void;
  onSessionComplete?: (data: { totalSuggestions: number }) => void;
  onError?: (error: { error: string }) => void;
}

const TEN_MINUTES_MS = 10 * 60 * 1000;

export function useWebSocket({
  sessionId,
  sessionState,
  onSuggestion,
  onStateChange,
  onSessionComplete,
  onError
}: UseWebSocketParams) {
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!sessionId) return;

    // Skip WebSocket connection if session completed more than 10 minutes ago
    if (sessionState) {
      const isFinished =
        sessionState.state === SessionStateEnum.COMPLETED ||
        sessionState.state === SessionStateEnum.FAILED ||
        sessionState.state === SessionStateEnum.CANCELLED;

      if (isFinished) {
        const completionTime = new Date(sessionState.timestamp).getTime();
        const timeSinceCompletion = Date.now() - completionTime;

        if (timeSinceCompletion > TEN_MINUTES_MS) {
          console.log(`[WebSocket] Skipping connection for session ${sessionId} (completed ${Math.round(timeSinceCompletion / 60000)} minutes ago)`);
          return;
        }
      }
    }

    console.log(`[WebSocket] Connecting to session ${sessionId}`);

    // Connect to WebSocket
    socketRef.current = io('http://localhost:3001');

    // Subscribe to session
    socketRef.current.emit('subscribe:session', sessionId);
    console.log(`[WebSocket] Subscribed to session ${sessionId}`);

    // Listen for suggestions
    socketRef.current.on('suggestion:new', (suggestion) => {
      console.log(`[WebSocket] Received suggestion:new for session ${sessionId}`, suggestion);
      onSuggestion(suggestion);
    });

    // Listen for state changes
    if (onStateChange) {
      socketRef.current.on('state:change', (state) => {
        console.log(`[WebSocket] Received state:change for session ${sessionId}`, state);
        onStateChange(state);
      });
    }

    // Listen for session completion
    if (onSessionComplete) {
      socketRef.current.on('session:complete', onSessionComplete);
    }

    // Listen for errors
    if (onError) {
      socketRef.current.on('session:error', onError);
    }

    return () => {
      socketRef.current?.disconnect();
    };
  }, [sessionId, sessionState, onSuggestion, onStateChange, onSessionComplete, onError]);

  return socketRef.current;
}
