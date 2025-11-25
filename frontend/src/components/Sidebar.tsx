import { useState, useRef } from 'react';
import useStore, { selectIsProcessing, selectSessionProgress } from '../store/useStore';
import { DeckIcon, ChevronDownIcon, LightningIcon, CloseIcon, ChatIcon, SyncIcon } from './ui/Icons';
import { useCardGeneration } from '../hooks/useCardGeneration';
import { useSessionManagement } from '../hooks/useSessionManagement';
import { useSidebarChat } from '../hooks/useSidebarChat';
import { useAnkiConnection } from '../hooks/useAnkiConnection';
import { Button } from './ui/Button';
import { ChatMessage } from './sidebar/ChatMessage';
import { LAYOUT } from '../constants/layout';
import type { SessionData } from '../types';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  currentSessionData: SessionData | null;
  onNewSession: () => void;
  onViewLogs?: () => void;
}

export default function Sidebar({ isOpen, onClose, currentSessionData, onNewSession, onViewLogs }: SidebarProps) {
  const {
    decks,
    selectedDeck,
    selectDeck,
    setPrompt,
    forceSync,
    setForceSync,
  } = useStore();

  const processing = useStore(selectIsProcessing);
  const { processed: processedCount, total: totalCount, suggestionsCount } = useStore(selectSessionProgress);

  const inputRef = useRef<HTMLTextAreaElement>(null);
  const [input, setInput] = useState('');

  const deckEntries = Object.entries(decks);

  // Hooks
  const { generateSuggestions, currentSession } = useCardGeneration();
  const { cancelSession } = useSessionManagement();

  const {
    messages,
    addMessage,
    resetToWelcome,
    resetProgressTracking,
  } = useSidebarChat({
    currentSessionData,
    processing,
    suggestionsCount,
    onSelectDeck: selectDeck,
  });

  const { syncing, syncDeck } = useAnkiConnection(addMessage);

  const handleCancelSession = async () => {
    if (!currentSession) return;

    try {
      await cancelSession(currentSession);
      addMessage('system', 'Session cancelled');
      resetProgressTracking();
    } catch {
      addMessage('system', 'Failed to cancel session');
    }
  };

  const handleNewSession = () => {
    onNewSession();
    resetToWelcome();
    setInput('');
    setTimeout(() => inputRef.current?.focus(), 0);
  };

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMessage = input.trim();
    setInput('');
    addMessage('user', userMessage);

    if (!selectedDeck) {
      addMessage('system', 'Please select a deck first.');
      return;
    }

    if (forceSync) {
      addMessage('system', `⚡ Force sync enabled - will sync "${selectedDeck}" before processing`);
    }

    setPrompt(userMessage);
    resetProgressTracking();

    await generateSuggestions(
      userMessage,
      (message) => addMessage('assistant', message),
      (message) => {
        addMessage('system', `Error: ${message}`);
        resetProgressTracking();
      }
    );
  };

  if (!isOpen) return null;

  return (
    <div
      className="bg-white dark:bg-gray-800 border-l border-gray-200 dark:border-gray-700 shadow-lg flex flex-col transition-colors duration-200"
      style={{ width: LAYOUT.SIDEBAR_WIDTH }}
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between bg-gradient-to-r from-primary-50 to-blue-50 dark:from-primary-900/30 dark:to-blue-900/30">
        <div className="flex items-center gap-2">
          <ChatIcon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          <h2 className="font-bold text-gray-900 dark:text-gray-100">AI Assistant</h2>
        </div>
        <div className="flex items-center gap-2">
          {currentSessionData && onViewLogs && (
            <Button onClick={onViewLogs} variant="secondary" size="sm">
              View Logs
            </Button>
          )}
          {currentSessionData && (
            <Button
              onClick={handleNewSession}
              variant="secondary"
              size="sm"
              icon={<LightningIcon className="w-4 h-4" />}
            >
              New Session
            </Button>
          )}
          <Button
            onClick={onClose}
            variant="ghost"
            icon={<CloseIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />}
            className="hover:bg-white/60 dark:hover:bg-gray-700/60"
          />
        </div>
      </div>

      {/* Deck Selector */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
        <label className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-2 flex items-center gap-2">
          <DeckIcon className="w-3.5 h-3.5 text-primary-600 dark:text-primary-400" />
          Deck
        </label>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <select
              value={selectedDeck || ''}
              onChange={(e) => selectDeck(e.target.value)}
              className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent appearance-none pr-10 cursor-pointer"
            >
              <option value="">Choose a deck...</option>
              {deckEntries.map(([name, id]) => (
                <option key={id} value={name}>
                  {name}
                </option>
              ))}
            </select>
            <ChevronDownIcon className="w-4 h-4 text-gray-400 dark:text-gray-500 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>
          <Button
            onClick={syncDeck}
            variant="secondary"
            size="sm"
            icon={<SyncIcon className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />}
            disabled={!selectedDeck || syncing}
            className="shrink-0"
          />
        </div>
      </div>

      {/* Processing Indicator */}
      {processing && (
        <div className="p-4 bg-primary-50 dark:bg-primary-900/30 border-b border-primary-100 dark:border-primary-800">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-primary-600 dark:bg-primary-400 rounded-full animate-pulse" />
              <span className="text-sm text-primary-700 dark:text-primary-300 font-medium">
                {totalCount > 0
                  ? `Processing ${processedCount} / ${totalCount} cards`
                  : 'AI Processing...'}
              </span>
            </div>
            <Button
              onClick={handleCancelSession}
              variant="ghost"
              size="sm"
              className="text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
            >
              Cancel
            </Button>
          </div>
          {totalCount > 0 && (
            <div className="w-full bg-primary-200 dark:bg-primary-800 rounded-full h-2 overflow-hidden">
              <div
                className="bg-primary-600 dark:bg-primary-400 h-full rounded-full transition-all duration-300 ease-out"
                style={{ width: `${Math.round((processedCount / totalCount) * 100)}%` }}
              />
            </div>
          )}
        </div>
      )}

      {/* Chat Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50 dark:bg-gray-900">
        {messages.map((message) => (
          <ChatMessage key={message.id} message={message} />
        ))}
      </div>

      {/* Chat Input - Only show when no active session */}
      {!currentSessionData && (
        <div className="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
          <div className="mb-3 flex items-center gap-2">
            <input
              type="checkbox"
              id="force-sync"
              checked={forceSync}
              onChange={(e) => setForceSync(e.target.checked)}
              disabled={syncing}
              className="w-4 h-4 text-primary-600 dark:text-primary-500 bg-gray-50 dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded focus:ring-2 focus:ring-primary-500 dark:focus:ring-primary-400 focus:ring-offset-0 cursor-pointer accent-primary-600 dark:accent-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
            />
            <label
              htmlFor="force-sync"
              className={`text-sm text-gray-700 dark:text-gray-300 select-none ${syncing ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
            >
              Sync deck before processing
            </label>
          </div>

          <div className="flex gap-2 items-end">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey && !syncing) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder={syncing ? 'Syncing deck...' : 'Describe what you want to improve... (Shift+Enter for new line)'}
              disabled={syncing}
              className="flex-1 px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none placeholder:text-gray-500 dark:placeholder:text-gray-400 disabled:opacity-50 disabled:cursor-not-allowed"
              rows={3}
            />
          </div>
          {!selectedDeck && (
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">Select a deck to start</p>
          )}
        </div>
      )}
    </div>
  );
}
