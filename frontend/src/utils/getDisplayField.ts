import type { Note, FieldDisplayConfig } from '../types/index.js';

/**
 * Gets the display value for a note based on the field display configuration
 * Falls back to the first field if no configuration exists for the model
 */
export function getDisplayField(note: Note, fieldDisplayConfig: FieldDisplayConfig): string {
  const configuredField = fieldDisplayConfig[note.modelName];

  if (configuredField && note.fields[configuredField]) {
    return note.fields[configuredField].value || 'No content';
  }

  // Fallback to first field
  const firstField = Object.values(note.fields).sort((a, b) => a.order - b.order)[0];
  return firstField?.value || 'No content';
}
