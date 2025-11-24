/**
 * Get styling classes for the suggestion card based on state
 */
export function getSuggestionCardStyles(showOriginalSuggestions: boolean, hasManualEdits: boolean) {
  const isAISuggestion = showOriginalSuggestions || !hasManualEdits;

  return {
    borderClass: isAISuggestion
      ? 'border-l-green-500 dark:border-l-green-600'
      : 'border-l-blue-500 dark:border-l-blue-600',
    headerBgClass: isAISuggestion
      ? 'bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 border-green-200 dark:border-green-800'
      : 'bg-gradient-to-r from-blue-50 to-sky-50 dark:from-blue-900/20 dark:to-sky-900/20 border-blue-200 dark:border-blue-800',
    iconClass: isAISuggestion
      ? 'text-green-600 dark:text-green-400'
      : 'text-blue-600 dark:text-blue-400',
    titleClass: isAISuggestion
      ? 'text-green-900 dark:text-green-200'
      : 'text-blue-900 dark:text-blue-200',
  };
}

/**
 * Get styling classes for action status display
 */
export function getStatusStyles(status: 'accept' | 'reject' | undefined) {
  if (status === 'accept') {
    return {
      containerClass: 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-700',
      iconClass: 'text-green-600 dark:text-green-400',
      textClass: 'text-green-700 dark:text-green-300',
      label: 'Accepted',
    };
  }
  return {
    containerClass: 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-700',
    iconClass: 'text-red-600 dark:text-red-400',
    textClass: 'text-red-700 dark:text-red-300',
    label: 'Rejected',
  };
}
