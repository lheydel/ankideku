import { DeckIcon, BrushIcon, ClipboardIcon } from '../ui/Icons';

interface HeaderCardProps {
  deckName?: string;
  modelName: string;
  currentIndex: number;
  queueLength: number;
  copySuccess: boolean;
  onCopy: () => void;
}

export function HeaderCard({ deckName, modelName, currentIndex, queueLength, copySuccess, onCopy }: HeaderCardProps) {
  return (
    <div className="card p-5 bg-gradient-to-r from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-sm">
            <DeckIcon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
            <span className="font-semibold text-gray-700 dark:text-gray-300">{deckName || 'Unknown'}</span>
          </div>
          <div className="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
          <div className="flex items-center gap-2 text-sm">
            <BrushIcon className="w-4 h-4 text-gray-400 dark:text-gray-500" />
            <span className="text-gray-600 dark:text-gray-400">{modelName}</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={onCopy}
            className={`px-3 py-1.5 text-sm font-medium border rounded-md transition-colors flex items-center gap-2 ${
              copySuccess
                ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 border-green-300 dark:border-green-700'
                : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-650'
            }`}
            title="Copy to clipboard for external LLM review"
          >
            <ClipboardIcon className="w-4 h-4" />
            {copySuccess ? 'Copied!' : 'Copy Suggestion'}
          </button>
          <div className="flex items-center gap-2 bg-primary-100 dark:bg-primary-900/30 px-3 py-1.5 rounded-full">
            <ClipboardIcon className="w-4 h-4 text-primary-700 dark:text-primary-400" />
            <span className="text-sm font-bold text-primary-900 dark:text-primary-200">
              {currentIndex + 1} / {queueLength}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
