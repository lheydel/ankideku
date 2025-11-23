import { Router, Request, Response } from 'express';
import ankiConnect from '../services/ankiConnect.js';
import cache from '../services/cache.js';
import { sendErrorResponse } from '../utils/errorHandler.js';
import type { GetNotesResponse, SyncResponse, CacheInfo } from '../types/index.js';

const router = Router();

/**
 * Get all deck names and IDs
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const decks = await ankiConnect.getDeckNamesAndIds();
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

    const notes = await ankiConnect.getDeckNotes(deckName);
    await cache.cacheNotes(deckName, notes);

    const response: SyncResponse = {
      success: true,
      count: notes.length,
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
    const info: CacheInfo = await cache.getCacheInfo(deckName);
    res.json(info);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

export default router;
