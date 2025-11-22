import { useMemo } from 'react';
import useStore from '../store/useStore.js';
import DiffMatchPatch from 'diff-match-patch';
import { WarningIcon, CheckIcon, ArchiveIcon, DeckIcon, BrushIcon, ClipboardIcon, BulbIcon, ClockIcon, CloseIcon, ArrowRightIcon, KeyboardIcon } from './ui/Icons.js';
import { Button } from './ui/Button.js';

const dmp = new DiffMatchPatch();

interface CardFieldProps {
  fieldName: string;
  value: string;
  isChanged: boolean;
  showDiff?: boolean;
  diffs?: any[];
}

function CardField({ fieldName, value, isChanged, showDiff, diffs }: CardFieldProps) {
  return (
    <div className={`py-3 border-b border-gray-100 last:border-b-0 ${isChanged ? 'bg-yellow-50/30' : ''}`}>
      <div className="flex items-center gap-2 mb-1.5">
        {isChanged ? (
          <WarningIcon className="w-3.5 h-3.5 text-amber-600" />
        ) : (
          <CheckIcon className="w-3.5 h-3.5 text-gray-400" />
        )}
        <span className={`text-xs font-semibold uppercase tracking-wide leading-none ${isChanged ? 'text-amber-700' : 'text-gray-500'}`}>
          {fieldName}
        </span>
      </div>
      <div className="text-sm text-gray-900 whitespace-pre-wrap leading-relaxed">
        {showDiff && diffs ? (
          diffs.map((diff, i) => {
            const [operation, text] = diff;
            if (showDiff === 'original') {
              if (operation === 1) return null;
              if (operation === -1) {
                return (
                  <span key={i} className="bg-red-100 text-red-900 px-0.5 rounded line-through">
                    {text}
                  </span>
                );
              }
              return <span key={i}>{text}</span>;
            } else {
              if (operation === -1) return null;
              if (operation === 1) {
                return (
                  <span key={i} className="bg-green-200 text-green-900 px-0.5 rounded font-medium">
                    {text}
                  </span>
                );
              }
              return <span key={i}>{text}</span>;
            }
          })
        ) : (
          value || <span className="text-gray-400 italic text-xs">(empty)</span>
        )}
      </div>
    </div>
  );
}

interface ComparisonViewProps {
  onAccept: () => void;
  onReject: () => void;
  onSkip: () => void;
}

export default function ComparisonView({ onAccept, onReject, onSkip }: ComparisonViewProps) {
  const { getCurrentCard, currentIndex, queue } = useStore();

  const card = getCurrentCard();

  if (!card) {
    return (
      <div className="card text-center py-16">
        <ArchiveIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <p className="text-gray-500 font-medium">No card to review</p>
      </div>
    );
  }

  const { original, changes, reasoning } = card;
  const changedFields = Object.keys(changes || {});

  return (
    <div className="space-y-6">
      {/* Header Card with Metadata */}
      <div className="card p-5 bg-gradient-to-r from-white to-gray-50">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 text-sm">
              <DeckIcon className="w-4 h-4 text-indigo-600" />
              <span className="font-semibold text-gray-700">{original.deckName || 'Unknown'}</span>
            </div>
            <div className="h-4 w-px bg-gray-300"></div>
            <div className="flex items-center gap-2 text-sm">
              <BrushIcon className="w-4 h-4 text-gray-400" />
              <span className="text-gray-600">{original.modelName}</span>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-indigo-100 px-3 py-1.5 rounded-full">
            <ClipboardIcon className="w-4 h-4 text-indigo-700" />
            <span className="text-sm font-bold text-indigo-900">
              {currentIndex + 1} / {queue.length}
            </span>
          </div>
        </div>
      </div>

      {/* AI Reasoning */}
      {reasoning && (
        <div className="card p-5 bg-gradient-to-br from-indigo-50 via-blue-50 to-indigo-50 border-indigo-200">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-indigo-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <BulbIcon className="w-5 h-5 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold text-indigo-900 mb-1 leading-none">AI Reasoning</p>
              <p className="text-sm text-indigo-800 leading-relaxed">{reasoning}</p>
            </div>
          </div>
        </div>
      )}

      {/* Two Separate Cards Side by Side */}
      <div className="grid grid-cols-2 gap-6">
        {/* Original Card */}
        <div className="card overflow-hidden">
          <div className="bg-gradient-to-r from-gray-100 to-gray-50 px-5 py-3 border-b border-gray-200">
            <div className="flex items-center gap-2">
              <ClockIcon className="w-5 h-5 text-gray-600" />
              <span className="font-semibold text-gray-900">Original Card</span>
            </div>
          </div>
          <div className="px-5">
            {Object.entries(original.fields)
              .sort(([, a], [, b]) => a.order - b.order)
              .map(([fieldName, fieldData]) => {
                const isChanged = changedFields.includes(fieldName);
                const diffs = isChanged ? dmp.diff_main(fieldData.value || '', changes[fieldName] || '') : [];
                return (
                  <CardField
                    key={fieldName}
                    fieldName={fieldName}
                    value={fieldData.value}
                    isChanged={isChanged}
                    showDiff={isChanged ? 'original' : undefined}
                    diffs={diffs}
                  />
                );
              })}
          </div>
        </div>

        {/* Suggested Card */}
        <div className="card overflow-hidden border-l-4 border-l-green-500">
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 px-5 py-3 border-b border-green-200">
            <div className="flex items-center gap-2">
              <CheckIcon className="w-5 h-5 text-green-600" />
              <span className="font-semibold text-green-900">Suggested Card</span>
            </div>
          </div>
          <div className="px-5">
            {Object.entries(original.fields)
              .sort(([, a], [, b]) => a.order - b.order)
              .map(([fieldName, fieldData]) => {
                const isChanged = changedFields.includes(fieldName);
                const suggestedValue = changes[fieldName] || fieldData.value;
                const diffs = isChanged ? dmp.diff_main(fieldData.value || '', suggestedValue || '') : [];
                return (
                  <CardField
                    key={fieldName}
                    fieldName={fieldName}
                    value={suggestedValue}
                    isChanged={isChanged}
                    showDiff={isChanged ? 'suggested' : undefined}
                    diffs={diffs}
                  />
                );
              })}
          </div>
        </div>
      </div>

      {changedFields.length === 0 && (
        <div className="card text-center py-12">
          <CheckIcon className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 font-medium">No changes detected</p>
        </div>
      )}

      {/* Action Buttons */}
      <div className="card p-6 bg-gradient-to-b from-white to-gray-50">
        <div className="flex items-center justify-between gap-4">
          <div className="flex gap-3">
            <Button
              onClick={onReject}
              variant="danger"
              icon={<CloseIcon className="w-5 h-5" />}
            >
              Reject
              <kbd className="hidden sm:inline-block ml-2 px-2 py-0.5 bg-red-700/20 text-red-200 rounded text-xs">R</kbd>
            </Button>

            <Button
              onClick={onSkip}
              variant="warning"
              icon={<ArrowRightIcon className="w-5 h-5" />}
            >
              Skip
              <kbd className="hidden sm:inline-block ml-2 px-2 py-0.5 bg-amber-600/30 text-amber-100 rounded text-xs">S</kbd>
            </Button>
          </div>

          <Button
            onClick={onAccept}
            variant="success"
            size="lg"
            icon={<CheckIcon className="w-6 h-6" />}
            className="shadow-lg shadow-green-500/30"
          >
            <span className="font-semibold">Accept Changes</span>
            <kbd className="hidden sm:inline-block ml-2 px-2.5 py-1 bg-green-700/20 text-green-100 rounded text-sm">↵</kbd>
          </Button>
        </div>

        {/* Keyboard Shortcuts Hint */}
        <div className="mt-4 pt-4 border-t border-gray-200 text-center">
          <p className="text-xs text-gray-500 flex items-center justify-center gap-2">
            <KeyboardIcon className="w-3.5 h-3.5" />
            Press
            <kbd className="px-2 py-1 bg-gray-100 text-gray-700 rounded font-mono text-xs border border-gray-300">?</kbd>
            for keyboard shortcuts
          </p>
        </div>
      </div>
    </div>
  );
}
