/**
 * Sync Progress Emitter Service
 * Emits WebSocket events for deck sync progress
 */

import type { Server as SocketIOServer } from 'socket.io';
import { SocketEvent, type SyncProgressPayload } from '../../../contract/types.js';

/**
 * Handles WebSocket event emission for sync progress
 */
export class SyncProgressEmitter {
  private io: SocketIOServer | null = null;

  /**
   * Set the Socket.IO server instance
   */
  setSocketIO(io: SocketIOServer): void {
    this.io = io;
  }

  /**
   * Emit sync progress to subscribers
   */
  emitProgress(deckName: string, payload: SyncProgressPayload): void {
    if (this.io) {
      // Emit to deck-specific room
      const room = `sync:${deckName}`;
      this.io.to(room).emit(SocketEvent.SYNC_PROGRESS, payload);
      console.log(`[SyncProgressEmitter] ${payload.stepName} ${payload.processed}/${payload.total} for ${deckName}`);
    }
  }
}

// Singleton instance
export const syncProgressEmitter = new SyncProgressEmitter();
