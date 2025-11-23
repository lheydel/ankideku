import { WarningIcon, RefreshIcon, CloseIcon } from './Icons';
import { Button } from './Button';

interface ConflictDialogProps {
  isOpen: boolean;
  onCancel: () => void;
  onViewNewVersion: () => void;
}

export function ConflictDialog({ isOpen, onCancel, onViewNewVersion }: ConflictDialogProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-md w-full border border-gray-200 dark:border-gray-700">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 bg-amber-100 dark:bg-amber-900/30 rounded-full flex items-center justify-center flex-shrink-0">
              <WarningIcon className="w-6 h-6 text-amber-600 dark:text-amber-400" />
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                Card Has Been Modified
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                This card has been changed externally since the suggestion was generated.
              </p>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="p-6">
          <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-4 mb-6">
            <p className="text-sm text-amber-900 dark:text-amber-200">
              The original card content has changed. Applying the suggestion now might overwrite those changes.
            </p>
          </div>

          <p className="text-sm text-gray-700 dark:text-gray-300 mb-4">
            What would you like to do?
          </p>

          <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
            <li className="flex items-start gap-2">
              <span className="text-primary-600 dark:text-primary-400 mt-0.5">•</span>
              <span><strong className="text-gray-900 dark:text-gray-100">Cancel:</strong> Abort and keep the current card state</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary-600 dark:text-primary-400 mt-0.5">•</span>
              <span><strong className="text-gray-900 dark:text-gray-100">View New Version:</strong> Reload the card with current content and recalculate the diff</span>
            </li>
          </ul>
        </div>

        {/* Actions */}
        <div className="p-6 border-t border-gray-200 dark:border-gray-700 flex gap-3 justify-end">
          <Button
            onClick={onCancel}
            variant="secondary"
            icon={<CloseIcon className="w-4 h-4" />}
          >
            Cancel
          </Button>
          <Button
            onClick={onViewNewVersion}
            variant="primary"
            icon={<RefreshIcon className="w-4 h-4" />}
          >
            View New Version
          </Button>
        </div>
      </div>
    </div>
  );
}
