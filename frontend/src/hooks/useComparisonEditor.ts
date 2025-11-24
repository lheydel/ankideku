import { useState, useEffect, useCallback } from 'react';
import { ankiApi } from '../services/api';
import useStore from '../store/useStore';
import { filterActualEdits, getEditedChangesForAction } from '../utils/editingUtils';
import { updateCardInQueue } from '../utils/cardUtils';
import type { ComparisonCard } from '../types';

interface UseComparisonEditorProps {
  card: ComparisonCard | null;
}

/**
 * Hook for managing the editing state in ComparisonView.
 * Handles:
 * - editedChanges state
 * - isEditing mode toggle
 * - showOriginalSuggestions toggle
 * - Save/revert operations
 */
export function useComparisonEditor({ card }: UseComparisonEditorProps) {
  const { queue, currentSession, setQueue, setSelectedCard } = useStore();

  // Edit state
  const [editedChanges, setEditedChanges] = useState<Record<string, string>>({});
  const [isEditing, setIsEditing] = useState(false);
  const [showOriginalSuggestions, setShowOriginalSuggestions] = useState(false);
  const [showRevertConfirm, setShowRevertConfirm] = useState(false);

  // Check if there are any manual edits
  const hasManualEdits = !!(card?.editedChanges && Object.keys(card.editedChanges).length > 0);

  // Reset state when card changes
  useEffect(() => {
    if (card) {
      setEditedChanges(card.editedChanges || card.changes || {});
      setShowOriginalSuggestions(false);
      setIsEditing(false);
    }
  }, [card]);

  // Update a single field
  const updateField = useCallback((fieldName: string, value: string) => {
    setEditedChanges(prev => ({ ...prev, [fieldName]: value }));
  }, []);

  // Save edited changes to backend
  const saveEdits = useCallback(async () => {
    if (!card || !currentSession) return;

    const actuallyEditedChanges = filterActualEdits(editedChanges, card.changes, card.original.fields);
    const hasAnyChanges = Object.keys(actuallyEditedChanges).length > 0;

    if (hasAnyChanges) {
      try {
        await ankiApi.saveEditedChanges(currentSession, card.noteId, actuallyEditedChanges);
        setQueue(updateCardInQueue(queue, card.noteId, { editedChanges: actuallyEditedChanges }));
        setSelectedCard({ ...card, editedChanges: actuallyEditedChanges });
      } catch (error) {
        console.error('Failed to save edited changes:', error);
      }
    } else if (card.editedChanges && Object.keys(card.editedChanges).length > 0) {
      // No changes - revert to AI suggestion
      try {
        await ankiApi.revertEditedChanges(currentSession, card.noteId);
        setQueue(updateCardInQueue(queue, card.noteId, { editedChanges: undefined }));
        setSelectedCard({ ...card, editedChanges: undefined });
      } catch (error) {
        console.error('Failed to revert edited changes:', error);
      }
    }
  }, [card, currentSession, editedChanges, queue, setQueue, setSelectedCard]);

  // Toggle edit mode (saves on exit)
  const toggleEditMode = useCallback(async () => {
    if (isEditing) {
      await saveEdits();
    }
    setIsEditing(!isEditing);
  }, [isEditing, saveEdits]);

  // Toggle between original AI suggestion and edited version
  const toggleOriginalSuggestions = useCallback(() => {
    setShowOriginalSuggestions(prev => !prev);
  }, []);

  // Revert to AI suggestion
  const revertEdits = useCallback(async () => {
    if (!currentSession || !card?.noteId) return;

    try {
      await ankiApi.revertEditedChanges(currentSession, card.noteId);
      setEditedChanges(card.changes || {});
      setShowOriginalSuggestions(false);
      setQueue(updateCardInQueue(queue, card.noteId, { editedChanges: undefined }));
      setSelectedCard({ ...card, editedChanges: undefined });
      setShowRevertConfirm(false);
    } catch (error) {
      console.error('Failed to revert edits:', error);
      setShowRevertConfirm(false);
    }
  }, [card, currentSession, queue, setQueue, setSelectedCard]);

  // Get edited changes for action (accept/reject)
  const getChangesForAction = useCallback(() => {
    if (!card) return undefined;
    return getEditedChangesForAction(editedChanges, card.changes, card.original.fields);
  }, [card, editedChanges]);

  return {
    // State
    editedChanges,
    isEditing,
    showOriginalSuggestions,
    showRevertConfirm,
    hasManualEdits,

    // Actions
    updateField,
    toggleEditMode,
    toggleOriginalSuggestions,
    setShowRevertConfirm,
    revertEdits,
    getChangesForAction,
  };
}
