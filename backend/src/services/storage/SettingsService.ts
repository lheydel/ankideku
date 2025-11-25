import fs from 'fs/promises';
import { DATABASE_DIR, SETTINGS_FILE } from '../../constants.js';
import type { LLMConfig } from '../llm/LLMService.js';
import { DEFAULT_LLM_CONFIG } from '../llm/LLMService.js';
import { ensureDir } from '../../utils/fs.js';

export interface FieldDisplayConfig {
  [modelName: string]: string; // Maps model name to field name to display
}

export interface UserSettings {
  fieldDisplayConfig: FieldDisplayConfig;
  llm: LLMConfig;
}

const DEFAULT_SETTINGS: UserSettings = {
  fieldDisplayConfig: {},
  llm: DEFAULT_LLM_CONFIG,
};

export class SettingsService {
  /**
   * Ensure database directory exists
   */
  async ensureDatabaseDir(): Promise<void> {
    await ensureDir(DATABASE_DIR);
  }

  /**
   * Load settings from file
   * Merges with defaults to handle missing keys from older settings files
   */
  async loadSettings(): Promise<UserSettings> {
    try {
      await this.ensureDatabaseDir();
      const data = await fs.readFile(SETTINGS_FILE, 'utf-8');
      const parsed = JSON.parse(data);
      // Merge with defaults to ensure all keys exist (handles migration)
      return {
        ...DEFAULT_SETTINGS,
        ...parsed,
        llm: { ...DEFAULT_LLM_CONFIG, ...parsed.llm },
      };
    } catch (error) {
      // Only return defaults if file doesn't exist
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return { ...DEFAULT_SETTINGS };
      }
      // For other errors (corrupted JSON, permission issues, etc.), throw
      console.error('Failed to load settings:', error);
      throw new Error('Settings file is corrupted or inaccessible');
    }
  }

  /**
   * Save settings to file
   */
  async saveSettings(settings: UserSettings): Promise<void> {
    try {
      await this.ensureDatabaseDir();
      await fs.writeFile(SETTINGS_FILE, JSON.stringify(settings, null, 2), 'utf-8');
      console.log('Settings saved successfully');
    } catch (error) {
      console.error('Error saving settings:', error);
      throw error;
    }
  }

  /**
   * Get field display configuration
   */
  async getFieldDisplayConfig(): Promise<FieldDisplayConfig> {
    const settings = await this.loadSettings();
    return settings.fieldDisplayConfig;
  }

  /**
   * Update field display configuration
   */
  async updateFieldDisplayConfig(config: FieldDisplayConfig): Promise<void> {
    const settings = await this.loadSettings();
    settings.fieldDisplayConfig = config;
    await this.saveSettings(settings);
  }

  /**
   * Update single model's display field
   */
  async updateModelDisplayField(modelName: string, fieldName: string): Promise<void> {
    const settings = await this.loadSettings();
    settings.fieldDisplayConfig[modelName] = fieldName;
    await this.saveSettings(settings);
  }

  /**
   * Get LLM configuration
   */
  async getLLMConfig(): Promise<LLMConfig> {
    const settings = await this.loadSettings();
    // Merge with defaults to ensure all fields exist
    return { ...DEFAULT_LLM_CONFIG, ...settings.llm };
  }

  /**
   * Update LLM configuration
   */
  async updateLLMConfig(config: Partial<LLMConfig>): Promise<void> {
    const settings = await this.loadSettings();
    settings.llm = { ...settings.llm, ...config };
    await this.saveSettings(settings);
  }
}

// Singleton instance
export const settingsService = new SettingsService();
