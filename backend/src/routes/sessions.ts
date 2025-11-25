import express, { Request, Response } from 'express';
import { SessionService } from '../services/session/SessionService.js';
import { SessionOrchestrator } from '../services/session/SessionOrchestrator.js';
import { historyService } from '../services/storage/HistoryService.js';

const router = express.Router();

// Services will be injected via router setup
let sessionService: SessionService;
let sessionOrchestrator: SessionOrchestrator;

/**
 * Initialize router with service dependencies
 */
export function initializeRouter(
  services: {
    sessionService: SessionService;
    sessionOrchestrator: SessionOrchestrator;
  }
) {
  sessionService = services.sessionService;
  sessionOrchestrator = services.sessionOrchestrator;
}

/**
 * POST /api/sessions/new
 * Create a new AI processing session
 *
 * Body: { prompt: string, deckName: string, forceSync?: boolean }
 * Returns: { sessionId: string }
 */
router.post('/new', async (req: Request, res: Response): Promise<void> => {
  try {
    const { prompt, deckName, forceSync = false } = req.body;

    // Validate input
    if (!prompt || typeof prompt !== 'string' || !prompt.trim()) {
      res.status(400).json({ error: 'Prompt is required' });
      return;
    }

    if (!deckName || typeof deckName !== 'string') {
      res.status(400).json({ error: 'Deck name is required' });
      return;
    }

    // Perform synchronous incremental sync if forceSync is true
    if (forceSync) {
      console.log(`Force sync enabled - syncing deck cache for "${deckName}" before starting AI session...`);
      const { getAnkiSyncService } = await import('../services/anki/AnkiSyncService.js');
      const ankiSyncService = await getAnkiSyncService();
      await ankiSyncService.syncDeck(deckName);
    }

    // Create session
    console.log(`Creating new session for deck "${deckName}" with prompt: "${prompt}"`);
    const sessionId = await sessionService.createSession(prompt, deckName);

    // Execute session in background (non-blocking)
    sessionOrchestrator.executeSession(sessionId)
      .then((result) => {
        console.log(`[Sessions] Session ${sessionId} completed:`, {
          suggestions: result.suggestionsGenerated,
          processed: result.processedCards,
          total: result.totalCards,
          failed: result.batchesFailed,
        });
      })
      .catch((error) => {
        console.error(`[Sessions] Session ${sessionId} failed:`, error);
      });

    res.json({ sessionId });
  } catch (error) {
    console.error('Error creating session:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to create session'
    });
  }
});

/**
 * GET /api/sessions/active
 * List all active sessions
 *
 * Returns: { activeSessions: string[], count: number }
 */
router.get('/active', (req: Request, res: Response): void => {
  try {
    const activeSessions = sessionOrchestrator.getActiveSessions();
    res.json({ activeSessions, count: activeSessions.length });
  } catch (error) {
    console.error('Error listing active sessions:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to list active sessions'
    });
  }
});

/**
 * GET /api/sessions
 * List all available sessions with metadata
 *
 * Returns: { sessions: Array<{ sessionId, timestamp, deckName, totalCards, state? }> }
 */
router.get('/', async (req: Request, res: Response): Promise<void> => {
  try {
    const sessions = await sessionService.listSessionsWithMetadata();
    res.json({ sessions });
  } catch (error) {
    console.error('Error listing sessions:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to list sessions'
    });
  }
});

/**
 * GET /api/sessions/:sessionId
 * Load an existing session with all suggestions and history
 *
 * Returns: { sessionId, request, suggestions, history, state?, cancelled? }
 */
router.get('/:sessionId', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    const request = await sessionService.getSessionRequest(sessionId);
    const suggestions = await sessionService.loadSession(sessionId);
    const history = await historyService.getSessionHistory(sessionId);
    const state = await sessionService.getSessionState(sessionId);
    const cancelled = await sessionService.getSessionCancellation(sessionId);

    res.json({
      sessionId,
      request,
      suggestions,
      history,
      ...(state && { state }),
      ...(cancelled && { cancelled })
    });
  } catch (error) {
    console.error('Error loading session:', error);
    res.status(404).json({
      error: error instanceof Error ? error.message : 'Session not found'
    });
  }
});

/**
 * DELETE /api/sessions/:sessionId
 * Delete a session and all its files
 *
 * Returns: { success: boolean }
 */
router.delete('/:sessionId', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    // Cancel if running
    if (sessionOrchestrator.isRunning(sessionId)) {
      sessionOrchestrator.cancelSession(sessionId);
    }

    // Delete session files
    await sessionService.deleteSession(sessionId);

    res.json({ success: true });
  } catch (error) {
    console.error('Error deleting session:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to delete session'
    });
  }
});

/**
 * POST /api/sessions/:sessionId/cancel
 * Cancel an ongoing session
 *
 * Returns: { success: boolean, message?: string }
 */
router.post('/:sessionId/cancel', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    if (sessionOrchestrator.isRunning(sessionId)) {
      sessionOrchestrator.cancelSession(sessionId);
      await sessionService.markSessionCancelled(sessionId);
      res.json({ success: true, message: 'Session cancelled' });
    } else {
      res.status(404).json({ error: 'Session not found or not running' });
    }
  } catch (error) {
    console.error('Error cancelling session:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to cancel session'
    });
  }
});

/**
 * GET /api/sessions/:sessionId/status
 * Get status of a session
 *
 * Returns: { sessionId, isRunning, suggestionCount, state? }
 */
router.get('/:sessionId/status', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    const state = await sessionService.getSessionState(sessionId);

    res.json({
      sessionId,
      isRunning: sessionOrchestrator.isRunning(sessionId),
      ...(state && { state })
    });
  } catch (error) {
    console.error('Error getting session status:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to get session status'
    });
  }
});

/**
 * PUT /api/sessions/:sessionId/suggestions/:noteId/refresh-original
 * Refresh a suggestion's original fields with current state from Anki
 *
 * Returns: Updated CardSuggestion
 */
router.put('/:sessionId/suggestions/:noteId/refresh-original', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId, noteId } = req.params;
    const updatedSuggestion = await sessionService.refreshSuggestionOriginal(sessionId, parseInt(noteId));
    res.json(updatedSuggestion);
  } catch (error) {
    console.error('Error refreshing suggestion original:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to refresh suggestion original'
    });
  }
});

/**
 * PUT /api/sessions/:sessionId/suggestions/:noteId/edited-changes
 * Save edited changes made by the user to a suggestion
 *
 * Body: { editedChanges: Record<string, string> }
 * Returns: { success: boolean }
 */
router.put('/:sessionId/suggestions/:noteId/edited-changes', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId, noteId } = req.params;
    const { editedChanges } = req.body;

    if (!editedChanges || typeof editedChanges !== 'object') {
      res.status(400).json({ error: 'editedChanges is required and must be an object' });
      return;
    }

    await sessionService.saveEditedChanges(sessionId, parseInt(noteId), editedChanges);
    res.json({ success: true });
  } catch (error) {
    console.error('Error saving edited changes:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to save edited changes'
    });
  }
});

/**
 * DELETE /api/sessions/:sessionId/suggestions/:noteId/edited-changes
 * Revert/remove all edited changes made by the user to a suggestion
 *
 * Returns: { success: boolean }
 */
router.delete('/:sessionId/suggestions/:noteId/edited-changes', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId, noteId } = req.params;
    await sessionService.revertEditedChanges(sessionId, parseInt(noteId));
    res.json({ success: true });
  } catch (error) {
    console.error('Error reverting edited changes:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to revert edited changes'
    });
  }
});

export default router;
