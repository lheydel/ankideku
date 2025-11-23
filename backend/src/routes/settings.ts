import { Router, Request, Response } from 'express';
import settings from '../services/settings.js';
import { sendErrorResponse } from '../utils/errorHandler.js';
import type { FieldDisplayConfig } from '../services/settings.js';

const router = Router();

/**
 * Get user settings
 */
router.get('/', async (_req: Request, res: Response) => {
  try {
    const userSettings = await settings.loadSettings();
    res.json(userSettings);
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

/**
 * Update field display configuration
 */
router.put('/field-display', async (req: Request, res: Response) => {
  try {
    const { config } = req.body as { config: FieldDisplayConfig };
    await settings.updateFieldDisplayConfig(config);
    res.json({ success: true });
  } catch (error) {
    sendErrorResponse(res, error, 500);
  }
});

export default router;
