import { SessionState } from '../../types/index.js';

interface SessionStateBadgeProps {
  state: SessionState;
}

const STATE_BADGE_CONFIG = {
  [SessionState.PENDING]: {
    label: 'Pending',
    className: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600'
  },
  [SessionState.RUNNING]: {
    label: 'Running',
    className: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-300 dark:border-blue-700'
  },
  [SessionState.COMPLETED]: {
    label: 'Completed',
    className: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-300 dark:border-green-700'
  },
  [SessionState.FAILED]: {
    label: 'Failed',
    className: 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-700'
  },
  [SessionState.CANCELLED]: {
    label: 'Cancelled',
    className: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 border border-amber-300 dark:border-amber-700'
  }
} as const;

export function SessionStateBadge({ state }: SessionStateBadgeProps) {
  const badge = STATE_BADGE_CONFIG[state];

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-semibold ${badge.className}`}>
      {badge.label}
    </span>
  );
}
