import express, { Request, Response } from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { Server as SocketIOServer } from 'socket.io';
import ankiConnect from './services/ankiConnect.js';
import cache from './services/cache.js';
import settings from './services/settings.js';
import { SessionService } from './services/sessionService.js';
import { FileWatcherService } from './services/fileWatcher.js';
import { ClaudeSpawnerService } from './services/claudeSpawner.js';
import sessionsRouter, { initializeRouter } from './routes/sessions.js';
import type { GetNotesResponse, SyncResponse, CacheInfo, ErrorResponse } from './types/index.js';
import type { FieldDisplayConfig } from './services/settings.js';

const app = express();
const PORT = 3001;

// Create HTTP server for Socket.IO
const httpServer = createServer(app);

// Initialize Socket.IO
const io = new SocketIOServer(httpServer, {
  cors: {
    origin: "http://localhost:5173", // Frontend URL
    methods: ["GET", "POST"]
  }
});

// Initialize AI workflow services
const sessionService = new SessionService();
const fileWatcher = new FileWatcherService();
const claudeSpawner = new ClaudeSpawnerService();

// Initialize sessions router with service dependencies
initializeRouter({ sessionService, fileWatcher, claudeSpawner });

// Middleware
app.use(cors());
app.use(express.json());

// WebSocket connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  // Subscribe to session updates
  socket.on('subscribe:session', (sessionId: string) => {
    socket.join(sessionId);
    console.log(`Client ${socket.id} subscribed to session: ${sessionId}`);
  });

  // Unsubscribe from session
  socket.on('unsubscribe:session', (sessionId: string) => {
    socket.leave(sessionId);
    console.log(`Client ${socket.id} unsubscribed from session: ${sessionId}`);
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Listen to file watcher events and emit via WebSocket
fileWatcher.on('suggestion:new', ({ sessionId, suggestion }) => {
  io.to(sessionId).emit('suggestion:new', suggestion);
  console.log(`Sent suggestion to session ${sessionId}: note ${suggestion.noteId}`);
});

fileWatcher.on('state:change', ({ sessionId, state }) => {
  console.log(`[WebSocket] Emitting state:change to session ${sessionId}:`, state);
  io.to(sessionId).emit('state:change', state);
  console.log(`State changed for session ${sessionId}:`, state.state);
});

fileWatcher.on('session:complete', ({ sessionId, totalSuggestions }) => {
  io.to(sessionId).emit('session:complete', { totalSuggestions });
  console.log(`Session ${sessionId} complete: ${totalSuggestions} suggestions`);
});

fileWatcher.on('error', ({ sessionId, error }) => {
  io.to(sessionId).emit('session:error', { error });
  console.error(`Session ${sessionId} error:`, error);
});

// Mount sessions routes
app.use('/api/sessions', sessionsRouter);

// Health check
app.get('/api/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok' });
});

// Check AnkiConnect connection
app.get('/api/anki/ping', async (_req: Request, res: Response) => {
  try {
    const isConnected = await ankiConnect.ping();
    res.json({ connected: isConnected });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage, connected: false });
  }
});

// Get all decks
app.get('/api/decks', async (_req: Request, res: Response) => {
  try {
    const decks = await ankiConnect.getDeckNamesAndIds();
    res.json(decks);
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Get notes from a deck (cache-first with incremental sync)
app.get('/api/decks/:deckName/notes', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    const { forceRefresh } = req.query;

    // Check if we should use cache
    if (!forceRefresh) {
      const cachedData = await cache.getCachedNotes(deckName);
      if (cachedData) {
        console.log(`Serving ${cachedData.notes.length} notes from cache for deck "${deckName}" (cached at ${cachedData.timestamp})`);

        // If cache exists and has lastSyncTimestamp, do incremental sync in background
        if (cachedData.lastSyncTimestamp) {
          // Don't await - let it sync in background
          (async () => {
            try {
              console.log(`Background incremental sync for "${deckName}"...`);
              const modifiedNotes = await ankiConnect.getDeckNotes(deckName, cachedData.lastSyncTimestamp);
              if (modifiedNotes.length > 0) {
                await cache.cacheNotes(deckName, modifiedNotes, true);
                console.log(`Background sync complete: ${modifiedNotes.length} notes updated`);
              } else {
                console.log(`Background sync complete: no modifications`);
              }
            } catch (error) {
              console.error(`Background sync failed:`, error);
            }
          })();
        }

        const response: GetNotesResponse = {
          notes: cachedData.notes,
          fromCache: true,
          cachedAt: cachedData.timestamp,
        };
        return res.json(response);
      }
    }

    // No cache or force refresh - fetch from Anki
    console.log(`Fetching notes from Anki for deck: "${deckName}"${forceRefresh ? ' (force refresh)' : ''}`);
    const notes = await ankiConnect.getDeckNotes(deckName);
    console.log(`Found ${notes.length} notes in deck "${deckName}"`);

    // Cache the results
    await cache.cacheNotes(deckName, notes, false);

    const response: GetNotesResponse = {
      notes,
      fromCache: false,
      cachedAt: new Date().toISOString(),
    };
    res.json(response);
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error(`Error fetching notes for deck "${req.params.deckName}":`, errorMessage);
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Sync/refresh cache from Anki
app.post('/api/decks/:deckName/sync', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    console.log(`Syncing deck "${deckName}" from Anki...`);

    const notes = await ankiConnect.getDeckNotes(deckName);
    await cache.cacheNotes(deckName, notes);

    const response: SyncResponse = {
      success: true,
      count: notes.length,
      timestamp: new Date().toISOString(),
    };
    res.json(response);
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error(`Error syncing deck "${req.params.deckName}":`, errorMessage);
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Get cache info for a deck
app.get('/api/decks/:deckName/cache-info', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    const info: CacheInfo = await cache.getCacheInfo(deckName);
    res.json(info);
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Update a note
app.put('/api/notes/:noteId', async (req: Request, res: Response) => {
  try {
    const noteId = parseInt(req.params.noteId);
    const { fields, deckName } = req.body as { fields: Record<string, string>; deckName?: string };

    // Update in Anki
    await ankiConnect.updateNoteFields(noteId, fields);

    // Update in cache if deckName provided
    if (deckName) {
      await cache.updateNoteInCache(deckName, noteId, fields);
    }

    res.json({ success: true });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Batch update notes
app.post('/api/notes/batch-update', async (req: Request, res: Response) => {
  try {
    const { updates } = req.body as { updates: Array<{ noteId: number; fields: Record<string, string> }> };
    const results = await ankiConnect.batchUpdateNotes(updates);
    res.json({ success: true, results });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Get user settings
app.get('/api/settings', async (_req: Request, res: Response) => {
  try {
    const userSettings = await settings.loadSettings();
    res.json(userSettings);
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Update field display configuration
app.put('/api/settings/field-display', async (req: Request, res: Response) => {
  try {
    const { config } = req.body as { config: FieldDisplayConfig };
    await settings.updateFieldDisplayConfig(config);
    res.json({ success: true });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: errorMessage } as ErrorResponse);
  }
});

// Graceful shutdown handling
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully...');
  await fileWatcher.unwatchAll();
  claudeSpawner.killAll();
  httpServer.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

process.on('SIGINT', async () => {
  console.log('SIGINT received, shutting down gracefully...');
  await fileWatcher.unwatchAll();
  claudeSpawner.killAll();
  httpServer.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

// Start server
httpServer.listen(PORT, () => {
  console.log(`AnkiDeku backend running on http://localhost:${PORT}`);
  console.log(`WebSocket server ready for connections`);
});
