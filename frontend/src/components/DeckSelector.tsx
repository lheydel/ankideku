import { useEffect, useState } from 'react';
import { ankiApi } from '../services/api.js';
import useStore from '../store/useStore.js';

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
      <div className="p-4 bg-gray-100 rounded-lg">
        <div className="animate-pulse">Loading decks...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-red-800 font-medium">Connection Error</p>
            <p className="text-red-600 text-sm mt-1">{error}</p>
          </div>
          <button
            onClick={checkConnection}
            className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">Select Deck</h2>
        <span className="text-sm text-gray-500">{deckEntries.length} decks available</span>
      </div>

      <select
        value={selectedDeck || ''}
        onChange={(e) => selectDeck(e.target.value)}
        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
      >
        <option value="">Choose a deck...</option>
        {deckEntries.map(([name, id]) => (
          <option key={id} value={name}>
            {name}
          </option>
        ))}
      </select>

      {selectedDeck && (
        <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
          <p className="text-sm text-blue-800">
            <span className="font-medium">Selected:</span> {selectedDeck}
          </p>
        </div>
      )}
    </div>
  );
}
