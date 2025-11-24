import { CheckIcon, CloseIcon } from '../ui/Icons';
import { getStatusStyles } from '../../utils/styleUtils';

interface ReadonlyStatusProps {
  status?: 'accept' | 'reject';
  timestamp?: string;
}

export function ReadonlyStatus({ status, timestamp }: ReadonlyStatusProps) {
  const styles = getStatusStyles(status);

  return (
    <div className="card p-6 bg-gradient-to-b from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
      <div className={`px-4 py-3 rounded-lg border-2 ${styles.containerClass}`}>
        <div className="flex items-center gap-3">
          {status === 'accept' ? (
            <CheckIcon className={`w-5 h-5 ${styles.iconClass}`} />
          ) : (
            <CloseIcon className={`w-5 h-5 ${styles.iconClass}`} />
          )}
          <div className="flex-1">
            <p className={`text-sm font-semibold ${styles.textClass}`}>{styles.label}</p>
            <p className="text-xs text-gray-600 dark:text-gray-400 mt-0.5">
              {new Date(timestamp || '').toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
