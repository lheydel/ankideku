import fs from 'fs/promises';

/**
 * Ensure a directory exists, creating it recursively if necessary
 * @param dirPath - Path to the directory
 */
export async function ensureDir(dirPath: string): Promise<void> {
  try {
    await fs.mkdir(dirPath, { recursive: true });
  } catch (error) {
    console.error(`Error creating directory ${dirPath}:`, error);
  }
}

/**
 * Read JSON file with type safety
 * @param filePath - Path to the JSON file
 * @returns Parsed JSON or null if file doesn't exist
 */
export async function readJsonFile<T>(filePath: string): Promise<T | null> {
  try {
    const data = await fs.readFile(filePath, 'utf-8');
    return JSON.parse(data) as T;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      return null;
    }
    throw error;
  }
}

/**
 * Write JSON file with pretty formatting
 * @param filePath - Path to the JSON file
 * @param data - Data to write
 */
export async function writeJsonFile<T>(filePath: string, data: T): Promise<void> {
  await fs.writeFile(filePath, JSON.stringify(data, null, 2), 'utf-8');
}
