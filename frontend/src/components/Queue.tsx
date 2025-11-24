import { useState, useEffect, useRef, useMemo } from 'react';
import useStore from '../store/useStore.js';
import { CheckIcon, ClockIcon, CloseIcon } from './ui/Icons.js';
import { Button } from './ui/Button.js';
import { TabButton } from './queue/TabButton.js';
import { ToggleButton } from './queue/ToggleButton.js';
import { QueueCard } from './queue/QueueCard.js';
import { HistoryCard } from './queue/HistoryCard.js';
import { StatCard } from './queue/StatCard.js';
import { createComparisonCardFromSuggestion, createComparisonCardFromHistory } from '../utils/cardUtils.js';
import { filterWithSearch } from '../utils/searchUtils.js';

type TabType = 'queue' | 'history';

export default function Queue() {
  const {
    queue,
    currentIndex,
    actionsHistory,
    globalHistory,
    historyViewMode,
    fieldDisplayConfig,
    selectedCard,
    goToCard,
    toggleHistoryView,
    loadGlobalHistory,
    setSelectedCard,
  } = useStore();
  const [activeTab, setActiveTab] = useState<TabType>('queue');
  const [searchQuery, setSearchQuery] = useState('');
  const currentItemRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to current item
  useEffect(() => {
    if (currentItemRef.current && activeTab === 'queue') {
      currentItemRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      });
    }
  }, [currentIndex, activeTab]);

  // Load global history when switching to global view
  useEffect(() => {
    if (activeTab === 'history' && historyViewMode === 'global') {
      loadGlobalHistory();
    }
  }, [activeTab, historyViewMode, loadGlobalHistory]);

  const reviewed = actionsHistory.length;
  const total = queue.length + actionsHistory.length;

  // Get the appropriate history based on view mode
  const displayHistory = historyViewMode === 'session' ? actionsHistory : globalHistory;

  // Filter items based on search (memoized for performance)
  const filteredQueue = useMemo(
    () => filterWithSearch(queue, searchQuery, fieldDisplayConfig),
    [queue, searchQuery, fieldDisplayConfig]
  );

  const filteredHistory = useMemo(
    () => filterWithSearch(displayHistory, searchQuery, fieldDisplayConfig, true),
    [displayHistory, searchQuery, fieldDisplayConfig]
  );

  return (
    <div className="bg-white dark:bg-gray-800 border-r border-gray-200/50 dark:border-gray-700/50 w-72 flex flex-col shadow-sm transition-colors duration-200">
      {/* Header with Tabs */}
      <div className="border-b border-gray-100 dark:border-gray-700">
        <div className="flex border-b border-gray-200 dark:border-gray-700">
          <TabButton active={activeTab === 'queue'} onClick={() => setActiveTab('queue')}>
            Queue {queue.length > 0 && `(${queue.length})`}
          </TabButton>
          <TabButton active={activeTab === 'history'} onClick={() => setActiveTab('history')}>
            History {displayHistory.length > 0 && `(${displayHistory.length})`}
          </TabButton>
        </div>

        {/* History View Toggle */}
        {activeTab === 'history' && (
          <div className="px-3 py-2 border-b border-gray-100 dark:border-gray-700">
            <div className="flex items-center gap-2 bg-gray-50 dark:bg-gray-700 rounded-lg p-1">
              <ToggleButton
                active={historyViewMode === 'session'}
                onClick={() => historyViewMode === 'global' && toggleHistoryView()}
              >
                Current Session
              </ToggleButton>
              <ToggleButton
                active={historyViewMode === 'global'}
                onClick={() => historyViewMode === 'session' && toggleHistoryView()}
              >
                All Sessions
              </ToggleButton>
            </div>
          </div>
        )}

        {/* Search Bar */}
        <div className="p-3">
          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={`Search ${activeTab}...`}
              className="w-full px-3 py-2 pr-8 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent placeholder:text-gray-500 dark:placeholder:text-gray-400"
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
              <span className="text-primary-600 dark:text-primary-400 font-bold">{reviewed} / {total}</span>
            </div>
            <div className="bg-gray-100 dark:bg-gray-700 rounded-full h-2 overflow-hidden">
              <div
                className="bg-gradient-to-r from-primary-600 to-primary-500 dark:from-primary-500 dark:to-primary-400 h-full rounded-full transition-all duration-500 ease-out shadow-sm"
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
                {filteredQueue.map((item) => {
                  const globalIdx = queue.indexOf(item);
                  const isCurrent = globalIdx === currentIndex;
                  return (
                    <QueueCard
                      key={item.noteId}
                      ref={isCurrent ? currentItemRef : null}
                      item={item}
                      index={globalIdx}
                      isCurrent={isCurrent}
                      fieldDisplayConfig={fieldDisplayConfig}
                      onClick={() => {
                        goToCard(globalIdx);
                        setSelectedCard(createComparisonCardFromSuggestion(item));
                      }}
                    />
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
                {filteredHistory.map((item) => {
                  const isCurrentlyViewing = !!(selectedCard?.readonly && selectedCard?.noteId === item.noteId && selectedCard?.timestamp === item.timestamp);

                  return (
                    <HistoryCard
                      key={item.timestamp}
                      item={item}
                      isCurrentlyViewing={isCurrentlyViewing}
                      fieldDisplayConfig={fieldDisplayConfig}
                      showDeckName={historyViewMode === 'global'}
                      onClick={() => {
                        setSelectedCard(createComparisonCardFromHistory(item));
                      }}
                    />
                  );
                })}
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
            <StatCard
              icon={<CheckIcon className="w-3.5 h-3.5 text-primary-600 dark:text-primary-400" />}
              label="Done"
              value={reviewed}
            />
            <StatCard
              icon={<ClockIcon className="w-3.5 h-3.5 text-gray-400 dark:text-gray-500" />}
              label="Left"
              value={total - reviewed}
            />
          </div>
        </div>
      )}
    </div>
  );
}
