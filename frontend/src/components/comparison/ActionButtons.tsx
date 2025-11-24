import { WarningIcon, CheckIcon, CloseIcon, ArrowRightIcon } from '../ui/Icons';
import { Button } from '../ui/Button';

interface ActionButtonsProps {
  hasManualEdits: boolean;
  showOriginalSuggestions: boolean;
  isAccepting: boolean;
  isRejecting: boolean;
  isDisabled: boolean;
  onAccept: () => void;
  onReject: () => void;
  onSkip: () => void;
}

export function ActionButtons({
  hasManualEdits,
  showOriginalSuggestions,
  isAccepting,
  isRejecting,
  isDisabled,
  onAccept,
  onReject,
  onSkip,
}: ActionButtonsProps) {
  return (
    <div className="card p-6 bg-gradient-to-b from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
      {/* Warning message when viewing AI suggestion with manual edits */}
      {hasManualEdits && showOriginalSuggestions && (
        <div className="mb-4 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg">
          <div className="flex items-start gap-2">
            <WarningIcon className="w-4 h-4 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-amber-800 dark:text-amber-200">
              You're viewing the original AI suggestion. Switch to "Edited" to review or apply your manual changes.
            </p>
          </div>
        </div>
      )}

      <div className="flex items-center justify-between gap-4">
        <div className="flex gap-3">
          <Button
            onClick={onReject}
            variant="danger"
            icon={<CloseIcon className="w-5 h-5" />}
            disabled={isDisabled}
            loading={isRejecting}
          >
            Reject
          </Button>

          <Button
            onClick={onSkip}
            variant="warning"
            icon={<ArrowRightIcon className="w-5 h-5" />}
            disabled={isDisabled}
          >
            Skip
          </Button>
        </div>

        <Button
          onClick={onAccept}
          variant="primary"
          size="lg"
          icon={<CheckIcon className="w-6 h-6" />}
          className="shadow-lg shadow-green-500/30"
          disabled={isDisabled}
          loading={isAccepting}
        >
          <span className="font-semibold">Accept Changes</span>
        </Button>
      </div>
    </div>
  );
}
