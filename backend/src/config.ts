/**
 * Centralized configuration for the AnkiDeku backend
 * All configuration values should be defined here
 */

export const CONFIG = {
  /**
   * Server configuration
   */
  server: {
    port: parseInt(process.env.PORT || '3001', 10),
    corsOrigin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  },

  /**
   * AnkiConnect configuration
   */
  anki: {
    url: process.env.ANKI_CONNECT_URL || 'http://localhost:8765',
    version: 6,
    requestTimeout: 60000, // 60 seconds
    batchSize: 50,
    batchDelay: 50, // ms between batch requests
  },

  /**
   * File watcher configuration
   */
  fileWatcher: {
    stabilityThreshold: 100, // ms to wait for file stability
    pollInterval: 50, // ms between polls
  },

  /**
   * Logging configuration
   */
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
} as const;

/**
 * Validate required configuration on startup
 */
export function validateConfig(): void {
  const errors: string[] = [];

  if (CONFIG.server.port < 1 || CONFIG.server.port > 65535) {
    errors.push(`Invalid port: ${CONFIG.server.port}`);
  }

  if (!CONFIG.anki.url.startsWith('http')) {
    errors.push(`Invalid AnkiConnect URL: ${CONFIG.anki.url}`);
  }

  if (errors.length > 0) {
    throw new Error(`Configuration validation failed:\n${errors.join('\n')}`);
  }
}
