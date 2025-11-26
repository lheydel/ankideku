import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import type { CardSuggestion, SessionStateData, SessionData } from '../types';
import { SessionState as SessionStateEnum, SocketEvent } from '../types';

interface UseWebSocketParams {
  sessionId: string | null;
  initialSessionState?: SessionStateData;
  onSuggestion: (suggestion: CardSuggestion) => void;
  onStateChange?: (state: SessionStateData) => void;
  onSessionData?: (data: SessionData) => void;
  onSessionComplete?: (data: { totalSuggestions: number }) => void;
  onError?: (error: { error: string }) => void;
}

const TEN_MINUTES_MS = 10 * 60 * 1000;

/**
 * Check if we should skip WebSocket connection for an old completed session
 */
function shouldSkipConnection(sessionState?: SessionStateData): boolean {
  if (!sessionState) return false;

  const isFinished =
    sessionState.state === SessionStateEnum.COMPLETED ||
    sessionState.state === SessionStateEnum.FAILED ||
    sessionState.state === SessionStateEnum.CANCELLED;

  if (isFinished) {
    const completionTime = new Date(sessionState.timestamp).getTime();
    const timeSinceCompletion = Date.now() - completionTime;
    return timeSinceCompletion > TEN_MINUTES_MS;
  }

  return false;
}

export function useWebSocket({
  sessionId,
  initialSessionState,
  onSuggestion,
  onStateChange,
  onSessionData,
  onSessionComplete,
  onError
}: UseWebSocketParams) {
  const socketRef = useRef<Socket | null>(null);
  // Store initial state in ref to avoid re-running effect when state updates
  const initialStateRef = useRef(initialSessionState);

  // Update ref when sessionId changes (new session)
  useEffect(() => {
    initialStateRef.current = initialSessionState;
  }, [sessionId]); // Only update when sessionId changes, not on every state update

  useEffect(() => {
    if (!sessionId) return;

    // Skip WebSocket connection if session completed more than 10 minutes ago
    if (shouldSkipConnection(initialStateRef.current)) {
      console.log(`[WebSocket] Skipping connection for session ${sessionId} (completed > 10 min ago)`);
      return;
    }

    console.log(`[WebSocket] Connecting to session ${sessionId}`);

    // Connect to WebSocket
    socketRef.current = io('http://localhost:3001');

    // Subscribe to session with acknowledgement callback for initial data
    socketRef.current.emit(SocketEvent.SUBSCRIBE_SESSION, sessionId, (response: { success: boolean; data?: SessionData; error?: string }) => {
      if (response.success && response.data) {
        console.log(`[WebSocket] Received session data for ${sessionId}`, response.data);
        onSessionData?.(response.data);
      } else if (!response.success) {
        console.error(`[WebSocket] Failed to load session ${sessionId}:`, response.error);
        onError?.({ error: response.error || 'Failed to load session' });
      }
    });
    console.log(`[WebSocket] Subscribed to session ${sessionId}`);

    // Listen for suggestions
    socketRef.current.on(SocketEvent.SUGGESTION_NEW, (suggestion) => {
      console.log(`[WebSocket] Received ${SocketEvent.SUGGESTION_NEW} for session ${sessionId}`, suggestion);
      onSuggestion(suggestion);
    });

    // Listen for state changes (includes progress updates)
    if (onStateChange) {
      socketRef.current.on(SocketEvent.STATE_CHANGE, (state) => {
        console.log(`[WebSocket] Received ${SocketEvent.STATE_CHANGE} for session ${sessionId}`, state);
        onStateChange(state);
      });
    }

    // Listen for session completion
    if (onSessionComplete) {
      socketRef.current.on(SocketEvent.SESSION_COMPLETE, onSessionComplete);
    }

    // Listen for errors
    if (onError) {
      socketRef.current.on(SocketEvent.SESSION_ERROR, onError);
    }

    return () => {
      console.log(`[WebSocket] Disconnecting from session ${sessionId}`);
      socketRef.current?.disconnect();
    };
  }, [sessionId, onSuggestion, onStateChange, onSessionData, onSessionComplete, onError]);

  return socketRef.current;
}
