import { useCallback } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';

export function useCardReview() {
  const {
    selectedDeck,
    currentSession,
    queue,
    currentIndex,
    getCurrentCard,
    nextCard,
    skipCard,
    setQueue,
    addToHistory,
    removeFromQueue,
    setSelectedCard,
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

      const historyEntry = {
        action: 'accept' as const,
        noteId: card.noteId,
        changes: card.changes,
        original: card.original,
        reasoning: card.reasoning,
        timestamp: new Date().toISOString(),
        sessionId: currentSession || undefined,
        deckName: deckName || undefined,
      };

      // Add to local history
      addToHistory(historyEntry);

      // Persist to backend if we have a session
      if (currentSession) {
        try {
          await ankiApi.saveHistoryAction(currentSession, historyEntry);
        } catch (err) {
          console.error('Failed to save history to backend:', err);
          // Don't fail the whole operation if history save fails
        }
      }

      // Remove from queue
      removeFromQueue(currentIndex);

      // Update selected card to the next one in queue
      const nextCard = queue[currentIndex]; // After removal, currentIndex points to the next card
      if (nextCard) {
        setSelectedCard({
          noteId: nextCard.noteId,
          original: nextCard.original,
          changes: nextCard.changes,
          reasoning: nextCard.reasoning,
          readonly: false
        });
      } else {
        setSelectedCard(null);
      }

      onSuccess('Changes accepted and applied');

      if (queue.length <= 1) {
        onSuccess('All suggestions reviewed!');
        setQueue([]);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      onError('Failed to apply changes: ' + errorMessage);
    }
  }, [getCurrentCard, selectedDeck, currentSession, currentIndex, queue, removeFromQueue, setQueue, addToHistory, setSelectedCard]);

  const handleReject = useCallback(async (onSuccess: (message: string) => void) => {
    const card = getCurrentCard();
    if (!card) return;

    const deckName = card.original.deckName || selectedDeck;
    const historyEntry = {
      action: 'reject' as const,
      noteId: card.noteId,
      changes: card.changes,
      original: card.original,
      reasoning: card.reasoning,
      timestamp: new Date().toISOString(),
      sessionId: currentSession || undefined,
      deckName: deckName || undefined,
    };

    // Add to local history
    addToHistory(historyEntry);

    // Persist to backend if we have a session
    if (currentSession) {
      try {
        await ankiApi.saveHistoryAction(currentSession, historyEntry);
      } catch (err) {
        console.error('Failed to save history to backend:', err);
        // Don't fail the whole operation if history save fails
      }
    }

    // Remove from queue
    removeFromQueue(currentIndex);

    // Update selected card to the next one in queue
    const nextCard = queue[currentIndex]; // After removal, currentIndex points to the next card
    if (nextCard) {
      setSelectedCard({
        noteId: nextCard.noteId,
        original: nextCard.original,
        changes: nextCard.changes,
        reasoning: nextCard.reasoning,
        readonly: false
      });
    } else {
      setSelectedCard(null);
    }

    onSuccess('Suggestion rejected');

    if (queue.length <= 1) {
      onSuccess('All suggestions reviewed!');
      setQueue([]);
    }
  }, [getCurrentCard, selectedDeck, currentSession, currentIndex, queue, removeFromQueue, setQueue, addToHistory, setSelectedCard]);

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
