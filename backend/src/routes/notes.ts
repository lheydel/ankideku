import { Router, Request, Response } from 'express';
import ankiConnect from '../services/ankiConnect.js';
import cache from '../services/cache.js';
import { sendErrorResponse } from '../utils/errorHandler.js';

const router = Router();

/**
 * Batch update notes
 */
router.post('/batch-update', async (req: Request, res: Response) => {
  try {
    const { updates } = req.body as { updates: Array<{ noteId: number; fields: Record<string, string> }> };
    const results = await ankiConnect.batchUpdateNotes(updates);
    res.json({ success: true, results });
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Get a single note by ID
 */
router.get('/:noteId', async (req: Request, res: Response) => {
  try {
    const noteId = parseInt(req.params.noteId);
    const notes = await ankiConnect.notesInfo([noteId]);

    if (notes.length === 0) {
      return res.status(404).json({ error: 'Note not found' });
    }

    res.json(notes[0]);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Update a note
 */
router.put('/:noteId', async (req: Request, res: Response) => {
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
    sendErrorResponse(res, error, 500);
  }
});

export default router;
