import { BulbIcon, ArchiveIcon } from '../ui/Icons';

interface EmptyStateProps {
  isProcessing: boolean;
}

export function EmptyState({ isProcessing }: EmptyStateProps) {
  if (isProcessing) {
    return (
      <div className="card text-center py-16">
        <div className="w-16 h-16 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-2xl flex items-center justify-center mx-auto mb-6 animate-pulse">
          <BulbIcon className="w-8 h-8 text-primary-600 dark:text-primary-400" />
        </div>
        <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-3">
          Analyzing Cards...
        </h3>
        <p className="text-gray-600 dark:text-gray-400">
          AI is processing your deck. Suggestions will appear here as they're generated.
        </p>
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
