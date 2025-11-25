import { BulbIcon, ArchiveIcon } from '../ui/Icons';

interface EmptyStateProps {
  isProcessing: boolean;
  progress?: number;
  total?: number;
}

export function EmptyState({ isProcessing, progress = 0, total = 0 }: EmptyStateProps) {
  if (isProcessing) {
    const percentage = total > 0 ? Math.round((progress / total) * 100) : 0;
    const hasProgress = total > 0;

    return (
      <div className="card text-center py-16">
        <div className="w-16 h-16 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-2xl flex items-center justify-center mx-auto mb-6 animate-pulse">
          <BulbIcon className="w-8 h-8 text-primary-600 dark:text-primary-400" />
        </div>
        <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-3">
          Analyzing Cards...
        </h3>

        {hasProgress ? (
          <div className="max-w-xs mx-auto">
            {/* Progress bar */}
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-3">
              <div
                className="bg-primary-500 h-2 rounded-full transition-all duration-300 ease-out"
                style={{ width: `${percentage}%` }}
              />
            </div>
            {/* Progress text */}
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Processing {progress} of {total} cards ({percentage}%)
            </p>
          </div>
        ) : (
          <p className="text-gray-600 dark:text-gray-400">
            AI is processing your deck. Suggestions will appear here as they're generated.
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="card text-center py-16">
      <ArchiveIcon className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
      <p className="text-gray-500 dark:text-gray-400 font-medium">No card to review</p>
    </div>
  );
}
