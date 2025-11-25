import { Router, Request, Response } from 'express';
import { ankiConnectService } from '../services/anki/AnkiConnectService.js';
import { cacheService } from '../services/anki/CacheService.js';
import { getAnkiSyncService } from '../services/anki/AnkiSyncService.js';
import { sendErrorResponse } from '../utils/errorHandler.js';
import type { GetNotesResponse, SyncResponse, CacheInfo } from '../types/index.js';

const router = Router();

/**
 * Get all deck names and IDs
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const decks = await ankiConnectService.getDeckNamesAndIds();
    res.json(decks);
  } catch (error) {
    console.error('Error fetching decks:', error);
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Get notes from a deck (cache-first with incremental sync)
 */
router.get('/:deckName/notes', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    const { forceRefresh } = req.query;

    // Check if we should use cache
    if (!forceRefresh) {
      const cachedData = await cacheService.getCachedNotes(deckName);
      if (cachedData) {
        console.log(`Serving ${cachedData.notes.length} notes from cache for deck "${deckName}" (cached at ${cachedData.timestamp})`);

        // If cache exists and has lastSyncTimestamp, do incremental sync in background
        if (cachedData.lastSyncTimestamp) {
          // Don't await - let it sync in background
          (async () => {
            try {
              console.log(`Background incremental sync for "${deckName}"...`);
              const ankiSyncService = await getAnkiSyncService();
              await ankiSyncService.syncDeck(deckName);
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

    // No cache or force refresh - sync from Anki
    console.log(`Fetching notes from Anki for deck: "${deckName}"${forceRefresh ? ' (force refresh)' : ''}`);
    const ankiSyncService = await getAnkiSyncService();
    await ankiSyncService.syncDeck(deckName);

    // Read from freshly synced cache
    const cachedData = await cacheService.getCachedNotes(deckName);
    const notes = cachedData?.notes || [];
    console.log(`Found ${notes.length} notes in deck "${deckName}"`);

    const response: GetNotesResponse = {
      notes,
      fromCache: false,
      cachedAt: new Date().toISOString(),
    };
    res.json(response);
  } catch (error) {
    console.error(`Error fetching notes for deck "${req.params.deckName}":`, error);
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Sync/refresh cache from Anki
 */
router.post('/:deckName/sync', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    console.log(`Syncing deck "${deckName}" from Anki...`);

    const ankiSyncService = await getAnkiSyncService();
    const result = await ankiSyncService.syncDeck(deckName);

    const response: SyncResponse = {
      success: true,
      count: result.notesUpdated,
      timestamp: new Date().toISOString(),
    };
    res.json(response);
  } catch (error) {
    console.error(`Error syncing deck "${req.params.deckName}":`, error);
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Get cache info for a deck
 */
router.get('/:deckName/cache-info', async (req: Request, res: Response) => {
  try {
    const { deckName } = req.params;
    const info: CacheInfo = await cacheService.getCacheInfo(deckName);
    res.json(info);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

export default router;
