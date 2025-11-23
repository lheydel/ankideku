import { Router } from 'express';
import { historyService } from '../services/historyService';
import { sessionService } from '../services/sessionService';
import { ActionHistoryEntry } from '../../../contract/types';

const router = Router();

/**
 * POST /api/sessions/:sessionId/history
 * Save a review action to the session's history
 */
router.post('/sessions/:sessionId/history', async (req, res) => {
  try {
    const { sessionId } = req.params;
    const action: ActionHistoryEntry = req.body;

    // Validate the action
    if (!action.action || !['accept', 'reject'].includes(action.action)) {
      return res.status(400).json({ error: 'Invalid action type. Must be "accept" or "reject".' });
    }

    if (!action.noteId || !action.timestamp) {
      return res.status(400).json({ error: 'Missing required fields: noteId, timestamp' });
    }

    // Save to history
    await historyService.saveAction(sessionId, action);

    // Mark the suggestion as accepted/rejected so it doesn't appear in the queue anymore
    const accepted = action.action === 'accept';
    // Pass the applied changes so the suggestion file's original can be updated to reflect the new state
    await sessionService.markSuggestionStatus(sessionId, action.noteId, accepted, accepted ? action.changes : undefined);

    res.json({ success: true, message: 'Action saved to history' });
  } catch (error) {
    console.error('Failed to save action to history:', error);
    res.status(500).json({ error: 'Failed to save action to history' });
  }
});

/**
 * GET /api/sessions/:sessionId/history
 * Get the review history for a specific session
 */
router.get('/sessions/:sessionId/history', async (req, res) => {
  try {
    const { sessionId } = req.params;
    const history = await historyService.getSessionHistory(sessionId);

    res.json({ history });
  } catch (error) {
    console.error('Failed to load session history:', error);
    res.status(500).json({ error: 'Failed to load session history' });
  }
});

/**
 * GET /api/history/global
 * Get all review history from all sessions
 */
router.get('/history/global', async (req, res) => {
  try {
    const history = await historyService.getAllHistory();

    res.json({ history });
  } catch (error) {
    console.error('Failed to load global history:', error);
    res.status(500).json({ error: 'Failed to load global history' });
  }
});

/**
 * GET /api/history/search?q=query
 * Search for cards across all history
 */
router.get('/history/search', async (req, res) => {
  try {
    const query = req.query.q as string;

    if (!query) {
      return res.status(400).json({ error: 'Missing search query parameter "q"' });
    }

    const results = await historyService.searchHistory(query);

    res.json({ history: results, query });
  } catch (error) {
    console.error('Failed to search history:', error);
    res.status(500).json({ error: 'Failed to search history' });
  }
});

export default router;
