import { useState, useEffect } from 'react';
import ComparisonView from './components/ComparisonView.js';
import Queue from './components/Queue.js';
import Sidebar from './components/Sidebar.js';
import Settings from './components/Settings.js';
import Notification from './components/ui/Notification.js';
import ConfirmDialog from './components/ui/ConfirmDialog.js';
import OutputViewer from './components/ui/OutputViewer.js';
import { Button } from './components/ui/Button.js';
import { Breadcrumb } from './components/ui/Breadcrumb.js';
import { LightningIcon, ChatIcon, SettingsIcon, MoonIcon, SunIcon, ClockIcon, TrashIcon, Logo } from './components/ui/Icons.js';
import useStore from './store/useStore.js';
import { useNotification } from './hooks/useNotification.js';
import { useCardReview } from './hooks/useCardReview.js';
import { useTheme } from './hooks/useTheme.js';
import { useSessionManagement } from './hooks/useSessionManagement.js';
import { SessionState } from './types/index.js';

// Helper function to get state badge styling
function getStateBadge(state: SessionState) {
  const badges = {
    [SessionState.PENDING]: {
      label: 'Pending',
      className: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600'
    },
    [SessionState.RUNNING]: {
      label: 'Running',
      className: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-300 dark:border-blue-700'
    },
    [SessionState.COMPLETED]: {
      label: 'Completed',
      className: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-300 dark:border-green-700'
    },
    [SessionState.FAILED]: {
      label: 'Failed',
      className: 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-700'
    },
    [SessionState.CANCELLED]: {
      label: 'Cancelled',
      className: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 border border-amber-300 dark:border-amber-700'
    }
  };

  const badge = badges[state];

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-semibold ${badge.className}`}>
      {badge.label}
    </span>
  );
}

function App() {
  const { queue, setQueue, currentSessionData } = useStore();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [sessionToDelete, setSessionToDelete] = useState<string | null>(null);
  const [outputViewerSession, setOutputViewerSession] = useState<string | null>(null);

  const { notification, showNotification } = useNotification();
  const { handleAccept, handleReject, handleSkip } = useCardReview();
  const { theme, toggleTheme } = useTheme();
  const { sessions, loadSession, listSessions, deleteSession, clearSession } = useSessionManagement();

  // Load sessions on mount
  useEffect(() => {
    listSessions().catch(console.error);
  }, []);

  const handleDeleteSession = async () => {
    if (!sessionToDelete) return;

    try {
      await deleteSession(sessionToDelete);
      showNotification('Session deleted successfully', 'success');
      setSessionToDelete(null);
    } catch (error) {
      showNotification('Failed to delete session', 'error');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 transition-colors duration-200">
      {/* Notification Toast */}
      {notification && <Notification notification={notification} />}

      {/* Modern Header */}
      <header className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-md border-b border-gray-200/50 dark:border-gray-700/50 sticky top-0 z-40 transition-colors duration-200">
        <div className="px-6 py-5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Logo size={48} variant="icon" theme={theme} />
            <div>
              <h1 className="text-2xl font-bold bg-gradient-to-r from-primary-600 to-primary-800 dark:from-primary-400 dark:to-primary-600 bg-clip-text text-transparent leading-none">
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
          {queue.length === 0 && (!currentSessionData?.state || (currentSessionData.state.state !== SessionState.PENDING && currentSessionData.state.state !== SessionState.RUNNING)) ? (
            // Session selector view
            <div className="min-h-full p-8">
              <div className="max-w-4xl mx-auto">
                <div className="mb-8">
                  <h2 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">
                    AI Sessions
                  </h2>
                  <p className="text-gray-600 dark:text-gray-400">
                    Select a previous session to review
                  </p>
                </div>

                {sessions.length === 0 ? (
                  <div className="text-center py-16">
                    <div className="w-20 h-20 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-2xl flex items-center justify-center mx-auto mb-6">
                      <LightningIcon className="w-10 h-10 text-primary-600 dark:text-primary-400" />
                    </div>
                    <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-3">
                      No Sessions Yet
                    </h3>
                    <p className="text-gray-600 dark:text-gray-400">
                      Use the AI assistant to create your first session
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {sessions.map((session) => {
                      // Parse ISO timestamp
                      const date = new Date(session.timestamp);

                      return (
                        <div
                          key={session.sessionId}
                          className="group relative bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-6 hover:border-primary-300 dark:hover:border-primary-600 hover:shadow-lg transition-all duration-200"
                        >
                          {/* Delete Button */}
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setSessionToDelete(session.sessionId);
                            }}
                            className="absolute top-4 right-4 p-2 rounded-lg text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors opacity-0 group-hover:opacity-100"
                            title="Delete session"
                          >
                            <TrashIcon className="w-4 h-4" />
                          </button>

                          {/* Session Card Content */}
                          <button
                            onClick={async () => {
                              try {
                                const sessionData = await loadSession(session.sessionId);
                                setQueue(sessionData.suggestions);
                                setSidebarOpen(true); // Open sidebar to show chat history
                              } catch (error) {
                                showNotification('Failed to load session', 'error');
                              }
                            }}
                            className="w-full text-left"
                          >
                            <div className="flex items-start justify-between mb-4">
                              <div className="w-12 h-12 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                                <ClockIcon className="w-6 h-6 text-primary-600 dark:text-primary-400" />
                              </div>
                              {session.state && (
                                <div>
                                  {getStateBadge(session.state.state)}
                                </div>
                              )}
                            </div>

                            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                              {session.deckName}
                            </h3>

                            <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
                              {date.toLocaleDateString('en-US', {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric'
                              })} • {date.toLocaleTimeString('en-US', {
                                hour: '2-digit',
                                minute: '2-digit',
                                hour12: false
                              })}
                            </p>

                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2 text-xs text-primary-600 dark:text-primary-400 font-medium">
                                <span>Load Session</span>
                                <span className="group-hover:translate-x-1 transition-transform">→</span>
                              </div>
                              <span
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setOutputViewerSession(session.sessionId);
                                }}
                                className="text-xs text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 underline cursor-pointer"
                              >
                                View Output
                              </span>
                            </div>
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          ) : (
            // Review view
            <div className="p-8">
              <div className="max-w-6xl mx-auto space-y-6">
                <ComparisonView
                  currentSessionData={currentSessionData}
                  onBackToSessions={() => {
                    setQueue([]);
                    clearSession();
                  }}
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
        <Sidebar
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          currentSessionData={currentSessionData}
          onNewSession={() => {
            clearSession();
            setQueue([]);
          }}
        />
      </div>

      {/* Settings Modal */}
      <Settings isOpen={settingsOpen} onClose={() => setSettingsOpen(false)} />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!sessionToDelete}
        title="Delete Session"
        message="Are you sure you want to delete this session? This will permanently remove the session and all its suggestions from the database."
        confirmText="Delete"
        cancelText="Cancel"
        variant="danger"
        onConfirm={handleDeleteSession}
        onCancel={() => setSessionToDelete(null)}
      />

      {/* Output Viewer */}
      {outputViewerSession && (
        <OutputViewer
          isOpen={!!outputViewerSession}
          sessionId={outputViewerSession}
          onClose={() => setOutputViewerSession(null)}
        />
      )}
    </div>
  );
}

export default App;
