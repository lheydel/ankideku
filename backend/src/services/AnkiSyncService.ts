/**
 * Anki Sync Service
 * Handles deck synchronization with progress tracking and batch retry logic
 *
 * Optimization: Queries each sub-deck separately to avoid slow cardsInfo enrichment
 */

import type { Note } from '../types/index.js';
import type { SyncProgressPayload } from '../../../contract/types.js';
import { CONFIG } from '../config.js';
import { AnkiConnectService } from './AnkiConnectService.js';
import { cacheService } from './CacheService.js';
import { syncProgressEmitter } from './SyncProgressEmitter.js';

export interface SyncResult {
  notesUpdated: number;
  fromCache: boolean;
}

/**
 * Service for syncing Anki decks with progress reporting
 */
export class AnkiSyncService {
  private ankiConnect: AnkiConnectService;

  constructor(ankiConnect: AnkiConnectService) {
    this.ankiConnect = ankiConnect;
  }

  /**
   * Sync a deck from Anki with real-time progress updates
   * Queries each sub-deck separately for better performance (avoids cardsInfo)
   */
  async syncDeck(deckName: string): Promise<SyncResult> {
    // Get all deck names to find sub-decks
    const allDeckNames = await this.ankiConnect.getDeckNames();

    // Find all decks to sync: the selected deck + all sub-decks
    const decksToSync = allDeckNames.filter(
      name => name === deckName || name.startsWith(deckName + '::')
    );

    console.log(`[AnkiSyncService] Found ${decksToSync.length} deck(s) to sync: ${decksToSync.join(', ')}`);

    if (decksToSync.length === 0) {
      return { notesUpdated: 0, fromCache: false };
    }

    // Check if we have cached data for incremental sync
    const cachedData = await cacheService.getCachedNotes(deckName);
    const isIncremental = !!(cachedData?.lastSyncTimestamp);
    const lastSyncTimestamp = cachedData?.lastSyncTimestamp;

    let totalNotesUpdated = 0;
    const totalSteps = decksToSync.length;

    // Sync each deck separately
    for (let i = 0; i < decksToSync.length; i++) {
      const currentDeck = decksToSync[i];
      const step = i + 1;

      const notesUpdated = await this.syncSingleDeck(
        currentDeck,
        deckName, // Root deck name for progress emission
        step,
        totalSteps,
        isIncremental,
        lastSyncTimestamp
      );

      totalNotesUpdated += notesUpdated;

      // Delay between decks to avoid overwhelming AnkiConnect
      if (i < decksToSync.length - 1) {
        await this.sleep(CONFIG.anki.batchDelay);
      }
    }

    console.log(`[AnkiSyncService] Sync complete: ${totalNotesUpdated} notes updated across ${decksToSync.length} deck(s)`);
    return { notesUpdated: totalNotesUpdated, fromCache: isIncremental };
  }

  /**
   * Sync a single deck (no sub-decks)
   */
  private async syncSingleDeck(
    deckName: string,
    rootDeckName: string,
    step: number,
    totalSteps: number,
    isIncremental: boolean,
    lastSyncTimestamp?: number
  ): Promise<number> {
    // Build query for this exact deck only (exclude sub-decks)
    let query = `deck:"${deckName}" -deck:"${deckName}::*"`;

    if (isIncremental && lastSyncTimestamp) {
      const daysSinceSync = Math.ceil((Date.now() / 1000 - lastSyncTimestamp) / 86400);
      query += ` edited:${Math.max(1, daysSinceSync)}`;
    }

    console.log(`[AnkiSyncService] Step ${step}/${totalSteps}: ${deckName} (${isIncremental ? 'incremental' : 'full'})`);

    // Find note IDs for this deck
    const noteIds = await this.ankiConnect.findNotes(query);

    if (noteIds.length === 0) {
      // Emit progress even for empty decks
      this.emitProgress(rootDeckName, {
        step,
        totalSteps,
        stepName: this.getShortDeckName(deckName, rootDeckName),
        processed: 1,
        total: 1,
      });
      return 0;
    }

    // Fetch notes in batches with progress
    const notes = await this.fetchNotesForDeck(
      deckName,
      rootDeckName,
      noteIds,
      step,
      totalSteps
    );

    // Assign deck name to all notes (we know it from the query)
    for (const note of notes) {
      note.deckName = deckName;
    }

    // Cache results for this specific deck
    await cacheService.cacheNotes(deckName, notes, isIncremental);

    return notes.length;
  }

  /**
   * Fetch notes for a single deck with progress reporting
   */
  private async fetchNotesForDeck(
    deckName: string,
    rootDeckName: string,
    noteIds: number[],
    step: number,
    totalSteps: number
  ): Promise<Note[]> {
    const batchSize = CONFIG.anki.batchSize;
    const totalBatches = Math.ceil(noteIds.length / batchSize);
    const notes: Note[] = [];
    const shortName = this.getShortDeckName(deckName, rootDeckName);

    for (let i = 0; i < noteIds.length; i += batchSize) {
      const batch = noteIds.slice(i, i + batchSize);
      const batchNumber = Math.floor(i / batchSize) + 1;

      this.emitProgress(rootDeckName, {
        step,
        totalSteps,
        stepName: shortName,
        processed: batchNumber,
        total: totalBatches,
      });

      const batchNotes = await this.fetchBatchWithRetry(
        batch,
        (ids) => this.ankiConnect.notesInfo(ids),
        'notes'
      );
      notes.push(...batchNotes);

      // Delay between batches
      if (i + batchSize < noteIds.length) {
        await this.sleep(CONFIG.anki.batchDelay);
      }
    }

    return notes;
  }

  /**
   * Get a shortened deck name for display
   * If it's a sub-deck, show only the last part
   */
  private getShortDeckName(deckName: string, rootDeckName: string): string {
    if (deckName === rootDeckName) {
      return deckName;
    }
    // For sub-decks, show path relative to root
    if (deckName.startsWith(rootDeckName + '::')) {
      return deckName.substring(rootDeckName.length + 2);
    }
    return deckName;
  }

  /**
   * Fetch a batch with retry logic - splits batch in half on failure
   */
  private async fetchBatchWithRetry<TItem, TResult>(
    batch: TItem[],
    fetchFn: (batch: TItem[]) => Promise<TResult[]>,
    itemName: string,
    depth: number = 0
  ): Promise<TResult[]> {
    try {
      return await fetchFn(batch);
    } catch (error) {
      // Can't split further - single item failed
      if (batch.length === 1) {
        console.error(`${'  '.repeat(depth)}Failed to fetch single ${itemName}: ${error instanceof Error ? error.message : error}`);
        return [];
      }

      // Split batch in half and retry recursively
      const halfSize = Math.ceil(batch.length / 2);
      const firstHalf = batch.slice(0, halfSize);
      const secondHalf = batch.slice(halfSize);

      console.log(`${'  '.repeat(depth)}Splitting failed batch of ${batch.length} into ${firstHalf.length} + ${secondHalf.length}`);

      const results: TResult[] = [];

      const firstResults = await this.fetchBatchWithRetry(firstHalf, fetchFn, itemName, depth + 1);
      results.push(...firstResults);

      await this.sleep(CONFIG.anki.batchDelay);

      const secondResults = await this.fetchBatchWithRetry(secondHalf, fetchFn, itemName, depth + 1);
      results.push(...secondResults);

      return results;
    }
  }

  /**
   * Sleep for a specified number of milliseconds
   */
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Emit sync progress event
   */
  private emitProgress(
    deckName: string,
    progress: Omit<SyncProgressPayload, 'deckName'>
  ): void {
    syncProgressEmitter.emitProgress(deckName, {
      deckName,
      ...progress,
    });
  }
}

// Singleton instance - import ankiConnectService lazily to avoid circular deps
let instance: AnkiSyncService | null = null;

export async function getAnkiSyncService(): Promise<AnkiSyncService> {
  if (!instance) {
    const { ankiConnectService } = await import('./AnkiConnectService.js');
    instance = new AnkiSyncService(ankiConnectService);
  }
  return instance;
}
