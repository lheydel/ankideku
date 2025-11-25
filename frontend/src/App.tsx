import { useState, useEffect, useCallback } from 'react';
import ComparisonView from './components/ComparisonView.js';
import Queue from './components/Queue.js';
import Sidebar from './components/Sidebar.js';
import Settings from './components/Settings.js';
import Notification from './components/ui/Notification.js';
import ConfirmDialog from './components/ui/ConfirmDialog.js';
import OutputViewer from './components/ui/OutputViewer.js';
import { ConflictDialog } from './components/ui/ConflictDialog.js';
import { Button } from './components/ui/Button.js';
import { LightningIcon, ChatIcon, SettingsIcon, MoonIcon, SunIcon, Logo } from './components/ui/Icons.js';
import { SessionCard } from './components/SessionCard.js';
import { EmptyState } from './components/EmptyState.js';
import useStore from './store/useStore.js';
import { useNotification } from './hooks/useNotification.js';
import { useCardReview } from './hooks/useCardReview.js';
import { useTheme } from './hooks/useTheme.js';
import { useSessionManagement } from './hooks/useSessionManagement.js';
import { LAYOUT } from './constants/layout.js';

function App() {
  const { setQueue, currentSession, currentSessionData, setSelectedCard } = useStore();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [sessionToDelete, setSessionToDelete] = useState<string | null>(null);
  const [outputViewerSession, setOutputViewerSession] = useState<string | null>(null);

  const { notification, showNotification } = useNotification();
  const {
    handleAccept,
    handleReject,
    handleSkip,
    conflictDetected,
    handleViewNewVersion,
    handleCancelConflict
  } = useCardReview();
  const { theme, toggleTheme } = useTheme();
  const { sessions, loadSession, listSessions, deleteSession, clearSession } = useSessionManagement();

  // Load sessions on mount
  useEffect(() => {
    listSessions().catch(console.error);
  }, []);

  const handleDeleteSession = useCallback(async () => {
    if (!sessionToDelete) return;

    try {
      await deleteSession(sessionToDelete);
      showNotification('Session deleted successfully', 'success');
      setSessionToDelete(null);
    } catch (error) {
      showNotification('Failed to delete session', 'error');
    }
  }, [sessionToDelete, deleteSession, showNotification]);

  const handleLoadSession = useCallback(async (sessionId: string) => {
    try {
      const sessionData = await loadSession(sessionId);
      setQueue(sessionData.suggestions);
      setSidebarOpen(true);

      // Automatically select the first card if available
      if (sessionData.suggestions.length > 0) {
        const firstSuggestion = sessionData.suggestions[0];
        setSelectedCard({
          noteId: firstSuggestion.noteId,
          original: firstSuggestion.original,
          changes: firstSuggestion.changes,
          reasoning: firstSuggestion.reasoning,
          readonly: false,
          editedChanges: firstSuggestion.editedChanges
        });
      }
    } catch (error) {
      showNotification('Failed to load session', 'error');
    }
  }, [loadSession, setQueue, setSelectedCard, showNotification]);

  const handleClearSession = useCallback(() => {
    setQueue([]);
    clearSession();
    setSelectedCard(null);
  }, [setQueue, clearSession, setSelectedCard]);

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
      <div className="flex" style={{ height: `calc(100vh - ${LAYOUT.HEADER_HEIGHT}px)` }}>
        {/* Queue Sidebar - Left */}
        <Queue />

        {/* Main Content Area */}
        <div className="flex-1 overflow-y-auto">
          {!currentSession ? (
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
                  <EmptyState
                    icon={<LightningIcon className="w-10 h-10 text-primary-600 dark:text-primary-400" />}
                    title="No Sessions Yet"
                    description="Use the AI assistant to create your first session"
                  />
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {sessions.map((session) => (
                      <SessionCard
                        key={session.sessionId}
                        session={session}
                        onLoad={() => handleLoadSession(session.sessionId)}
                        onDelete={() => setSessionToDelete(session.sessionId)}
                        onViewOutput={() => setOutputViewerSession(session.sessionId)}
                      />
                    ))}
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
                  onBackToSessions={handleClearSession}
                  onAccept={(editedChanges) => handleAccept(
                    (msg) => showNotification(msg, 'success'),
                    (msg) => showNotification(msg, 'error'),
                    editedChanges
                  )}
                  onReject={(editedChanges) => handleReject((msg) => showNotification(msg, 'info'), editedChanges)}
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
          onNewSession={handleClearSession}
          onViewLogs={currentSession ? () => setOutputViewerSession(currentSession) : undefined}
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

      {/* Conflict Detection Dialog */}
      <ConflictDialog
        isOpen={conflictDetected}
        onCancel={handleCancelConflict}
        onViewNewVersion={handleViewNewVersion}
      />
    </div>
  );
}

export default App;
