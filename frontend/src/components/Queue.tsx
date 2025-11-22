import { useState } from 'react';
import useStore from '../store/useStore.js';
import { ClipboardIcon, EyeIcon, TagIcon, CheckIcon, ClockIcon, CloseIcon } from './ui/Icons.js';
import { getDisplayField } from '../utils/getDisplayField.js';
import { Button } from './ui/Button.js';

type TabType = 'queue' | 'history';

export default function Queue() {
  const { queue, currentIndex, actionsHistory, fieldDisplayConfig } = useStore();
  const [activeTab, setActiveTab] = useState<TabType>('queue');
  const [searchQuery, setSearchQuery] = useState('');

  if (queue.length === 0 && actionsHistory.length === 0) {
    return null;
  }

  const reviewed = currentIndex;
  const total = queue.length;

  // Filter queue items based on search
  const filteredQueue = queue.filter((item) => {
    if (!searchQuery) return true;
    const displayValue = getDisplayField(item.original, fieldDisplayConfig);
    return displayValue.toLowerCase().includes(searchQuery.toLowerCase());
  });

  // Filter history items based on search
  const filteredHistory = actionsHistory.filter((item) => {
    if (!searchQuery || !item.original) return true;
    const displayValue = getDisplayField(item.original, fieldDisplayConfig);
    return displayValue.toLowerCase().includes(searchQuery.toLowerCase());
  }).reverse(); // Show most recent first

  return (
    <div className="bg-white dark:bg-gray-800 border-r border-gray-200/50 dark:border-gray-700/50 w-72 flex flex-col shadow-sm transition-colors duration-200">
      {/* Header with Tabs */}
      <div className="border-b border-gray-100 dark:border-gray-700">
        <div className="flex border-b border-gray-200 dark:border-gray-700">
          <button
            onClick={() => setActiveTab('queue')}
            className={`flex-1 px-4 py-3 text-sm font-semibold transition-colors ${
              activeTab === 'queue'
                ? 'text-indigo-600 dark:text-indigo-400 border-b-2 border-indigo-600 dark:border-indigo-400 bg-indigo-50/50 dark:bg-indigo-900/30'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700'
            }`}
          >
            Queue {queue.length > 0 && `(${queue.length})`}
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`flex-1 px-4 py-3 text-sm font-semibold transition-colors ${
              activeTab === 'history'
                ? 'text-indigo-600 dark:text-indigo-400 border-b-2 border-indigo-600 dark:border-indigo-400 bg-indigo-50/50 dark:bg-indigo-900/30'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700'
            }`}
          >
            History {actionsHistory.length > 0 && `(${actionsHistory.length})`}
          </button>
        </div>

        {/* Search Bar */}
        <div className="p-3">
          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={`Search ${activeTab}...`}
              className="w-full px-3 py-2 pr-8 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-gray-500 dark:placeholder:text-gray-400"
            />
            {searchQuery && (
              <Button
                onClick={() => setSearchQuery('')}
                variant="ghost"
                icon={<CloseIcon className="w-3.5 h-3.5 text-gray-400" />}
                className="absolute right-2 top-1/2 -translate-y-1/2 hover:bg-gray-200"
              />
            )}
          </div>
        </div>

        {/* Progress Bar (only show for queue tab) */}
        {activeTab === 'queue' && queue.length > 0 && (
          <div className="px-4 pb-3 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-600 dark:text-gray-400 font-medium">Progress</span>
              <span className="text-indigo-600 dark:text-indigo-400 font-bold">{reviewed} / {total}</span>
            </div>
            <div className="bg-gray-100 dark:bg-gray-700 rounded-full h-2 overflow-hidden">
              <div
                className="bg-gradient-to-r from-indigo-600 to-indigo-500 dark:from-indigo-500 dark:to-indigo-400 h-full rounded-full transition-all duration-500 ease-out shadow-sm"
                style={{ width: `${total > 0 ? (reviewed / total) * 100 : 0}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-3">
          {activeTab === 'queue' ? (
            // Queue Items
            filteredQueue.length > 0 ? (
              <div className="space-y-2">
                {filteredQueue.slice(currentIndex, currentIndex + 10).map((item, idx) => {
                  const globalIdx = queue.indexOf(item);
                  const isCurrent = globalIdx === currentIndex;
                  return (
                    <div
                      key={item.noteId}
                      className={`relative rounded-lg border transition-all ${
                        isCurrent
                          ? 'bg-gradient-to-r from-indigo-50 to-blue-50 dark:from-indigo-900/30 dark:to-blue-900/30 border-indigo-200 dark:border-indigo-700 shadow-sm ring-2 ring-indigo-100 dark:ring-indigo-800'
                          : 'bg-white dark:bg-gray-700 border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500 hover:shadow-sm'
                      }`}
                    >
                      {isCurrent && (
                        <div className="absolute -left-1 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-indigo-600 to-indigo-500 dark:from-indigo-500 dark:to-indigo-400 rounded-r"></div>
                      )}
                      <div className="p-3">
                        <div className="flex items-center gap-2">
                          {isCurrent ? (
                            <div className="w-6 h-6 bg-indigo-600 dark:bg-indigo-500 rounded-md flex items-center justify-center flex-shrink-0">
                              <EyeIcon className="w-3.5 h-3.5 text-white" />
                            </div>
                          ) : (
                            <div className="w-6 h-6 bg-gray-100 dark:bg-gray-600 rounded-md flex items-center justify-center flex-shrink-0">
                              <span className="text-xs font-semibold text-gray-500 dark:text-gray-300">{globalIdx + 1}</span>
                            </div>
                          )}
                          <div className="flex-1 min-w-0">
                            <div className={`text-sm font-medium truncate ${isCurrent ? 'text-gray-900 dark:text-gray-100' : 'text-gray-700 dark:text-gray-300'}`}>
                              {getDisplayField(item.original, fieldDisplayConfig)}
                            </div>
                            <div className="flex items-center gap-1.5 mt-1">
                              <TagIcon className="w-3 h-3 text-gray-400 dark:text-gray-500" />
                              <span className="text-xs text-gray-500 dark:text-gray-400">
                                {item.changes ? Object.keys(item.changes).length : 0} change{Object.keys(item.changes || {}).length !== 1 ? 's' : ''}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-8">
                <p className="text-sm text-gray-500 dark:text-gray-400">No cards match your search</p>
              </div>
            )
          ) : (
            // History Items
            filteredHistory.length > 0 ? (
              <div className="space-y-2">
                {filteredHistory.map((item) => (
                  <div
                    key={item.timestamp}
                    className="rounded-lg border bg-white dark:bg-gray-700 border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500 hover:shadow-sm transition-all"
                  >
                    <div className="p-3">
                      <div className="flex items-center gap-2">
                        <div className={`w-6 h-6 rounded-md flex items-center justify-center flex-shrink-0 ${
                          item.action === 'accept'
                            ? 'bg-green-100 dark:bg-green-900'
                            : item.action === 'reject'
                            ? 'bg-red-100 dark:bg-red-900'
                            : 'bg-gray-100 dark:bg-gray-600'
                        }`}>
                          {item.action === 'accept' ? (
                            <CheckIcon className="w-3.5 h-3.5 text-green-600 dark:text-green-400" />
                          ) : item.action === 'reject' ? (
                            <CloseIcon className="w-3.5 h-3.5 text-red-600 dark:text-red-400" />
                          ) : (
                            <ClockIcon className="w-3.5 h-3.5 text-gray-600 dark:text-gray-300" />
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium truncate text-gray-900 dark:text-gray-100">
                            {item.original ? getDisplayField(item.original, fieldDisplayConfig) : 'No content'}
                          </div>
                          <div className="flex items-center gap-2 mt-1">
                            <span className={`text-xs font-medium ${
                              item.action === 'accept'
                                ? 'text-green-600 dark:text-green-400'
                                : item.action === 'reject'
                                ? 'text-red-600 dark:text-red-400'
                                : 'text-gray-600 dark:text-gray-300'
                            }`}>
                              {item.action === 'accept' ? 'Accepted' : item.action === 'reject' ? 'Rejected' : 'Skipped'}
                            </span>
                            <span className="text-xs text-gray-400 dark:text-gray-500">•</span>
                            <span className="text-xs text-gray-500 dark:text-gray-400">
                              {new Date(item.timestamp).toLocaleTimeString()}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {searchQuery ? 'No history matches your search' : 'No history yet'}
                </p>
              </div>
            )
          )}
        </div>
      </div>

      {/* Stats Footer */}
      {activeTab === 'queue' && queue.length > 0 && (
        <div className="p-4 border-t border-gray-100 dark:border-gray-700 bg-gradient-to-b from-gray-50 to-white dark:from-gray-800 dark:to-gray-800">
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-white dark:bg-gray-700 rounded-lg p-3 border border-gray-200 dark:border-gray-600">
              <div className="flex items-center gap-2 mb-1">
                <CheckIcon className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
                <span className="text-xs font-medium text-gray-600 dark:text-gray-400">Done</span>
              </div>
              <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{reviewed}</p>
            </div>
            <div className="bg-white dark:bg-gray-700 rounded-lg p-3 border border-gray-200 dark:border-gray-600">
              <div className="flex items-center gap-2 mb-1">
                <ClockIcon className="w-3.5 h-3.5 text-gray-400 dark:text-gray-500" />
                <span className="text-xs font-medium text-gray-600 dark:text-gray-400">Left</span>
              </div>
              <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{total - reviewed}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
