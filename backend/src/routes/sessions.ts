import express, { Request, Response } from 'express';
import * as path from 'path';
import { SessionService } from '../services/sessionService.js';
import { FileWatcherService } from '../services/fileWatcher.js';
import { ClaudeSpawnerService } from '../services/claudeSpawner.js';
import { historyService } from '../services/historyService.js';

const router = express.Router();

// Services will be injected via router setup
let sessionService: SessionService;
let fileWatcher: FileWatcherService;
let claudeSpawner: ClaudeSpawnerService;

/**
 * Initialize router with service dependencies
 */
export function initializeRouter(
  services: {
    sessionService: SessionService;
    fileWatcher: FileWatcherService;
    claudeSpawner: ClaudeSpawnerService;
  }
) {
  sessionService = services.sessionService;
  fileWatcher = services.fileWatcher;
  claudeSpawner = services.claudeSpawner;
}

/**
 * POST /api/sessions/new
 * Create a new AI processing session
 *
 * Body: { prompt: string, deckName: string }
 * Returns: { sessionId: string }
 */
router.post('/new', async (req: Request, res: Response): Promise<void> => {
  try {
    const { prompt, deckName } = req.body;

    // Validate input
    if (!prompt || typeof prompt !== 'string' || !prompt.trim()) {
      res.status(400).json({ error: 'Prompt is required' });
      return;
    }

    if (!deckName || typeof deckName !== 'string') {
      res.status(400).json({ error: 'Deck name is required' });
      return;
    }

    // Create session
    console.log(`Creating new session for deck "${deckName}" with prompt: "${prompt}"`);
    const sessionId = await sessionService.createSession(prompt, deckName);

    // Get session directory
    const sessionDir = sessionService.getSessionDir(sessionId);

    // Start file watcher for this session
    await fileWatcher.watchSession(sessionId, sessionDir);

    // Spawn Claude Code CLI in background (non-blocking)
    // Mark as RUNNING when spawning starts
    sessionService.markSessionRunning(sessionId);

    claudeSpawner.spawnClaude({ sessionId, sessionDir })
      .then((result) => {
        console.log(`Claude processing completed for session: ${sessionId}`);
        // Mark as COMPLETED with exit code
        sessionService.markSessionCompleted(sessionId, result.exitCode);
      })
      .catch(async (error) => {
        console.error(`Claude processing failed for session ${sessionId}:`, error);

        // Check if session was already marked as cancelled (don't overwrite!)
        const currentState = await sessionService.getSessionState(sessionId);
        if (currentState?.state === 'cancelled') {
          console.log(`Session ${sessionId} was cancelled, keeping CANCELLED state`);
          return;
        }

        // Mark as FAILED with error message
        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        sessionService.markSessionFailed(sessionId, errorMessage);
      })
      .finally(() => {
        // Stop watching after a delay to ensure all files are caught
        // This runs whether Claude succeeded or failed, catching any partial results
        setTimeout(() => {
          fileWatcher.unwatchSession(sessionId);
        }, 2000);
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
 * List all sessions with active Claude processes
 *
 * Returns: { activeSessions: string[] }
 */
router.get('/active', (req: Request, res: Response): void => {
  try {
    const activeSessions = claudeSpawner.getActiveSessions();
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
 * Returns: { sessions: Array<{ sessionId: string, timestamp: string, deckName: string, totalCards: number }> }
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
 * Returns: { sessionId: string, request: SessionRequest, suggestions: CardSuggestion[], history: ActionHistoryEntry[], state?: SessionStateData, cancelled?: { cancelled: true, timestamp: string } }
 */
router.get('/:sessionId', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    // Load session request
    const request = await sessionService.getSessionRequest(sessionId);

    // Load all suggestions
    const suggestions = await sessionService.loadSession(sessionId);

    // Load history
    const history = await historyService.getSessionHistory(sessionId);

    // Get session state
    const state = await sessionService.getSessionState(sessionId);

    // Check if session was cancelled (backwards compatibility)
    const cancelled = await sessionService.getSessionCancellation(sessionId);

    // If session is still running, start watching it for real-time updates
    if (state && (state.state === 'pending' || state.state === 'running')) {
      const sessionDir = sessionService.getSessionDir(sessionId);
      if (!fileWatcher.isWatching(sessionId)) {
        await fileWatcher.watchSession(sessionId, sessionDir);
        console.log(`Started watching session ${sessionId} (state: ${state.state})`);
      }
    }

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

    // Stop watching if active
    if (fileWatcher.isWatching(sessionId)) {
      await fileWatcher.unwatchSession(sessionId);
    }

    // Kill Claude process if running
    if (claudeSpawner.isRunning(sessionId)) {
      claudeSpawner.killProcess(sessionId);
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
 * Cancel an ongoing session (kill Claude process)
 *
 * Returns: { success: boolean }
 */
router.post('/:sessionId/cancel', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    const killed = claudeSpawner.killProcess(sessionId);

    if (killed) {
      // Mark session as cancelled in the filesystem
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
 * PUT /api/sessions/:sessionId/suggestions/:noteId/refresh-original
 * Refresh a suggestion's original fields with current state from Anki
 *
 * Returns: Updated CardSuggestion
 */
router.put('/:sessionId/suggestions/:noteId/refresh-original', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId, noteId } = req.params;
    const noteIdNum = parseInt(noteId);

    const updatedSuggestion = await sessionService.refreshSuggestionOriginal(sessionId, noteIdNum);

    res.json(updatedSuggestion);
  } catch (error) {
    console.error('Error refreshing suggestion original:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to refresh suggestion original'
    });
  }
});

/**
 * GET /api/sessions/:sessionId/status
 * Get status of a session
 *
 * Returns: { sessionId: string, isRunning: boolean, isWatching: boolean, suggestionCount: number, state?: SessionStateData }
 */
router.get('/:sessionId/status', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    // Get session state
    const state = await sessionService.getSessionState(sessionId);

    const status = {
      sessionId,
      isRunning: claudeSpawner.isRunning(sessionId),
      isWatching: fileWatcher.isWatching(sessionId),
      suggestionCount: fileWatcher.getSuggestionCount(sessionId),
      ...(state && { state })
    };

    res.json(status);
  } catch (error) {
    console.error('Error getting session status:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to get session status'
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

/**
 * GET /api/sessions/:sessionId/output
 * Get Claude output logs for a session
 *
 * Query params:
 * - type: 'combined' (default), 'stdout', or 'stderr'
 *
 * Returns: Raw log file content
 */
router.get('/:sessionId/output', async (req: Request, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;
    const type = (req.query.type as string) || 'combined';

    const sessionDir = sessionService.getSessionDir(sessionId);
    const logsDir = path.join(sessionDir, 'logs');
    let logFile: string;

    switch (type) {
      case 'stdout':
        logFile = 'claude-stdout.log';
        break;
      case 'stderr':
        logFile = 'claude-stderr.log';
        break;
      case 'combined':
      default:
        logFile = 'claude-output.log';
        break;
    }

    const logPath = path.join(logsDir, logFile);

    // Check if file exists
    const fs = await import('fs/promises');
    try {
      await fs.access(logPath);
    } catch {
      res.status(404).json({ error: 'Output log not found' });
      return;
    }

    // Read and return log content
    const content = await fs.readFile(logPath, 'utf-8');
    res.type('text/plain').send(content);
  } catch (error) {
    console.error('Error reading output log:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to read output log'
    });
  }
});

export default router;
