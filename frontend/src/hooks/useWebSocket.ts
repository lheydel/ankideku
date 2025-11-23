import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import type { CardSuggestion, SessionStateData } from '../types';

interface UseWebSocketParams {
  sessionId: string | null;
  onSuggestion: (suggestion: CardSuggestion) => void;
  onStateChange?: (state: SessionStateData) => void;
  onSessionComplete?: (data: { totalSuggestions: number }) => void;
  onError?: (error: { error: string }) => void;
}

export function useWebSocket({
  sessionId,
  onSuggestion,
  onStateChange,
  onSessionComplete,
  onError
}: UseWebSocketParams) {
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!sessionId) return;

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
  }, [sessionId, onSuggestion, onStateChange, onSessionComplete, onError]);

  return socketRef.current;
}
