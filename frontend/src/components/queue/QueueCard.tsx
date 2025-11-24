import { forwardRef } from 'react';
import type { CardSuggestion } from '../../../../contract/types';
import type { FieldDisplayConfig } from '../../types';
import { EyeIcon, TagIcon } from '../ui/Icons';
import { getDisplayField } from '../../utils/getDisplayField';
import { Card } from './Card';

interface QueueCardProps {
  item: CardSuggestion;
  index: number;
  isCurrent: boolean;
  fieldDisplayConfig: FieldDisplayConfig;
  onClick: () => void;
}

export const QueueCard = forwardRef<HTMLDivElement, QueueCardProps>(
  ({ item, index, isCurrent, fieldDisplayConfig, onClick }, ref) => {
    const changeCount = item.changes ? Object.keys(item.changes).length : 0;

    const icon = isCurrent ? (
      <div className="w-6 h-6 bg-primary-600 dark:bg-primary-500 rounded-md flex items-center justify-center flex-shrink-0">
        <EyeIcon className="w-3.5 h-3.5 text-white" />
      </div>
    ) : (
      <div className="w-6 h-6 bg-gray-100 dark:bg-gray-600 rounded-md flex items-center justify-center flex-shrink-0">
        <span className="text-xs font-semibold text-gray-500 dark:text-gray-300">
          {index + 1}
        </span>
      </div>
    );

    const metadata = (
      <div className="flex items-center gap-1.5 mt-1">
        <TagIcon className="w-3 h-3 text-gray-400 dark:text-gray-500" />
        <span className="text-xs text-gray-500 dark:text-gray-400">
          {changeCount} change{changeCount !== 1 ? 's' : ''}
        </span>
      </div>
    );

    return (
      <Card
        ref={ref}
        icon={icon}
        title={getDisplayField(item.original, fieldDisplayConfig)}
        metadata={metadata}
        isActive={isCurrent}
        showActiveIndicator={true}
        onClick={onClick}
      />
    );
  }
);

QueueCard.displayName = 'QueueCard';
