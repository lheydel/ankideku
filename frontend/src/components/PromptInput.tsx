import { useState } from 'react';
import useStore from '../store/useStore.js';
import { ChatIcon, ClockIcon, PlusIcon, LightningIcon, InfoIcon, ChevronRightIcon } from './ui/Icons.js';
import { Button } from './ui/Button.js';

interface PromptInputProps {
  onGenerate: () => void;
}

export default function PromptInput({ onGenerate }: PromptInputProps) {
  const { prompt, setPrompt, promptHistory, selectedDeck } = useStore();
  const [showHistory, setShowHistory] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (prompt.trim() && selectedDeck) {
      onGenerate();
    }
  };

  const selectFromHistory = (historicalPrompt: string) => {
    setPrompt(historicalPrompt);
    setShowHistory(false);
  };

  const examplePrompts = [
    'Fix spelling and grammar errors',
    'Add example sentences where missing',
    'Improve explanations for clarity',
    'Check if translations are accurate',
    'Standardize formatting across all fields',
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <label className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <ChatIcon className="w-4 h-4 text-indigo-600" />
          AI Instructions
        </label>
        {promptHistory.length > 0 && (
          <Button
            onClick={() => setShowHistory(!showHistory)}
            variant="text"
            size="sm"
            icon={<ClockIcon className="w-3.5 h-3.5" />}
            className="text-xs"
          >
            {showHistory ? 'Hide' : 'Show'} History
          </Button>
        )}
      </div>

      {showHistory && promptHistory.length > 0 && (
        <div className="p-4 bg-gray-50 border border-gray-200 rounded-xl space-y-2 fade-in">
          <p className="text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2">Recent Prompts</p>
          <div className="space-y-2">
            {promptHistory.map((p, i) => (
              <button
                key={i}
                onClick={() => selectFromHistory(p)}
                className="block w-full text-left px-4 py-2.5 text-sm bg-white border border-gray-200 rounded-lg hover:border-indigo-300 hover:bg-indigo-50 transition-all group"
              >
                <div className="flex items-center gap-2">
                  <PlusIcon className="w-4 h-4 text-gray-400 group-hover:text-indigo-600 transition" />
                  <span className="text-gray-700 group-hover:text-gray-900">{p}</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="relative">
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="Describe what you want to improve... (e.g., 'Fix spelling errors', 'Add pronunciation')"
            className="input resize-none pr-12"
            rows={4}
          />
          <div className="absolute bottom-3 right-3 text-xs text-gray-400">
            {prompt.length} chars
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button
            type="submit"
            disabled={!prompt.trim() || !selectedDeck}
            variant="primary"
            icon={<LightningIcon className="w-5 h-5" />}
            className="flex-1 sm:flex-none"
          >
            Generate Suggestions
          </Button>

          {!selectedDeck && (
            <span className="text-sm text-gray-500 flex items-center gap-1.5">
              <InfoIcon className="w-4 h-4" />
              Select a deck first
            </span>
          )}
        </div>
      </form>

      <details className="group">
        <summary className="cursor-pointer text-sm text-gray-600 hover:text-gray-900 flex items-center gap-2 font-medium transition">
          <ChevronRightIcon className="w-4 h-4 group-open:rotate-90 transition-transform" />
          Example prompts
        </summary>
        <div className="mt-3 grid gap-2 pl-6">
          {examplePrompts.map((example, i) => (
            <button
              key={i}
              onClick={() => setPrompt(example)}
              className="text-left px-4 py-2.5 text-sm bg-white border border-gray-200 rounded-lg hover:border-indigo-300 hover:bg-indigo-50 transition-all group/item"
            >
              <div className="flex items-center gap-2">
                <ChevronRightIcon className="w-3.5 h-3.5 text-indigo-600" />
                <span className="text-gray-700 group-hover/item:text-gray-900">{example}</span>
              </div>
            </button>
          ))}
        </div>
      </details>
    </div>
  );
}
