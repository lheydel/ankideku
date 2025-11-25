/**
 * LLM Routes
 * Endpoints for LLM health checks and configuration
 */

import express, { Request, Response } from 'express';
import { LLMServiceFactory } from '../services/llm/LLMServiceFactory.js';
import { DEFAULT_LLM_CONFIG } from '../services/llm/LLMService.js';

const router = express.Router();

/**
 * GET /api/llm/health
 * Check LLM provider health status
 *
 * Returns: { available: boolean, error?: string, info?: string }
 */
router.get('/health', async (req: Request, res: Response): Promise<void> => {
  try {
    const llmService = LLMServiceFactory.getInstance();
    const health = await llmService.checkHealth();

    res.json(health);
  } catch (error) {
    console.error('Error checking LLM health:', error);
    res.status(500).json({
      available: false,
      error: error instanceof Error ? error.message : 'Failed to check LLM health'
    });
  }
});

/**
 * GET /api/llm/config
 * Get current LLM configuration
 *
 * Returns: { provider: string, ... }
 */
router.get('/config', (req: Request, res: Response): void => {
  try {
    // For now, just return the default config
    // In the future, this could read from settings
    res.json({
      provider: DEFAULT_LLM_CONFIG.provider,
      availableProviders: LLMServiceFactory.getAvailableProviders(),
    });
  } catch (error) {
    console.error('Error getting LLM config:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to get LLM config'
    });
  }
});

/**
 * GET /api/llm/providers
 * Get list of available LLM providers
 *
 * Returns: { providers: string[] }
 */
router.get('/providers', (req: Request, res: Response): void => {
  try {
    const providers = LLMServiceFactory.getAvailableProviders();
    res.json({ providers });
  } catch (error) {
    console.error('Error getting LLM providers:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to get LLM providers'
    });
  }
});

export default router;
