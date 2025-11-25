import { useEffect, useRef, useState, useCallback } from 'react';
import { io, Socket } from 'socket.io-client';
import { SocketEvent } from '../types';
import type { SyncProgressPayload } from '../types';

export interface SyncProgress {
  step: number;
  totalSteps: number;
  stepName: string;
  processed: number;
  total: number;
}

/**
 * Hook for subscribing to sync progress via WebSocket
 */
export function useSyncProgress(deckName: string | null) {
  const socketRef = useRef<Socket | null>(null);
  const [progress, setProgress] = useState<SyncProgress | null>(null);

  const subscribe = useCallback((deck: string) => {
    if (!socketRef.current) {
      socketRef.current = io('http://localhost:3001');
    }

    // Subscribe to sync progress for this deck
    socketRef.current.emit(SocketEvent.SUBSCRIBE_SYNC, deck);
    console.log(`[SyncProgress] Subscribed to sync: ${deck}`);

    // Listen for progress updates
    socketRef.current.on(SocketEvent.SYNC_PROGRESS, (payload: SyncProgressPayload) => {
      if (payload.deckName === deck) {
        setProgress({
          step: payload.step,
          totalSteps: payload.totalSteps,
          stepName: payload.stepName,
          processed: payload.processed,
          total: payload.total,
        });
      }
    });
  }, []);

  const unsubscribe = useCallback((deck: string) => {
    if (socketRef.current) {
      socketRef.current.emit(SocketEvent.UNSUBSCRIBE_SYNC, deck);
      socketRef.current.off(SocketEvent.SYNC_PROGRESS);
      console.log(`[SyncProgress] Unsubscribed from sync: ${deck}`);
    }
    setProgress(null);
  }, []);

  const clearProgress = useCallback(() => {
    setProgress(null);
  }, []);

  // Subscribe when deck changes
  useEffect(() => {
    if (deckName) {
      subscribe(deckName);
    }

    return () => {
      if (deckName) {
        unsubscribe(deckName);
      }
    };
  }, [deckName, subscribe, unsubscribe]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      socketRef.current?.disconnect();
    };
  }, []);

  return {
    progress,
    clearProgress,
  };
}
