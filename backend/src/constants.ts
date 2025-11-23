import * as path from 'path';
import { fileURLToPath } from 'url';

// Get the project root directory (2 levels up from this file: backend/src/constants.ts)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
export const PROJECT_ROOT = path.join(__dirname, '../..');

// Database directories
export const DATABASE_DIR = path.join(PROJECT_ROOT, 'database');
export const DECKS_DIR = path.join(DATABASE_DIR, 'decks');
export const AI_SESSIONS_DIR = path.join(DATABASE_DIR, 'ai-sessions');

// Settings file
export const SETTINGS_FILE = path.join(DATABASE_DIR, 'settings.json');
