import { useEffect, useState, useCallback } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';
import { useSyncProgress } from './useSyncProgress';
import type { CacheInfo } from '../types';

/**
 * Hook for managing Anki connection, deck loading, and syncing
 */
export function useAnkiConnection(onMessage?: (type: 'system' | 'assistant', content: string) => void) {
  const { setDecks, setAnkiConnected, setFieldDisplayConfig, selectedDeck } = useStore();
  const [syncing, setSyncing] = useState(false);
  const [cacheInfo, setCacheInfo] = useState<CacheInfo | null>(null);

  // Subscribe to sync progress for the selected deck
  const { progress: syncProgress, clearProgress } = useSyncProgress(syncing ? selectedDeck : null);

  const loadSettings = useCallback(async () => {
    try {
      const settings = await ankiApi.getSettings();
      if (settings.fieldDisplayConfig) {
        setFieldDisplayConfig(settings.fieldDisplayConfig);
      }
    } catch (error) {
      console.error('Failed to load settings:', error);
    }
  }, [setFieldDisplayConfig]);

  const loadDecks = useCallback(async () => {
    try {
      const { connected } = await ankiApi.ping();
      setAnkiConnected(connected);

      if (!connected) {
        onMessage?.('system', 'AnkiConnect is not running. Please start Anki.');
        return;
      }

      const deckData = await ankiApi.getDecks();
      setDecks(deckData);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to connect to AnkiConnect';
      onMessage?.('system', errorMessage);
      setAnkiConnected(false);
    }
  }, [setDecks, setAnkiConnected, onMessage]);

  const loadCacheInfo = useCallback(async (deckName: string) => {
    try {
      const info = await ankiApi.getCacheInfo(deckName);
      setCacheInfo(info);
    } catch (error) {
      console.error('Failed to load cache info:', error);
      setCacheInfo(null);
    }
  }, []);

  const syncDeck = useCallback(async () => {
    if (!selectedDeck) {
      onMessage?.('system', 'Please select a deck first');
      return;
    }

    try {
      setSyncing(true);
      onMessage?.('system', `Syncing "${selectedDeck}"...`);
      const result = await ankiApi.syncDeck(selectedDeck);
      onMessage?.('assistant', `✓ Synced ${result.count} card${result.count !== 1 ? 's' : ''}`);
      // Refresh cache info after sync
      await loadCacheInfo(selectedDeck);
    } catch (error) {
      onMessage?.('system', 'Sync failed');
      console.error('Sync error:', error);
    } finally {
      setSyncing(false);
      clearProgress();
    }
  }, [selectedDeck, onMessage, loadCacheInfo, clearProgress]);

  // Load decks and settings on mount
  useEffect(() => {
    loadDecks();
    loadSettings();
  }, [loadDecks, loadSettings]);

  // Load cache info when deck changes
  useEffect(() => {
    if (selectedDeck) {
      loadCacheInfo(selectedDeck);
    } else {
      setCacheInfo(null);
    }
  }, [selectedDeck, loadCacheInfo]);

  return {
    syncing,
    syncDeck,
    loadDecks,
    cacheInfo,
    syncProgress,
  };
}
