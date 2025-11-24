import type { ActionHistoryEntry } from '../../../../contract/types';
import type { FieldDisplayConfig } from '../../types';
import { getDisplayField } from '../../utils/getDisplayField';
import { formatTime } from '../../utils/formatUtils';
import { getActionStyle } from '../../utils/actionStyles';
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
  const actionStyle = getActionStyle(item.action);
  const ActionIcon = actionStyle.Icon;

  const icon = (
    <div className={`w-6 h-6 rounded-md flex items-center justify-center flex-shrink-0 ${actionStyle.bgClass}`}>
      <ActionIcon className={`w-3.5 h-3.5 ${actionStyle.iconClass}`} />
    </div>
  );

  const metadata = (
    <div className="flex items-center gap-2 mt-1 flex-wrap">
      <span className={`text-xs font-medium ${actionStyle.textClass}`}>
        {actionStyle.label}
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
