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
  } = useStore();

  const handleAccept = useCallback(async (
    onSuccess: (message: string) => void,
    onError: (message: string) => void
  ) => {
    const card = getCurrentCard();
    if (!card) return;

    try {
      await ankiApi.updateNote(card.noteId, card.changes, selectedDeck);

      addToHistory({
        action: 'accept',
        noteId: card.noteId,
        changes: card.changes,
        original: card.original,
      });

      onSuccess('Changes accepted and applied');

      if (currentIndex < queue.length - 1) {
        nextCard();
      } else {
        onSuccess('All suggestions reviewed!');
        setQueue([]);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      onError('Failed to apply changes: ' + errorMessage);
    }
  }, [getCurrentCard, selectedDeck, currentIndex, queue.length, nextCard, setQueue, addToHistory]);

  const handleReject = useCallback((onSuccess: (message: string) => void) => {
    const card = getCurrentCard();
    if (!card) return;

    addToHistory({
      action: 'reject',
      noteId: card.noteId,
      changes: card.changes,
    });

    onSuccess('Suggestion rejected');

    if (currentIndex < queue.length - 1) {
      nextCard();
    } else {
      onSuccess('All suggestions reviewed!');
      setQueue([]);
    }
  }, [getCurrentCard, currentIndex, queue.length, nextCard, setQueue, addToHistory]);

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
