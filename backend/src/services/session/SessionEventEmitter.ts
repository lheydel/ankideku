/**
 * Session Event Emitter Service
 * Single responsibility: Emit WebSocket events for session lifecycle
 */

import type { Server as SocketIOServer } from 'socket.io';
import {
  SocketEvent,
  type SuggestionNewPayload,
  type StateChangePayload,
  type SessionCompletePayload,
  type SessionErrorPayload,
} from '../../../../contract/types.js';

/**
 * Handles all WebSocket event emission for sessions
 */
export class SessionEventEmitter {
  private io: SocketIOServer | null = null;

  /**
   * Set the Socket.IO server instance
   */
  setSocketIO(io: SocketIOServer): void {
    this.io = io;
  }

  /**
   * Emit a new suggestion to subscribers
   */
  emitSuggestion(sessionId: string, suggestion: SuggestionNewPayload): void {
    this.emit(sessionId, SocketEvent.SUGGESTION_NEW, suggestion, `noteId: ${suggestion.noteId}`);
  }

  /**
   * Emit session state change (includes progress)
   */
  emitStateChange(sessionId: string, state: StateChangePayload): void {
    this.emit(sessionId, SocketEvent.STATE_CHANGE, state, state.state);
  }

  /**
   * Emit session completion
   */
  emitComplete(sessionId: string, totalSuggestions: number): void {
    const payload: SessionCompletePayload = { totalSuggestions };
    this.emit(sessionId, SocketEvent.SESSION_COMPLETE, payload, `${totalSuggestions} suggestions`);
  }

  /**
   * Emit session error
   */
  emitError(sessionId: string, error: string): void {
    const payload: SessionErrorPayload = { error };
    this.emit(sessionId, SocketEvent.SESSION_ERROR, payload, error);
  }

  private emit(sessionId: string, event: SocketEvent, data: unknown, logMessage?: string): void {
    if (this.io) {
      this.io.to(sessionId).emit(event, data);
      console.log(`[SessionEventEmitter] ${event} for ${sessionId}: ${logMessage ?? ''}`);
    }
  }
}

// Singleton instance
export const sessionEventEmitter = new SessionEventEmitter();
