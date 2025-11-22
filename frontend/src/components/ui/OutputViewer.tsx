import { useState, useEffect } from 'react';
import { Button } from './Button';
import { CloseIcon, RefreshIcon } from './Icons';

interface OutputViewerProps {
  isOpen: boolean;
  sessionId: string;
  onClose: () => void;
}

export default function OutputViewer({ isOpen, sessionId, onClose }: OutputViewerProps) {
  const [output, setOutput] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchOutput = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`http://localhost:3001/api/sessions/${sessionId}/output`);

      if (!response.ok) {
        throw new Error('Failed to fetch output');
      }

      const content = await response.text();
      setOutput(content || 'No output available yet');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load output');
      setOutput('');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && sessionId) {
      fetchOutput();
    }
  }, [isOpen, sessionId]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-4xl w-full mx-4 max-h-[80vh] flex flex-col border border-gray-200 dark:border-gray-700">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Claude Output Log
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              Session: {sessionId}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              onClick={fetchOutput}
              variant="secondary"
              icon={<RefreshIcon className="w-4 h-4" />}
              disabled={loading}
            >
              Refresh
            </Button>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
            >
              <CloseIcon className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 bg-gray-50 dark:bg-gray-900">
          {loading ? (
            <div className="text-center py-8">
              <p className="text-gray-600 dark:text-gray-400">Loading output...</p>
            </div>
          ) : error ? (
            <div className="text-center py-8">
              <p className="text-red-600 dark:text-red-400">{error}</p>
            </div>
          ) : (
            <pre className="text-xs font-mono text-gray-900 dark:text-gray-100 whitespace-pre-wrap break-words">
              {output}
            </pre>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {output.split('\n').length} lines
          </p>
          <Button onClick={onClose} variant="primary">
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
