import { ActionHistoryEntry, FieldDisplayConfig } from '../../../../contract/types';
import { CheckIcon, CloseIcon, ClockIcon } from '../ui/Icons';
import { getDisplayField } from '../../utils/getDisplayField';
import { formatTime } from '../../utils/formatUtils';
import { Card } from './Card';

interface HistoryCardProps {
  item: ActionHistoryEntry;
  isCurrentlyViewing: boolean;
  fieldDisplayConfig: FieldDisplayConfig;
  showDeckName?: boolean;
  onClick: () => void;
}

export function HistoryCard({
  item,
  isCurrentlyViewing,
  fieldDisplayConfig,
  showDeckName = false,
  onClick
}: HistoryCardProps) {
  const getActionIcon = () => {
    if (item.action === 'accept') {
      return <CheckIcon className="w-3.5 h-3.5 text-green-600 dark:text-green-400" />;
    }
    if (item.action === 'reject') {
      return <CloseIcon className="w-3.5 h-3.5 text-red-600 dark:text-red-400" />;
    }
    return <ClockIcon className="w-3.5 h-3.5 text-gray-600 dark:text-gray-300" />;
  };

  const getActionBgClass = () => {
    if (item.action === 'accept') return 'bg-green-100 dark:bg-green-900';
    if (item.action === 'reject') return 'bg-red-100 dark:bg-red-900';
    return 'bg-gray-100 dark:bg-gray-600';
  };

  const getActionTextClass = () => {
    if (item.action === 'accept') return 'text-green-600 dark:text-green-400';
    if (item.action === 'reject') return 'text-red-600 dark:text-red-400';
    return 'text-gray-600 dark:text-gray-300';
  };

  const getActionLabel = () => {
    if (item.action === 'accept') return 'Accepted';
    if (item.action === 'reject') return 'Rejected';
    return 'Skipped';
  };

  const icon = (
    <div className={`w-6 h-6 rounded-md flex items-center justify-center flex-shrink-0 ${getActionBgClass()}`}>
      {getActionIcon()}
    </div>
  );

  const metadata = (
    <div className="flex items-center gap-2 mt-1 flex-wrap">
      <span className={`text-xs font-medium ${getActionTextClass()}`}>
        {getActionLabel()}
      </span>
      <span className="text-xs text-gray-400 dark:text-gray-500">•</span>
      <span className="text-xs text-gray-500 dark:text-gray-400">
        {formatTime(new Date(item.timestamp))}
      </span>
      {showDeckName && item.deckName && (
        <>
          <span className="text-xs text-gray-400 dark:text-gray-500">•</span>
          <span
            className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[120px]"
            title={item.deckName}
          >
            {item.deckName}
          </span>
        </>
      )}
    </div>
  );

  return (
    <Card
      icon={icon}
      title={item.original ? getDisplayField(item.original, fieldDisplayConfig) : 'No content'}
      metadata={metadata}
      isActive={isCurrentlyViewing}
      showActiveIndicator={false}
      onClick={onClick}
    />
  );
}
