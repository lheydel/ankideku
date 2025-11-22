import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DATABASE_DIR = path.join(__dirname, '../../../database');
const SETTINGS_FILE = path.join(DATABASE_DIR, 'settings.json');

export interface FieldDisplayConfig {
  [modelName: string]: string; // Maps model name to field name to display
}

export interface UserSettings {
  fieldDisplayConfig: FieldDisplayConfig;
}

const DEFAULT_SETTINGS: UserSettings = {
  fieldDisplayConfig: {},
};

class SettingsService {
  /**
   * Ensure database directory exists
   */
  async ensureDatabaseDir(): Promise<void> {
    try {
      await fs.mkdir(DATABASE_DIR, { recursive: true });
    } catch (error) {
      console.error('Error creating database directory:', error);
    }
  }

  /**
   * Load settings from file
   */
  async loadSettings(): Promise<UserSettings> {
    try {
      await this.ensureDatabaseDir();
      const data = await fs.readFile(SETTINGS_FILE, 'utf-8');
      return JSON.parse(data);
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
}

export default new SettingsService();
