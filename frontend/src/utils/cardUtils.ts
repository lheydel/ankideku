import type { NoteField, CardSuggestion, ActionHistoryEntry, ComparisonCard } from '../types';

/**
 * Sort fields by their order property
 */
export function getSortedFields(fields: Record<string, NoteField>): Array<[string, NoteField]> {
  return Object.entries(fields).sort(([, a], [, b]) => a.order - b.order);
}

/**
 * Create a ComparisonCard from a CardSuggestion (for queue items)
 */
export function createComparisonCardFromSuggestion(
  suggestion: CardSuggestion,
  readonly: boolean = false
): ComparisonCard {
  return {
    noteId: suggestion.noteId,
    original: suggestion.original,
    changes: suggestion.changes,
    reasoning: suggestion.reasoning,
    readonly,
    editedChanges: suggestion.editedChanges
  };
}

/**
 * Create a ComparisonCard from an ActionHistoryEntry (for history items)
 */
export function createComparisonCardFromHistory(
  entry: ActionHistoryEntry
): ComparisonCard {
  return {
    noteId: entry.noteId,
    original: entry.original!,
    changes: entry.changes,
    reasoning: entry.reasoning,
    readonly: true,
    status: entry.action as 'accept' | 'reject',
    timestamp: entry.timestamp,
    editedChanges: entry.editedChanges
  };
}

/**
 * Update a card in a queue array
 */
export function updateCardInQueue<T extends { noteId: number }>(
  queue: T[],
  noteId: number,
  updates: Partial<T>
): T[] {
  return queue.map(item =>
    item.noteId === noteId
      ? { ...item, ...updates }
      : item
  );
}
