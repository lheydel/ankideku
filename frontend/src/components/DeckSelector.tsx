import { useEffect, useState } from 'react';
import { ankiApi } from '../services/api.js';
import useStore from '../store/useStore.js';
import { SpinnerIcon, WarningIcon, RefreshIcon, DeckIcon, ChevronDownIcon, CheckIcon } from './ui/Icons.js';

export default function DeckSelector() {
  const { decks, selectedDeck, setDecks, selectDeck, setAnkiConnected } = useStore();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    checkConnection();
  }, []);

  const checkConnection = async () => {
    try {
      setLoading(true);
      setError(null);

      // Check AnkiConnect connection
      const { connected } = await ankiApi.ping();
      setAnkiConnected(connected);

      if (!connected) {
        setError('AnkiConnect is not running. Please start Anki.');
        setLoading(false);
        return;
      }

      // Fetch decks
      const deckData = await ankiApi.getDecks();
      setDecks(deckData);
      setLoading(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to connect to AnkiConnect';
      setError(errorMessage);
      setAnkiConnected(false);
      setLoading(false);
    }
  };

  const deckEntries = Object.entries(decks);

  if (loading) {
    return (
      <div className="p-6 bg-gradient-to-r from-primary-50 to-blue-50 rounded-xl border border-primary-100">
        <div className="flex items-center gap-3">
          <SpinnerIcon className="w-5 h-5 text-primary-600" />
          <span className="text-primary-900 font-medium">Loading decks...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 bg-red-50 border border-red-200 rounded-xl">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-start gap-3 flex-1">
            <WarningIcon className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-red-900 font-semibold">Connection Error</p>
              <p className="text-red-700 text-sm mt-1">{error}</p>
            </div>
          </div>
          <button
            onClick={checkConnection}
            className="btn btn-danger flex-shrink-0"
          >
            <RefreshIcon className="w-4 h-4 inline mr-2" />
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <label className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <DeckIcon className="w-4 h-4 text-primary-600" />
          Select Deck
        </label>
        <span className="text-xs text-gray-500 bg-gray-100 px-2.5 py-1 rounded-full font-medium">
          {deckEntries.length} available
        </span>
      </div>

      <div className="relative">
        <select
          value={selectedDeck || ''}
          onChange={(e) => selectDeck(e.target.value)}
          className="input appearance-none pr-10 cursor-pointer"
        >
          <option value="">Choose a deck...</option>
          {deckEntries.map(([name, id]) => (
            <option key={id} value={name}>
              {name}
            </option>
          ))}
        </select>
        <ChevronDownIcon className="w-5 h-5 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
      </div>

      {selectedDeck && (
        <div className="p-4 bg-gradient-to-r from-primary-50 to-blue-50 border border-primary-100 rounded-xl fade-in">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <CheckIcon className="w-4 h-4 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-medium text-primary-600 mb-0.5">Selected Deck</p>
              <p className="text-sm font-semibold text-primary-900 truncate">{selectedDeck}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
