import { Router, Request, Response } from 'express';
import ankiConnect from '../services/ankiConnect.js';
import { sendErrorResponse } from '../utils/errorHandler.js';
import type { ErrorResponse, PingResponse } from '../types/index.js';

const router = Router();

/**
 * Check AnkiConnect connection
 */
router.get('/ping', async (_req: Request, res: Response) => {
  try {
    const isConnected = await ankiConnect.ping();
    res.json({ connected: isConnected } as PingResponse);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Get all decks
 */
router.get('/decks', async (_req: Request, res: Response) => {
  try {
    const decks = await ankiConnect.getDeckNamesAndIds();
    res.json(decks);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

export default router;
