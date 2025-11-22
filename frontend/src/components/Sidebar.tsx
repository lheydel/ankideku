import { useState, useEffect } from 'react';
import { ankiApi } from '../services/api.js';
import useStore from '../store/useStore.js';
import { DeckIcon, ChevronDownIcon, LightningIcon, CloseIcon, ChatIcon } from './ui/Icons.js';
import { useCardGeneration } from '../hooks/useCardGeneration.js';
import { Button } from './ui/Button.js';

interface Message {
  id: string;
  type: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
}

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const {
    decks,
    selectedDeck,
    selectDeck,
    setDecks,
    setAnkiConnected,
    processing,
    processedCount,
    totalCount,
    setFieldDisplayConfig,
    setPrompt,
  } = useStore();

  const [messages, setMessages] = useState<Message[]>([
    {
      id: '0',
      type: 'system',
      content: 'Welcome! Select a deck and describe what you want to improve.',
      timestamp: new Date(),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  const deckEntries = Object.entries(decks);

  // Initialize hook for card generation
  const { generateSuggestions } = useCardGeneration();

  // Load decks and settings on mount
  useEffect(() => {
    loadDecks();
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const settings = await ankiApi.getSettings();
      if (settings.fieldDisplayConfig) {
        setFieldDisplayConfig(settings.fieldDisplayConfig);
      }
    } catch (error) {
      console.error('Failed to load settings:', error);
    }
  };

  const loadDecks = async () => {
    try {
      setLoading(true);
      const { connected } = await ankiApi.ping();
      setAnkiConnected(connected);

      if (!connected) {
        addMessage('system', 'AnkiConnect is not running. Please start Anki.');
        return;
      }

      const deckData = await ankiApi.getDecks();
      setDecks(deckData);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to connect to AnkiConnect';
      addMessage('system', errorMessage);
      setAnkiConnected(false);
    } finally {
      setLoading(false);
    }
  };

  const addMessage = (type: 'user' | 'assistant' | 'system', content: string) => {
    setMessages((prev) => [
        ...prev,
      {
        id: prev.length.toString(),
        type,
        content,
        timestamp: new Date(),
      } as Message
    ]);
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

    // Update the prompt in the store and generate suggestions
    setPrompt(userMessage);
    addMessage('assistant', `Generating suggestions for "${selectedDeck}"...`);

    await generateSuggestions(
      userMessage,
      (message) => {
        addMessage('assistant', message);
      },
      (message) => {
        addMessage('system', `Error: ${message}`);
      }
    );
  };

  if (!isOpen) return null;

  return (
    <div
      className="bg-white dark:bg-gray-800 border-l border-gray-200 dark:border-gray-700 shadow-lg flex flex-col transition-colors duration-200"
      style={{ width: '28rem' }}
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between bg-gradient-to-r from-indigo-50 to-blue-50 dark:from-indigo-900/30 dark:to-blue-900/30">
        <div className="flex items-center gap-2">
          <ChatIcon className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
          <h2 className="font-bold text-gray-900 dark:text-gray-100">AI Assistant</h2>
        </div>
        <Button
          onClick={onClose}
          variant="ghost"
          icon={<CloseIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />}
          className="hover:bg-white/60 dark:hover:bg-gray-700/60"
        />
      </div>

      {/* Deck Selector */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
        <label className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-2 flex items-center gap-2">
          <DeckIcon className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
          Deck
        </label>
        <div className="relative">
          <select
            value={selectedDeck || ''}
            onChange={(e) => selectDeck(e.target.value)}
            className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent appearance-none pr-10 cursor-pointer"
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
      </div>

      {/* Progress Info */}
      {processing && (
        <div className="p-4 bg-indigo-50 dark:bg-indigo-900/30 border-b border-indigo-100 dark:border-indigo-800">
          <div className="flex items-center justify-between text-xs mb-2">
            <span className="text-indigo-700 dark:text-indigo-300 font-medium">Processing cards...</span>
            <span className="text-indigo-900 dark:text-indigo-200 font-bold">
              {processedCount} / {totalCount}
            </span>
          </div>
          <div className="bg-white/60 dark:bg-gray-700/60 rounded-full h-2 overflow-hidden">
            <div
              className="bg-gradient-to-r from-indigo-600 to-indigo-500 dark:from-indigo-500 dark:to-indigo-400 h-full transition-all duration-500"
              style={{ width: `${totalCount > 0 ? (processedCount / totalCount) * 100 : 0}%` }}
            />
          </div>
        </div>
      )}

      {/* Chat Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50 dark:bg-gray-900">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[80%] rounded-lg px-4 py-2 ${
                message.type === 'user'
                  ? 'bg-indigo-600 dark:bg-indigo-500 text-white'
                  : message.type === 'system'
                  ? 'bg-amber-50 dark:bg-amber-900/30 text-amber-900 dark:text-amber-200 border border-amber-200 dark:border-amber-700'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100'
              }`}
            >
              <p className="text-sm whitespace-pre-wrap">{message.content}</p>
              <p
                className={`text-xs mt-1 ${
                  message.type === 'user'
                    ? 'text-indigo-200 dark:text-indigo-300'
                    : message.type === 'system'
                    ? 'text-amber-600 dark:text-amber-400'
                    : 'text-gray-500 dark:text-gray-400'
                }`}
              >
                {message.timestamp.toLocaleTimeString()}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Chat Input */}
      <div className="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
        <div className="flex gap-2 items-end">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder="Describe what you want to improve... (Shift+Enter for new line)"
            className="flex-1 px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none placeholder:text-gray-500 dark:placeholder:text-gray-400"
            rows={3}
          />
        </div>
        {!selectedDeck && (
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">Select a deck to start</p>
        )}
      </div>
    </div>
  );
}
