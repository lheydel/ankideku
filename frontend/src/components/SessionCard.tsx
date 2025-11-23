import { ClockIcon, TrashIcon } from './ui/Icons.js';
import { SessionStateBadge } from './ui/SessionStateBadge.js';
import { formatDateTime } from '../utils/formatUtils.js';
import type { SessionMetadata } from '../types/index.js';

interface SessionCardProps {
  session: SessionMetadata;
  onLoad: () => void;
  onDelete: () => void;
  onViewOutput: () => void;
}

export function SessionCard({ session, onLoad, onDelete, onViewOutput }: SessionCardProps) {
  const date = new Date(session.timestamp);

  return (
    <div className="group relative bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-6 hover:border-primary-300 dark:hover:border-primary-600 hover:shadow-lg transition-all duration-200">
      {/* Delete Button */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          onDelete();
        }}
        className="absolute top-4 right-4 p-2 rounded-lg text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors opacity-0 group-hover:opacity-100"
        title="Delete session"
      >
        <TrashIcon className="w-4 h-4" />
      </button>

      {/* Session Card Content */}
      <button onClick={onLoad} className="w-full text-left">
        <div className="flex items-start justify-between mb-4">
          <div className="w-12 h-12 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
            <ClockIcon className="w-6 h-6 text-primary-600 dark:text-primary-400" />
          </div>
          {session.state && (
            <div>
              <SessionStateBadge state={session.state.state} />
            </div>
          )}
        </div>

        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
          {session.deckName}
        </h3>

        <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
          {formatDateTime(date)}
        </p>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs text-primary-600 dark:text-primary-400 font-medium">
            <span>Load Session</span>
            <span className="group-hover:translate-x-1 transition-transform">→</span>
          </div>
          <span
            onClick={(e) => {
              e.stopPropagation();
              onViewOutput();
            }}
            className="text-xs text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 underline cursor-pointer"
          >
            View Output
          </span>
        </div>
      </button>
    </div>
  );
}
