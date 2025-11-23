import { useCallback, useState } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';
import type { Note, ActionHistoryEntry, CardSuggestion } from '../types';

export function useCardReview() {
  const {
    selectedDeck,
    currentSession,
    queue,
    currentIndex,
    getCurrentCard,
    skipCard,
    setQueue,
    addToHistory,
    removeFromQueue,
    setSelectedCard,
  } = useStore();

  const [conflictDetected, setConflictDetected] = useState(false);
  const [pendingAction, setPendingAction] = useState<{
    type: 'accept' | 'reject';
    editedChanges?: Record<string, string>;
    onSuccess: (message: string) => void;
    onError?: (message: string) => void;
  } | null>(null);

  // Check if card has been modified externally
  const checkForConflict = useCallback(async (noteId: number, originalFields: Record<string, { value: string; order: number }>): Promise<Note | null> => {
    try {
      const currentNote = await ankiApi.getNote(noteId);

      // Compare all fields
      for (const [fieldName, fieldData] of Object.entries(originalFields)) {
        const currentField = currentNote.fields[fieldName];
        // Extract value from NoteField object or treat as string
        const currentValue = typeof currentField === 'string'
          ? currentField
          : (currentField?.value || '');

        if (currentValue !== fieldData.value) {
          // Field has been modified - return the current note
          return currentNote;
        }
      }

      // No conflicts
      return null;
    } catch (error) {
      console.error('Failed to check for conflicts:', error);
      // If we can't check, assume no conflict
      return null;
    }
  }, []);

  // Helper to check if editedChanges differ from original changes
  const hasActualEdits = useCallback((editedChanges: Record<string, string> | undefined, originalChanges: Record<string, string>): boolean => {
    return editedChanges !== undefined && JSON.stringify(editedChanges) !== JSON.stringify(originalChanges);
  }, []);

  // Helper to create history entry
  const createHistoryEntry = useCallback((
    action: 'accept' | 'reject',
    card: CardSuggestion,
    deckName: string | null,
    editedChanges?: Record<string, string>
  ): ActionHistoryEntry => {
    return {
      action,
      noteId: card.noteId,
      changes: action === 'accept' ? (editedChanges || card.changes) : card.changes,
      original: card.original,
      reasoning: card.reasoning,
      timestamp: new Date().toISOString(),
      sessionId: currentSession || undefined,
      deckName: deckName || undefined,
      editedChanges: hasActualEdits(editedChanges, card.changes) ? editedChanges : undefined,
    };
  }, [currentSession, hasActualEdits]);

  // Helper to save history (local + backend)
  const saveHistory = useCallback(async (historyEntry: ActionHistoryEntry) => {
    addToHistory(historyEntry);

    if (currentSession) {
      try {
        await ankiApi.saveHistoryAction(currentSession, historyEntry);
      } catch (err) {
        console.error('Failed to save history to backend:', err);
        // Don't fail the whole operation if history save fails
      }
    }
  }, [currentSession, addToHistory]);

  // Helper to move to next card in queue
  const moveToNextCard = useCallback(() => {
    // Get the next card BEFORE removing current one from queue
    // The next card is the one immediately after the current index (which will shift down after removal)
    const nextCard = queue.length > 1 ? queue[currentIndex + 1] || queue[currentIndex - 1] : null;

    // Remove from queue
    removeFromQueue(currentIndex);

    // Update selected card to the next one
    if (nextCard) {
      setSelectedCard({
        noteId: nextCard.noteId,
        original: nextCard.original,
        changes: nextCard.changes,
        reasoning: nextCard.reasoning,
        readonly: false,
        editedChanges: nextCard.editedChanges
      });
    } else {
      setSelectedCard(null);
    }

    // Check if all cards reviewed
    return queue.length <= 1;
  }, [queue, currentIndex, removeFromQueue, setSelectedCard]);

  // Common action handler for both accept and reject
  const performAction = useCallback(async (
    type: 'accept' | 'reject',
    onSuccess: (message: string) => void,
    onError: (message: string) => void,
    editedChanges?: Record<string, string>
  ) => {
    const card = getCurrentCard();
    if (!card) return;

    // Check for conflicts
    const conflictingNote = await checkForConflict(card.noteId, card.original.fields);
    if (conflictingNote) {
      // Store the pending action and show conflict dialog
      setPendingAction({ type, editedChanges, onSuccess, onError });
      setConflictDetected(true);
      return;
    }

    const deckName = card.original.deckName || selectedDeck;

    try {
      // Only apply changes to Anki for accept action
      if (type === 'accept') {
        const changesToApply = editedChanges || card.changes;
        await ankiApi.updateNote(card.noteId, changesToApply, deckName);
      }

      // Create and save history entry
      const historyEntry = createHistoryEntry(type, card, deckName, editedChanges);
      await saveHistory(historyEntry);

      // Move to next card and check if all reviewed
      const allReviewed = moveToNextCard();

      // Success messages
      const actionMessage = type === 'accept' ? 'Changes accepted and applied' : 'Suggestion rejected';
      onSuccess(actionMessage);

      if (allReviewed) {
        onSuccess('All suggestions reviewed!');
        setQueue([]);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      onError(`Failed to ${type} changes: ${errorMessage}`);
    }
  }, [getCurrentCard, selectedDeck, checkForConflict, createHistoryEntry, saveHistory, moveToNextCard, setQueue]);

  const handleAccept = useCallback(async (
    onSuccess: (message: string) => void,
    onError: (message: string) => void,
    editedChanges?: Record<string, string>
  ) => {
    await performAction('accept', onSuccess, onError, editedChanges);
  }, [performAction]);

  const handleReject = useCallback(async (
    onSuccess: (message: string) => void,
    editedChanges?: Record<string, string>
  ) => {
    await performAction('reject', onSuccess, () => {}, editedChanges);
  }, [performAction]);

  const handleSkip = useCallback((onSuccess: (message: string) => void) => {
    skipCard();
    onSuccess('Card skipped - moved to end of queue');
  }, [skipCard]);

  // Handle viewing the card with new version after conflict detected
  const handleViewNewVersion = useCallback(async () => {
    const card = getCurrentCard();
    if (!card || !currentSession) return;

    try {
      // Call backend to refresh the suggestion with current Anki state
      const updatedSuggestion = await ankiApi.refreshSuggestionOriginal(currentSession, card.noteId);

      // Update the card in the queue
      const updatedQueue = queue.map(item =>
        item.noteId === card.noteId
          ? { ...item, original: updatedSuggestion.original }
          : item
      );
      setQueue(updatedQueue);

      // Update the selected card with refreshed original fields
      setSelectedCard({
        ...card,
        original: updatedSuggestion.original
      });

      // Close the conflict dialog
      setConflictDetected(false);
      setPendingAction(null);
    } catch (error) {
      console.error('Failed to refresh card with new version:', error);
      // Close dialog on error
      setConflictDetected(false);
      setPendingAction(null);
    }
  }, [getCurrentCard, currentSession, queue, setQueue, setSelectedCard]);

  // Handle canceling the conflict resolution
  const handleCancelConflict = useCallback(() => {
    setConflictDetected(false);
    setPendingAction(null);
  }, []);

  return {
    handleAccept,
    handleReject,
    handleSkip,
    conflictDetected,
    handleViewNewVersion,
    handleCancelConflict,
  };
}
