import fs from 'fs/promises';
import path from 'path';
import type { Note, CachedDeckData, CacheInfo } from '../types/index.js';
import { DECKS_DIR as CACHE_DIR } from '../constants.js';
import { ensureDir } from '../utils/fs.js';
import { estimateDeckTokens } from '../utils/tokenizer.js';

export class CacheService {
  /**
   * Ensure cache directory exists
   */
  async ensureCacheDir(): Promise<void> {
    await ensureDir(CACHE_DIR);
  }

  /**
   * Get parent deck name from a sub-deck
   * e.g., "JP VOC::JP VOC 01 Perso" -> "JP VOC"
   */
  getParentDeck(deckName: string): string | null {
    const lastSeparator = deckName.lastIndexOf('::');
    return lastSeparator > 0 ? deckName.substring(0, lastSeparator) : null;
  }

  /**
   * Get all parent decks in the hierarchy
   * e.g., "A::B::C" -> ["A", "A::B"]
   */
  getAllParentDecks(deckName: string): string[] {
    const parents: string[] = [];
    let current = this.getParentDeck(deckName);
    while (current) {
      parents.push(current);
      current = this.getParentDeck(current);
    }
    return parents;
  }

  /**
   * Check if a deck name matches a pattern (including sub-decks)
   * e.g., isSubDeckOf("JP VOC::Sub", "JP VOC") -> true
   */
  isSubDeckOf(deckName: string, parentDeck: string): boolean {
    return deckName === parentDeck || deckName.startsWith(parentDeck + '::');
  }

  /**
   * Get all cached deck names
   */
  async getAllCachedDecks(): Promise<string[]> {
    try {
      await this.ensureCacheDir();
      const files = await fs.readdir(CACHE_DIR);
      const deckNames: string[] = [];

      for (const file of files) {
        if (file.endsWith('.json')) {
          try {
            const cachePath = path.join(CACHE_DIR, file);
            const data = await fs.readFile(cachePath, 'utf-8');
            const cache = JSON.parse(data) as CachedDeckData;
            if (cache.deckName) {
              deckNames.push(cache.deckName);
            }
          } catch (error) {
            console.error(`Error reading cache file ${file}:`, error);
          }
        }
      }

      return deckNames;
    } catch (error) {
      return [];
    }
  }

  /**
   * Get sanitized filename for a deck
   */
  getSafeFileName(deckName: string): string {
    // Replace special characters with underscores
    return deckName.replace(/[^a-zA-Z0-9-_]/g, '_') + '.json';
  }

  /**
   * Get cache file path for a deck
   */
  getCachePath(deckName: string): string {
    return path.join(CACHE_DIR, this.getSafeFileName(deckName));
  }

  /**
   * Check if cache exists for a deck
   */
  async hasCache(deckName: string): Promise<boolean> {
    try {
      const cachePath = this.getCachePath(deckName);
      await fs.access(cachePath);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get cached notes for a deck (combines sub-deck caches if parent deck)
   */
  async getCachedNotes(deckName: string): Promise<CachedDeckData | null> {
    try {
      // First, try to load cache for this exact deck
      const cachePath = this.getCachePath(deckName);
      try {
        const data = await fs.readFile(cachePath, 'utf-8');
        const cache = JSON.parse(data) as CachedDeckData;
        console.log(`Found cache for exact deck "${deckName}" with ${cache.count} notes`);
        return cache;
      } catch {
        // No exact cache, check if it's a parent deck with sub-deck caches
      }

      // Check if this could be a parent deck by finding sub-deck caches
      const allCachedDecks = await this.getAllCachedDecks();
      const subDeckCaches = allCachedDecks.filter(deck =>
        deck.startsWith(deckName + '::')
      );

      if (subDeckCaches.length === 0) {
        console.log(`No cache found for deck "${deckName}"`);
        return null;
      }

      // Combine all sub-deck caches
      console.log(`Found ${subDeckCaches.length} sub-deck cache(s) for parent deck "${deckName}"`);
      const allNotes: Note[] = [];
      let oldestTimestamp: string | null = null;
      let oldestSyncTimestamp: number | undefined = undefined;
      let totalEstimatedTokens = 0;

      for (const subDeck of subDeckCaches) {
        const subCache = await this.getCachedNotes(subDeck);
        if (subCache) {
          allNotes.push(...subCache.notes);
          if (!oldestTimestamp || subCache.timestamp < oldestTimestamp) {
            oldestTimestamp = subCache.timestamp;
          }
          // Track the oldest sync timestamp to enable incremental sync for parent deck
          if (subCache.lastSyncTimestamp) {
            if (oldestSyncTimestamp === undefined || subCache.lastSyncTimestamp < oldestSyncTimestamp) {
              oldestSyncTimestamp = subCache.lastSyncTimestamp;
            }
          }
          // Sum up token estimates
          if (subCache.estimatedTokens) {
            totalEstimatedTokens += subCache.estimatedTokens;
          }
        }
      }

      if (allNotes.length === 0) {
        return null;
      }

      console.log(`Combined ${allNotes.length} notes from ${subDeckCaches.length} sub-deck(s)`);
      return {
        deckName,
        notes: allNotes,
        timestamp: oldestTimestamp || new Date().toISOString(),
        count: allNotes.length,
        lastSyncTimestamp: oldestSyncTimestamp,
        estimatedTokens: totalEstimatedTokens > 0 ? totalEstimatedTokens : undefined,
      };
    } catch (error) {
      console.log(`Error loading cache for deck "${deckName}":`, error);
      return null;
    }
  }

  /**
   * Save notes to cache (organized by actual sub-decks)
   */
  async cacheNotes(deckName: string, notes: Note[], isIncremental: boolean = false): Promise<void> {
    await this.ensureCacheDir();

    // Group notes by their actual deck (from deckName property)
    const notesByDeck = new Map<string, Note[]>();

    for (const note of notes) {
      const noteDeck = note.deckName || deckName;
      if (!notesByDeck.has(noteDeck)) {
        notesByDeck.set(noteDeck, []);
      }
      notesByDeck.get(noteDeck)!.push(note);
    }

    const timestamp = new Date().toISOString();
    const lastSyncTimestamp = Math.floor(Date.now() / 1000);

    // If notes are from a single deck, cache normally
    if (notesByDeck.size === 1) {
      await this.saveDeckCache(deckName, notes, timestamp, lastSyncTimestamp, isIncremental);
      return;
    }

    // Notes are from multiple sub-decks - cache each separately
    console.log(`Caching notes from ${notesByDeck.size} sub-decks separately...`);
    let totalCached = 0;

    for (const [subDeckName, subDeckNotes] of notesByDeck) {
      await this.saveDeckCache(subDeckName, subDeckNotes, timestamp, lastSyncTimestamp, isIncremental);
      totalCached += subDeckNotes.length;
    }

    console.log(`Total: Cached ${totalCached} notes across ${notesByDeck.size} sub-decks`);
  }

  /**
   * Save or update a deck cache (internal helper)
   */
  private async saveDeckCache(
    deckName: string,
    notes: Note[],
    timestamp: string,
    lastSyncTimestamp: number,
    isIncremental: boolean
  ): Promise<void> {
    const cachePath = this.getCachePath(deckName);

    if (isIncremental) {
      // Merge with existing cache
      const existingCache = await this.getCachedNotes(deckName);
      if (existingCache) {
        // Create a map of existing notes by ID
        const existingNotesMap = new Map(existingCache.notes.map(n => [n.noteId, n]));

        // Update or add new notes
        for (const note of notes) {
          existingNotesMap.set(note.noteId, note);
        }

        const mergedNotes = Array.from(existingNotesMap.values());
        const estimatedTokens = estimateDeckTokens(mergedNotes);
        const cacheData: CachedDeckData = {
          deckName,
          notes: mergedNotes,
          timestamp,
          count: mergedNotes.length,
          lastSyncTimestamp,
          estimatedTokens,
        };

        await fs.writeFile(cachePath, JSON.stringify(cacheData, null, 2), 'utf-8');
        console.log(`  - Updated cache for "${deckName}": ${notes.length} modified, ${mergedNotes.length} total (~${estimatedTokens} tokens)`);
        return;
      }
    }

    // Full cache save
    const estimatedTokens = estimateDeckTokens(notes);
    const cacheData: CachedDeckData = {
      deckName,
      notes,
      timestamp,
      count: notes.length,
      lastSyncTimestamp,
      estimatedTokens,
    };

    await fs.writeFile(cachePath, JSON.stringify(cacheData, null, 2), 'utf-8');
    console.log(`  - Cached ${notes.length} notes for sub-deck "${deckName}" (~${estimatedTokens} tokens)`);
  }

  /**
   * Update a single note in cache
   * Since we cache by actual deck, we just need to update the specific sub-deck cache
   */
  async updateNoteInCache(deckName: string, noteId: number, updatedFields: Record<string, string>): Promise<void> {
    // First, find which specific deck file contains this note
    const noteDeck = await this.findDeckForNote(deckName, noteId);
    if (!noteDeck) {
      console.warn(`Note ${noteId} not found in any cache for deck "${deckName}"`);
      return;
    }

    // Load the specific deck cache
    const cachePath = this.getCachePath(noteDeck);
    try {
      const data = await fs.readFile(cachePath, 'utf-8');
      const cache = JSON.parse(data) as CachedDeckData;

      // Find and update the note
      const noteIndex = cache.notes.findIndex(n => n.noteId === noteId);
      if (noteIndex === -1) {
        console.warn(`Note ${noteId} not found in cache file for deck "${noteDeck}"`);
        return;
      }

      // Update the fields
      for (const [fieldName, newValue] of Object.entries(updatedFields)) {
        if (cache.notes[noteIndex].fields[fieldName]) {
          cache.notes[noteIndex].fields[fieldName].value = newValue;
        }
      }

      // Update modification timestamp
      cache.notes[noteIndex].mod = Math.floor(Date.now() / 1000);

      // Save the entire cache back (with all notes intact)
      await fs.writeFile(cachePath, JSON.stringify(cache, null, 2), 'utf-8');
      console.log(`Updated note ${noteId} in cache for deck "${noteDeck}"`);
    } catch (error) {
      console.error(`Error updating note ${noteId} in cache:`, error);
    }
  }

  /**
   * Find which deck file contains a specific note
   */
  private async findDeckForNote(deckName: string, noteId: number): Promise<string | null> {
    // Check exact deck first
    const exactCache = this.getCachePath(deckName);
    try {
      const data = await fs.readFile(exactCache, 'utf-8');
      const cache = JSON.parse(data) as CachedDeckData;
      if (cache.notes.some(n => n.noteId === noteId)) {
        return deckName;
      }
    } catch {
      // Not in exact deck, check subdecks
    }

    // Check all subdeck caches
    const allCachedDecks = await this.getAllCachedDecks();
    const subDecks = allCachedDecks.filter(deck => deck.startsWith(deckName + '::'));

    for (const subDeck of subDecks) {
      const cachePath = this.getCachePath(subDeck);
      try {
        const data = await fs.readFile(cachePath, 'utf-8');
        const cache = JSON.parse(data) as CachedDeckData;
        if (cache.notes.some(n => n.noteId === noteId)) {
          return subDeck;
        }
      } catch {
        continue;
      }
    }

    return null;
  }

  /**
   * Delete cache for a deck
   */
  async deleteCache(deckName: string): Promise<void> {
    try {
      const cachePath = this.getCachePath(deckName);
      await fs.unlink(cachePath);
      console.log(`Deleted cache for deck "${deckName}"`);
    } catch (error) {
      console.log(`No cache to delete for deck "${deckName}"`);
    }
  }

  /**
   * Invalidate (delete) cache for a deck and all sub-decks
   */
  async invalidateRelatedCaches(deckName: string): Promise<void> {
    const allCachedDecks = await this.getAllCachedDecks();
    const decksToInvalidate: string[] = [];

    for (const cachedDeck of allCachedDecks) {
      // Invalidate if it's the deck itself or a sub-deck
      if (cachedDeck === deckName || this.isSubDeckOf(cachedDeck, deckName)) {
        decksToInvalidate.push(cachedDeck);
      }
    }

    // Delete all related caches
    for (const deck of decksToInvalidate) {
      await this.deleteCache(deck);
    }

    if (decksToInvalidate.length > 0) {
      console.log(`Invalidated ${decksToInvalidate.length} cache(s): ${decksToInvalidate.join(', ')}`);
    }
  }

  /**
   * Get cache info for a deck
   */
  async getCacheInfo(deckName: string): Promise<CacheInfo> {
    const cache = await this.getCachedNotes(deckName);
    if (!cache) {
      return { exists: false };
    }

    return {
      exists: true,
      timestamp: cache.timestamp,
      count: cache.count,
      deckName: cache.deckName,
      estimatedTokens: cache.estimatedTokens,
    };
  }

  /**
   * Sync deck cache with Anki (incremental if cache exists, full if not)
   * @param deckName - Name of the deck to sync
   * @returns Object with sync results { notesUpdated: number, fromCache: boolean }
   */
  async syncDeckCache(deckName: string): Promise<{ notesUpdated: number; fromCache: boolean }> {
    const { ankiConnectService } = await import('./AnkiConnectService.js');

    // Check if cache exists
    const cachedData = await this.getCachedNotes(deckName);

    if (cachedData && cachedData.lastSyncTimestamp) {
      // Incremental sync - only fetch modified notes
      console.log(`  Cache exists (last sync: ${new Date(cachedData.lastSyncTimestamp * 1000).toISOString()})`);
      const modifiedNotes = await ankiConnectService.getDeckNotes(deckName, cachedData.lastSyncTimestamp);

      if (modifiedNotes.length > 0) {
        await this.cacheNotes(deckName, modifiedNotes, true);
        console.log(`  Sync complete: ${modifiedNotes.length} notes updated`);
      } else {
        console.log(`  Sync complete: no modifications since last sync`);
      }

      return { notesUpdated: modifiedNotes.length, fromCache: true };
    } else {
      // No cache - full sync
      console.log(`  No cache found, performing full sync...`);
      const notes = await ankiConnectService.getDeckNotes(deckName);
      await this.cacheNotes(deckName, notes, false);
      console.log(`  Full sync complete: ${notes.length} notes cached`);

      return { notesUpdated: notes.length, fromCache: false };
    }
  }
}

// Singleton instance
export const cacheService = new CacheService();
