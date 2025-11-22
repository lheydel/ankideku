import { useState } from 'react';
import ComparisonView from './components/ComparisonView.js';
import Queue from './components/Queue.js';
import Sidebar from './components/Sidebar.js';
import Settings from './components/Settings.js';
import Notification from './components/ui/Notification.js';
import { Button } from './components/ui/Button.js';
import { LightningIcon, ChatIcon, SettingsIcon, MoonIcon, SunIcon } from './components/ui/Icons.js';
import useStore from './store/useStore.js';
import { useNotification } from './hooks/useNotification.js';
import { useCardReview } from './hooks/useCardReview.js';
import { useTheme } from './hooks/useTheme.js';

function App() {
  const { queue } = useStore();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const { notification, showNotification } = useNotification();
  const { handleAccept, handleReject, handleSkip } = useCardReview();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 transition-colors duration-200">
      {/* Notification Toast */}
      {notification && <Notification notification={notification} />}

      {/* Modern Header */}
      <header className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-md border-b border-gray-200/50 dark:border-gray-700/50 sticky top-0 z-40 transition-colors duration-200">
        <div className="px-6 py-5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-600 to-indigo-700 dark:from-indigo-500 dark:to-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <LightningIcon className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-indigo-800 dark:from-indigo-400 dark:to-indigo-600 bg-clip-text text-transparent leading-none">
                AnkiDeku
              </h1>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1 leading-none">AI-Powered Deck Revision</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Button
              onClick={toggleTheme}
              variant="secondary"
              size="md"
              icon={theme === 'dark' ? <SunIcon className="w-4 h-4" /> : <MoonIcon className="w-4 h-4" />}
            >
              <span>{theme === 'dark' ? 'Light' : 'Dark'}</span>
            </Button>
            <Button
              onClick={() => setSettingsOpen(true)}
              variant="secondary"
              size="md"
              icon={<SettingsIcon className="w-4 h-4" />}
            >
              <span>Settings</span>
            </Button>
            <Button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              variant="primary"
              size="md"
              icon={<ChatIcon className="w-4 h-4" />}
            >
              {sidebarOpen ? 'Hide' : 'Show'} Assistant
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <div className="flex h-[calc(100vh-81px)]">
        {/* Queue Sidebar - Left */}
        <Queue />

        {/* Main Content Area */}
        <div className="flex-1 overflow-y-auto">
          {queue.length === 0 ? (
            // Empty state
            <div className="min-h-full flex items-center justify-center p-8">
              <div className="text-center max-w-md">
                <div className="w-20 h-20 bg-gradient-to-br from-indigo-100 to-blue-100 dark:from-indigo-900 dark:to-blue-900 rounded-2xl flex items-center justify-center mx-auto mb-6">
                  <LightningIcon className="w-10 h-10 text-indigo-600 dark:text-indigo-400" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-3">
                  Ready to Improve Your Decks
                </h2>
                <p className="text-gray-600 dark:text-gray-400 mb-6">
                  Open the AI Assistant, select a deck, and describe what you'd like to improve.
                </p>
                <Button
                  onClick={() => setSidebarOpen(true)}
                  variant="primary"
                  icon={<ChatIcon className="w-5 h-5" />}
                >
                  Open AI Assistant
                </Button>
              </div>
            </div>
          ) : (
            // Review view
            <div className="p-8">
              <div className="max-w-6xl mx-auto space-y-6">
                <ComparisonView
                  onAccept={() => handleAccept(
                    (msg) => showNotification(msg, 'success'),
                    (msg) => showNotification(msg, 'error')
                  )}
                  onReject={() => handleReject((msg) => showNotification(msg, 'info'))}
                  onSkip={() => handleSkip((msg) => showNotification(msg, 'info'))}
                />
              </div>
            </div>
          )}
        </div>

        {/* AI Assistant Sidebar - Right */}
        <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      </div>

      {/* Settings Modal */}
      <Settings isOpen={settingsOpen} onClose={() => setSettingsOpen(false)} />
    </div>
  );
}

export default App;
