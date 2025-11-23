import { forwardRef, ReactNode } from 'react';

interface CardProps {
  icon: ReactNode;
  title: string;
  metadata: ReactNode;
  isActive: boolean;
  showActiveIndicator?: boolean;
  onClick: () => void;
}

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ icon, title, metadata, isActive, showActiveIndicator = false, onClick }, ref) => {
    return (
      <div
        ref={ref}
        onClick={onClick}
        className={`relative rounded-lg border transition-all cursor-pointer ${
          isActive
            ? 'bg-gradient-to-r from-primary-50 to-blue-50 dark:from-primary-900/30 dark:to-blue-900/30 border-primary-200 dark:border-primary-700 shadow-sm ring-2 ring-primary-100 dark:ring-primary-800'
            : 'bg-white dark:bg-gray-700 border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500 hover:shadow-sm'
        }`}
      >
        {isActive && showActiveIndicator && (
          <div className="absolute -left-1 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-primary-600 to-primary-500 dark:from-primary-500 dark:to-primary-400 rounded-r" />
        )}
        <div className="p-3">
          <div className="flex items-center gap-2">
            {icon}
            <div className="flex-1 min-w-0">
              <div
                className={`text-sm font-medium truncate ${
                  isActive ? 'text-gray-900 dark:text-gray-100' : 'text-gray-700 dark:text-gray-300'
                }`}
              >
                {title}
              </div>
              {metadata}
            </div>
          </div>
        </div>
      </div>
    );
  }
);

Card.displayName = 'Card';
