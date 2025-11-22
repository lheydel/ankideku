import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import type { CardSuggestion } from '../types';

interface UseWebSocketParams {
  sessionId: string | null;
  onSuggestion: (suggestion: CardSuggestion) => void;
  onSessionComplete?: (data: { totalSuggestions: number }) => void;
  onError?: (error: { error: string }) => void;
}

export function useWebSocket({
  sessionId,
  onSuggestion,
  onSessionComplete,
  onError
}: UseWebSocketParams) {
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!sessionId) return;

    // Connect to WebSocket
    socketRef.current = io('http://localhost:3001');

    // Subscribe to session
    socketRef.current.emit('subscribe:session', sessionId);

    // Listen for suggestions
    socketRef.current.on('suggestion:new', onSuggestion);

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
  }, [sessionId, onSuggestion, onSessionComplete, onError]);

  return socketRef.current;
}
