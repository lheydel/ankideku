import { useState } from 'react';
import useStore from '../store/useStore.js';

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
        <h2 className="text-lg font-semibold text-gray-900">AI Prompt</h2>
        {promptHistory.length > 0 && (
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="text-sm text-primary hover:text-blue-700"
          >
            {showHistory ? 'Hide' : 'Show'} History
          </button>
        )}
      </div>

      {showHistory && promptHistory.length > 0 && (
        <div className="p-3 bg-gray-50 rounded-lg space-y-2">
          <p className="text-xs font-medium text-gray-700 uppercase">Recent Prompts</p>
          {promptHistory.map((p, i) => (
            <button
              key={i}
              onClick={() => selectFromHistory(p)}
              className="block w-full text-left px-3 py-2 text-sm bg-white border border-gray-200 rounded hover:bg-gray-50 transition"
            >
              {p}
            </button>
          ))}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3">
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Enter your instructions for AI... (e.g., 'Fix spelling errors')"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
          rows={3}
        />

        <div className="flex items-center justify-between">
          <button
            type="submit"
            disabled={!prompt.trim() || !selectedDeck}
            className="px-6 py-3 bg-primary text-white rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition"
          >
            Generate Suggestions
          </button>

          {!selectedDeck && (
            <span className="text-sm text-gray-500">Select a deck first</span>
          )}
        </div>
      </form>

      <details className="text-sm">
        <summary className="cursor-pointer text-gray-600 hover:text-gray-900">
          Example prompts
        </summary>
        <ul className="mt-2 space-y-1 pl-4">
          {examplePrompts.map((example, i) => (
            <li key={i}>
              <button
                onClick={() => setPrompt(example)}
                className="text-primary hover:underline text-left"
              >
                {example}
              </button>
            </li>
          ))}
        </ul>
      </details>
    </div>
  );
}
