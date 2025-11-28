/**
 * LLM Routes
 * Endpoints for LLM health checks and configuration
 */

import express, { Request, Response } from 'express';
import { LLMServiceFactory } from '../services/llm/LLMServiceFactory.js';
import { settingsService } from '../services/storage/SettingsService.js';

const router = express.Router();

/**
 * GET /api/llm/health
 * Check LLM provider health status
 *
 * Returns: { available: boolean, error?: string, info?: string }
 */
router.get('/health', async (req: Request, res: Response): Promise<void> => {
  try {
    const llmConfig = await settingsService.getLLMConfig();
    const llmService = LLMServiceFactory.getInstance(llmConfig);
    const health = await llmService.getHealth();

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
 * Returns: { provider: string, availableProviders: string[] }
 */
router.get('/config', async (req: Request, res: Response): Promise<void> => {
  try {
    const llmConfig = await settingsService.getLLMConfig();
    res.json({
      provider: llmConfig.provider,
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
 * PUT /api/llm/config
 * Update LLM provider configuration
 *
 * Body: { provider: string }
 * Returns: { success: boolean, provider: string }
 */
router.put('/config', async (req: Request, res: Response): Promise<void> => {
  try {
    const { provider } = req.body;

    if (!provider) {
      res.status(400).json({ error: 'Provider is required' });
      return;
    }

    // Validate provider is available
    const availableProviders = LLMServiceFactory.getAvailableProviders();
    if (!availableProviders.includes(provider)) {
      res.status(400).json({
        error: `Invalid provider: ${provider}. Available: ${availableProviders.join(', ')}`
      });
      return;
    }

    // Update settings
    await settingsService.updateLLMConfig({ provider });

    // Clear factory instance so next request uses new provider
    LLMServiceFactory.clearInstance();

    res.json({ success: true, provider });
  } catch (error) {
    console.error('Error updating LLM config:', error);
    res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to update LLM config'
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
