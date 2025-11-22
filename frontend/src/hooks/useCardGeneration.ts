import { useCallback } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';
import type { Note, CardSuggestion } from '../types';

export function useCardGeneration() {
  const {
    selectedDeck,
    setQueue,
    setProcessing,
    setProgress,
    addToPromptHistory,
  } = useStore();

  const generateSuggestions = useCallback(async (
    prompt: string,
    onSuccess: (message: string) => void,
    onError: (message: string) => void
  ) => {
    if (!selectedDeck || !prompt.trim()) return;

    try {
      setProcessing(true);
      setProgress(0, 0);

      addToPromptHistory(prompt);

      // Fetch notes from deck
      const response = await ankiApi.getDeckNotes(selectedDeck);
      const notes = response.notes || (response as any);

      if (response.fromCache) {
        onSuccess(`Loaded ${notes.length} cards from cache`);
      }

      setProgress(notes.length, notes.length);

      // Create mock suggestions with varied changes
      const mockSuggestions: CardSuggestion[] = notes.slice(0, Math.min(10, notes.length)).map((note: Note) => {
        const fieldNames = Object.keys(note.fields);
        const changes: Record<string, string> = {};

        fieldNames.forEach((fieldName, fieldIndex) => {
          const originalValue = note.fields[fieldName].value;

          if (fieldIndex === 0) {
            changes[fieldName] = originalValue + ' (with additional context)';
          } else if (fieldIndex === 1 && originalValue.length > 10) {
            const halfLength = Math.floor(originalValue.length / 2);
            changes[fieldName] = originalValue.substring(0, halfLength);
          } else if (fieldIndex === 2) {
            changes[fieldName] = originalValue.replace(/\w+/, 'corrected');
          }
        });

        return {
          noteId: note.noteId,
          original: note,
          changes,
          reasoning: `Found ${Object.keys(changes).length} field(s) that could be improved: ${Object.keys(changes).join(', ')}. Added missing context, removed redundant information, and corrected terminology.`,
        };
      });

      setQueue(mockSuggestions);
      setProcessing(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to generate suggestions';
      onError(errorMessage);
      setProcessing(false);
    }
  }, [selectedDeck, setQueue, setProcessing, setProgress, addToPromptHistory]);

  return { generateSuggestions };
}
