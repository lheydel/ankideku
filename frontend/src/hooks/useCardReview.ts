import { useCallback } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';

export function useCardReview() {
  const {
    selectedDeck,
    queue,
    currentIndex,
    getCurrentCard,
    nextCard,
    skipCard,
    setQueue,
    addToHistory,
    removeFromQueue,
  } = useStore();

  const handleAccept = useCallback(async (
    onSuccess: (message: string) => void,
    onError: (message: string) => void
  ) => {
    const card = getCurrentCard();
    if (!card) return;

    try {
      // Use the deck name from the card's original data to ensure cache updates correctly
      const deckName = card.original.deckName || selectedDeck;
      await ankiApi.updateNote(card.noteId, card.changes, deckName);

      addToHistory({
        action: 'accept',
        noteId: card.noteId,
        changes: card.changes,
        original: card.original,
      });

      // Remove from queue
      removeFromQueue(currentIndex);

      onSuccess('Changes accepted and applied');

      if (queue.length <= 1) {
        onSuccess('All suggestions reviewed!');
        setQueue([]);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      onError('Failed to apply changes: ' + errorMessage);
    }
  }, [getCurrentCard, selectedDeck, currentIndex, queue.length, removeFromQueue, setQueue, addToHistory]);

  const handleReject = useCallback((onSuccess: (message: string) => void) => {
    const card = getCurrentCard();
    if (!card) return;

    addToHistory({
      action: 'reject',
      noteId: card.noteId,
      changes: card.changes,
      original: card.original,
    });

    // Remove from queue
    removeFromQueue(currentIndex);

    onSuccess('Suggestion rejected');

    if (queue.length <= 1) {
      onSuccess('All suggestions reviewed!');
      setQueue([]);
    }
  }, [getCurrentCard, currentIndex, queue.length, removeFromQueue, setQueue, addToHistory]);

  const handleSkip = useCallback((onSuccess: (message: string) => void) => {
    skipCard();
    onSuccess('Card skipped - moved to end of queue');
  }, [skipCard]);

  return {
    handleAccept,
    handleReject,
    handleSkip,
  };
}
