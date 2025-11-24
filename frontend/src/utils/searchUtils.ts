import type { Note, FieldDisplayConfig } from '../types';
import { getDisplayField } from './getDisplayField';

/**
 * Create a search filter function for items with original Note
 */
export function createSearchFilter<T extends { original?: Note | null }>(
  searchQuery: string,
  fieldDisplayConfig: FieldDisplayConfig
) {
  if (!searchQuery) {
    return () => true;
  }

  const lowerQuery = searchQuery.toLowerCase();

  return (item: T): boolean => {
    if (!item.original) return true;
    const displayValue = getDisplayField(item.original, fieldDisplayConfig);
    return displayValue.toLowerCase().includes(lowerQuery);
  };
}

/**
 * Filter an array with search and optionally reverse
 */
export function filterWithSearch<T extends { original?: Note | null }>(
  items: T[],
  searchQuery: string,
  fieldDisplayConfig: FieldDisplayConfig,
  reverse: boolean = false
): T[] {
  const filtered = items.filter(createSearchFilter(searchQuery, fieldDisplayConfig));
  return reverse ? filtered.reverse() : filtered;
}
