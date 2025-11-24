import type { NoteField } from '../types';

/**
 * Filter edited changes to only include fields that actually differ from AI suggestion or original.
 * Returns only the fields that the user modified.
 */
export function filterActualEdits(
  editedChanges: Record<string, string>,
  aiChanges: Record<string, string> | undefined,
  originalFields: Record<string, NoteField>
): Record<string, string> {
  const actualEdits: Record<string, string> = {};

  Object.entries(editedChanges).forEach(([fieldName, editedValue]) => {
    const aiSuggestedValue = aiChanges?.[fieldName];
    const originalValue = originalFields[fieldName]?.value ?? '';
    const expectedValue = aiSuggestedValue !== undefined ? aiSuggestedValue : originalValue;

    if (editedValue !== expectedValue) {
      actualEdits[fieldName] = editedValue;
    }
  });

  return actualEdits;
}

/**
 * Check if there are any actual edits (non-empty result from filterActualEdits)
 */
export function hasActualEdits(
  editedChanges: Record<string, string>,
  aiChanges: Record<string, string> | undefined,
  originalFields: Record<string, NoteField>
): boolean {
  return Object.keys(filterActualEdits(editedChanges, aiChanges, originalFields)).length > 0;
}

/**
 * Get the display value for a field based on current state.
 * Falls back through: editedChanges -> aiChanges -> originalValue
 */
export function getFieldDisplayValue(
  fieldName: string,
  originalValue: string,
  aiChanges: Record<string, string> | undefined,
  editedChanges: Record<string, string>,
  showOriginalSuggestions: boolean
): string {
  if (showOriginalSuggestions) {
    return aiChanges?.[fieldName] ?? originalValue;
  }
  return editedChanges[fieldName] ?? aiChanges?.[fieldName] ?? originalValue;
}

/**
 * Merge AI changes with user edits (user edits take priority)
 */
export function mergeChanges(
  aiChanges: Record<string, string>,
  editedChanges?: Record<string, string>
): Record<string, string> {
  if (!editedChanges) return aiChanges;
  return { ...aiChanges, ...editedChanges };
}

/**
 * Get edited changes to pass to action handlers, or undefined if no edits
 */
export function getEditedChangesForAction(
  editedChanges: Record<string, string>,
  aiChanges: Record<string, string> | undefined,
  originalFields: Record<string, NoteField>
): Record<string, string> | undefined {
  const actualEdits = filterActualEdits(editedChanges, aiChanges, originalFields);
  return Object.keys(actualEdits).length > 0 ? actualEdits : undefined;
}
