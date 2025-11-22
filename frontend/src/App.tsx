import { useState } from 'react';
import DeckSelector from './components/DeckSelector.js';
import PromptInput from './components/PromptInput.js';
import Queue from './components/Queue.js';
import ComparisonView from './components/ComparisonView.js';
import useStore from './store/useStore.js';
import { ankiApi } from './services/api.js';
import type { NotificationState, Note, CardSuggestion } from './types/index.js';

function App() {
  const {
    selectedDeck,
    prompt,
    setQueue,
    setProcessing,
    setProgress,
    addToPromptHistory,
    queue,
    getCurrentCard,
    nextCard,
    skipCard,
    currentIndex,
    addToHistory,
  } = useStore();

  const [error, setError] = useState<string | null>(null);
  const [notification, setNotification] = useState<NotificationState | null>(null);

  const handleGenerate = async () => {
    if (!selectedDeck || !prompt.trim()) return;

    try {
      setError(null);
      setProcessing(true);
      setProgress(0, 0);

      // Add to prompt history
      addToPromptHistory(prompt);

      // Fetch all notes from the deck (cache-first)
      console.log('Fetching notes from deck:', selectedDeck);
      const response = await ankiApi.getDeckNotes(selectedDeck);
      const notes = response.notes || (response as any); // Handle both old and new format
      console.log(`Fetched ${notes.length} notes ${response.fromCache ? '(from cache at ' + response.cachedAt + ')' : '(from Anki)'}`);

      if (response.fromCache) {
        showNotification(`Loaded ${notes.length} cards from cache`, 'info');
      }

      setProgress(notes.length, notes.length);

      // For now, create mock suggestions (we'll integrate real AI later)
      // This simulates finding issues in some cards
      const mockSuggestions: CardSuggestion[] = notes.slice(0, Math.min(10, notes.length)).map((note: Note) => {
        // Create a mock change for the first field
        const firstFieldName = Object.keys(note.fields)[0];
        const originalValue = note.fields[firstFieldName].value;

        return {
          noteId: note.noteId,
          original: note,
          changes: {
            [firstFieldName]: originalValue + ' [AI: suggestion placeholder]',
          },
          reasoning: 'This is a placeholder. AI integration coming soon!',
        };
      });

      setQueue(mockSuggestions);
      setProcessing(false);

      console.log(`Generated ${mockSuggestions.length} suggestions`);
    } catch (err) {
      console.error('Error generating suggestions:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to generate suggestions';
      setError(errorMessage);
      setProcessing(false);
    }
  };

  const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleAccept = async () => {
    const card = getCurrentCard();
    if (!card) return;

    try {
      // Update note in Anki and cache
      await ankiApi.updateNote(card.noteId, card.changes, selectedDeck);

      // Log the action
      addToHistory({
        action: 'accept',
        noteId: card.noteId,
        changes: card.changes,
        original: card.original,
      });

      showNotification('Changes accepted and applied', 'success');

      // Move to next card or finish
      if (currentIndex < queue.length - 1) {
        nextCard();
      } else {
        showNotification('All suggestions reviewed!', 'success');
        setQueue([]);
      }
    } catch (err) {
      console.error('Error accepting changes:', err);
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      showNotification('Failed to apply changes: ' + errorMessage, 'error');
    }
  };

  const handleReject = () => {
    const card = getCurrentCard();
    if (!card) return;

    // Log the rejection
    addToHistory({
      action: 'reject',
      noteId: card.noteId,
      changes: card.changes,
    });

    showNotification('Suggestion rejected', 'info');

    // Move to next card or finish
    if (currentIndex < queue.length - 1) {
      nextCard();
    } else {
      showNotification('All suggestions reviewed!', 'success');
      setQueue([]);
    }
  };

  const handleSkip = () => {
    skipCard();
    showNotification('Card skipped - moved to end of queue', 'info');
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Notification Toast */}
      {notification && (
        <div className="fixed top-4 right-4 z-50 animate-slide-in">
          <div
            className={`px-6 py-3 rounded-lg shadow-lg ${
              notification.type === 'success'
                ? 'bg-green-500 text-white'
                : notification.type === 'error'
                ? 'bg-red-500 text-white'
                : 'bg-blue-500 text-white'
            }`}
          >
            {notification.message}
          </div>
        </div>
      )}

      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-6 py-4">
        <h1 className="text-2xl font-bold text-gray-900">AnkiDeku</h1>
        <p className="text-sm text-gray-600">AI-Powered Deck Revision</p>
      </header>

      <div className="flex h-[calc(100vh-73px)]">
        {/* Sidebar - Queue */}
        {queue.length > 0 && <Queue />}

        {/* Main Content */}
        <div className="flex-1 overflow-y-auto">
          {queue.length === 0 ? (
            // Setup view
            <div className="max-w-2xl mx-auto p-8 space-y-6">
              <DeckSelector />
              <PromptInput onGenerate={handleGenerate} />

              {error && (
                <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-red-800 font-medium">Error</p>
                  <p className="text-red-600 text-sm mt-1">{error}</p>
                </div>
              )}

              <div className="mt-8 p-6 bg-blue-50 border border-blue-200 rounded-lg">
                <h3 className="font-semibold text-blue-900 mb-2">How it works</h3>
                <ol className="text-sm text-blue-800 space-y-2 list-decimal list-inside">
                  <li>Select a deck from your Anki collection</li>
                  <li>Enter instructions for AI (e.g., "Fix typos")</li>
                  <li>AI processes all cards and suggests improvements</li>
                  <li>Review suggestions and accept/reject each one</li>
                  <li>Changes are applied directly to your Anki deck</li>
                </ol>
              </div>
            </div>
          ) : (
            // Review view
            <div className="p-8">
              <div className="max-w-5xl mx-auto">
                <div className="mb-6 flex items-center justify-between">
                  <h2 className="text-xl font-semibold text-gray-900">
                    Review Suggestions
                  </h2>
                  <button
                    onClick={() => setQueue([])}
                    className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                  >
                    Start New Review
                  </button>
                </div>

                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                  <ComparisonView
                    onAccept={handleAccept}
                    onReject={handleReject}
                    onSkip={handleSkip}
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
