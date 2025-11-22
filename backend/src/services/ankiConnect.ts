import axios, { AxiosError } from 'axios';
import type { Note, DeckInfo, NoteUpdate, AnkiConnectResponse } from '../types/index.js';

const ANKI_CONNECT_URL = 'http://localhost:8765';
const ANKI_CONNECT_VERSION = 6;
const REQUEST_TIMEOUT = 60000; // 60 seconds for large requests
const BATCH_SIZE = 50; // Batch size for notes
const BATCH_DELAY = 50; // 50ms delay between batches

class AnkiConnectService {
  /**
   * Generic batch fetching with retry logic
   */
  private async fetchInBatches<TItem, TResult>(
    items: TItem[],
    fetchFn: (batch: TItem[]) => Promise<TResult[]>,
    options: {
      batchSize: number;
      itemName: string;
      delay?: number;
    }
  ): Promise<TResult[]> {
    const { batchSize, itemName, delay = BATCH_DELAY } = options;
    const results: TResult[] = [];
    const totalBatches = Math.ceil(items.length / batchSize);

    for (let i = 0; i < items.length; i += batchSize) {
      const batch = items.slice(i, i + batchSize);
      const batchNumber = Math.floor(i / batchSize) + 1;

      console.log(`Fetching batch ${batchNumber}/${totalBatches} (${batch.length} ${itemName})`);

      const batchResults = await this.fetchBatchWithRetry(batch, fetchFn, itemName, delay);
      results.push(...batchResults);

      if (i + batchSize < items.length) {
        await this.sleep(delay);
      }
    }

    return results;
  }

  /**
   * Recursively fetch a batch, splitting in half on failure
   */
  private async fetchBatchWithRetry<TItem, TResult>(
    batch: TItem[],
    fetchFn: (batch: TItem[]) => Promise<TResult[]>,
    itemName: string,
    delay: number,
    depth: number = 0
  ): Promise<TResult[]> {
    try {
      return await fetchFn(batch);
    } catch (error) {
      // Can't split further - single item failed
      if (batch.length === 1) {
        console.error(`  ${'  '.repeat(depth)}Failed to fetch single ${itemName}: ${error instanceof Error ? error.message : error}`);
        return [];
      }

      // Split batch in half and retry recursively
      const halfSize = Math.ceil(batch.length / 2);
      const firstHalf = batch.slice(0, halfSize);
      const secondHalf = batch.slice(halfSize);

      console.log(`  ${'  '.repeat(depth)}Splitting failed batch of ${batch.length} into ${firstHalf.length} + ${secondHalf.length}`);

      const results: TResult[] = [];

      // Fetch first half
      const firstResults = await this.fetchBatchWithRetry(firstHalf, fetchFn, itemName, delay, depth + 1);
      results.push(...firstResults);

      await this.sleep(delay);

      // Fetch second half
      const secondResults = await this.fetchBatchWithRetry(secondHalf, fetchFn, itemName, delay, depth + 1);
      results.push(...secondResults);

      return results;
    }
  }

  /**
   * Make a request to AnkiConnect
   */
  async request<T = any>(action: string, params: Record<string, any> = {}, timeout: number = REQUEST_TIMEOUT): Promise<T> {
    try {
      const response = await axios.post<AnkiConnectResponse<T>>(ANKI_CONNECT_URL, {
        action,
        version: ANKI_CONNECT_VERSION,
        params
      }, {
        timeout
      });

      if (response.data.error) {
        throw new Error(`AnkiConnect error: ${response.data.error}`);
      }

      return response.data.result;
    } catch (error) {
      const axiosError = error as AxiosError;
      if (axiosError.code === 'ECONNREFUSED') {
        throw new Error('AnkiConnect is not running. Please start Anki with the AnkiConnect addon installed.');
      }
      if (axiosError.code === 'ECONNRESET') {
        throw new Error('Connection reset by AnkiConnect. The request may be too large.');
      }
      if (axiosError.code === 'ETIMEDOUT') {
        throw new Error('Request timed out. Try reducing the number of cards.');
      }
      throw error;
    }
  }

  /**
   * Sleep for a specified number of milliseconds
   */
  private async sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Check if AnkiConnect is accessible
   */
  async ping(): Promise<boolean> {
    try {
      const version = await this.request<number>('version');
      return version === ANKI_CONNECT_VERSION;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get all deck names
   */
  async getDeckNames(): Promise<string[]> {
    return await this.request<string[]>('deckNames');
  }

  /**
   * Get deck names with IDs
   */
  async getDeckNamesAndIds(): Promise<DeckInfo> {
    return await this.request<DeckInfo>('deckNamesAndIds');
  }

  /**
   * Find notes matching a query
   */
  async findNotes(query: string): Promise<number[]> {
    return await this.request<number[]>('findNotes', { query });
  }

  /**
   * Get detailed information about notes
   */
  async notesInfo(noteIds: number[]): Promise<Note[]> {
    return await this.request<Note[]>('notesInfo', { notes: noteIds });
  }

  /**
   * Get detailed information about cards (includes deck names)
   */
  async cardsInfo(cardIds: number[]): Promise<Array<{ cardId: number; deckName: string }>> {
    return await this.request<Array<{ cardId: number; deckName: string }>>('cardsInfo', { cards: cardIds });
  }

  /**
   * Update fields of a note
   */
  async updateNoteFields(noteId: number, fields: Record<string, string>): Promise<null> {
    return await this.request<null>('updateNoteFields', {
      note: {
        id: noteId,
        fields
      }
    });
  }

  /**
   * Get all notes from a deck (with optional incremental sync)
   */
  async getDeckNotes(deckName: string, lastSyncTimestamp?: number): Promise<Note[]> {
    let query = `deck:"${deckName}"`;

    // If we have a last sync timestamp, only fetch modified notes
    if (lastSyncTimestamp) {
      const daysSinceSync = Math.ceil((Date.now() / 1000 - lastSyncTimestamp) / 86400);
      query += ` edited:${Math.max(1, daysSinceSync)}`;
      console.log(`AnkiConnect incremental query: ${query} (last sync: ${new Date(lastSyncTimestamp * 1000).toISOString()})`);
    } else {
      console.log(`AnkiConnect query: ${query}`);
    }

    // First, get all note IDs
    const noteIds = await this.findNotes(query);
    console.log(`Found ${noteIds.length} note IDs ${lastSyncTimestamp ? '(modified since last sync)' : ''}`);

    if (noteIds.length === 0) {
      return [];
    }

    // Fetch notes in batches
    const allNotes = await this.fetchInBatches(
      noteIds,
      (batch) => this.notesInfo(batch),
      {
        batchSize: BATCH_SIZE,
        itemName: 'notes'
      }
    );

    console.log(`Successfully fetched ${allNotes.length} notes out of ${noteIds.length} total`);

    // Enrich notes with deck information from their cards
    await this.enrichNotesWithDeckInfo(allNotes);

    return allNotes;
  }

  /**
   * Enrich notes with deck information by querying card info
   */
  private async enrichNotesWithDeckInfo(notes: Note[]): Promise<void> {
    // Collect all unique card IDs
    const allCardIds = new Set<number>();
    for (const note of notes) {
      for (const cardId of note.cards) {
        allCardIds.add(cardId);
      }
    }

    if (allCardIds.size === 0) {
      return;
    }

    console.log(`Fetching deck info for ${allCardIds.size} cards...`);

    // Fetch card info in batches (smaller than notesInfo due to more data)
    const cardIdArray = Array.from(allCardIds);
    const cardsInfoResults = await this.fetchInBatches(
      cardIdArray,
      (batch) => this.cardsInfo(batch),
      {
        batchSize: 20,
        itemName: 'cards'
      }
    );

    // Build card ID to deck name map
    const cardInfoMap = new Map<number, string>();
    for (const cardInfo of cardsInfoResults) {
      cardInfoMap.set(cardInfo.cardId, cardInfo.deckName);
    }

    // Assign deck name to each note (use first card's deck)
    for (const note of notes) {
      if (note.cards.length > 0) {
        const deckName = cardInfoMap.get(note.cards[0]);
        if (deckName) {
          note.deckName = deckName;
        }
      }
    }

    console.log(`Enriched ${notes.length} notes with deck information (${cardInfoMap.size}/${allCardIds.size} cards successful)`);
  }

  /**
   * Batch update multiple notes
   */
  async batchUpdateNotes(updates: NoteUpdate[]): Promise<any[]> {
    const actions = updates.map(({ noteId, fields }) => ({
      action: 'updateNoteFields',
      params: {
        note: {
          id: noteId,
          fields
        }
      }
    }));

    return await this.request<any[]>('multi', { actions });
  }
}

export default new AnkiConnectService();
