import express, { Request, Response } from 'express';
import cors from 'cors';
import ankiConnect from './services/ankiConnect.js';
import cache from './services/cache.js';
import type { GetNotesResponse, SyncResponse, CacheInfo, ErrorResponse } from './types/index.js';

const app = express();
const PORT = 3001;

// Middleware
app.use(cors());
app.use(express.json());

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

// Start server
app.listen(PORT, () => {
  console.log(`AnkiDeku backend running on http://localhost:${PORT}`);
});
