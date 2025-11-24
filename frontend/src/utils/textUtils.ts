/**
 * Returns singular or plural form based on count
 */
export function pluralize(count: number, singular: string, plural?: string): string {
  return count === 1 ? singular : (plural || `${singular}s`);
}

/**
 * Format count with pluralized word (e.g., "5 cards", "1 card")
 */
export function formatCount(count: number, singular: string, plural?: string): string {
  return `${count} ${pluralize(count, singular, plural)}`;
}

/**
 * Get the suggested card title based on edit state
 */
export function getSuggestedCardTitle(
  isEditing: boolean,
  showOriginalSuggestions: boolean,
  hasManualEdits: boolean
): string {
  if (isEditing) return 'Editing Card';
  if (showOriginalSuggestions) return 'Original AI Suggestion';
  if (hasManualEdits) return 'Manually Edited Card';
  return 'Suggested Card';
}
