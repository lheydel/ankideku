import { useMemo } from 'react';
import useStore from '../store/useStore.js';
import DiffMatchPatch from 'diff-match-patch';

const dmp = new DiffMatchPatch();

interface FieldDiffProps {
  fieldName: string;
  originalValue: string;
  suggestedValue: string;
}

function FieldDiff({ fieldName, originalValue, suggestedValue }: FieldDiffProps) {
  const diffs = useMemo(() => {
    return dmp.diff_main(originalValue || '', suggestedValue || '');
  }, [originalValue, suggestedValue]);

  // If no changes, don't render this field
  if (originalValue === suggestedValue) {
    return null;
  }

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
        <h4 className="font-medium text-gray-900">{fieldName}</h4>
      </div>

      <div className="grid grid-cols-2 divide-x divide-gray-200">
        {/* Original */}
        <div className="p-4">
          <div className="text-xs font-medium text-gray-500 uppercase mb-2">Original</div>
          <div className="text-sm text-gray-900 whitespace-pre-wrap">
            {diffs.map((diff, i) => {
              const [operation, text] = diff;
              if (operation === 1) return null; // Skip additions in original
              if (operation === -1) {
                return (
                  <span key={i} className="bg-red-100 text-red-800 line-through">
                    {text}
                  </span>
                );
              }
              return <span key={i}>{text}</span>;
            })}
          </div>
        </div>

        {/* Suggested */}
        <div className="p-4 bg-green-50">
          <div className="text-xs font-medium text-green-700 uppercase mb-2">Suggested</div>
          <div className="text-sm text-gray-900 whitespace-pre-wrap">
            {diffs.map((diff, i) => {
              const [operation, text] = diff;
              if (operation === -1) return null; // Skip deletions in suggestion
              if (operation === 1) {
                return (
                  <span key={i} className="bg-green-200 text-green-900">
                    {text}
                  </span>
                );
              }
              return <span key={i}>{text}</span>;
            })}
          </div>
        </div>
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
      <div className="text-center py-12">
        <p className="text-gray-500">No card to review</p>
      </div>
    );
  }

  const { original, changes, reasoning } = card;
  const changedFields = Object.keys(changes || {});

  return (
    <div className="space-y-6">
      {/* Metadata */}
      <div className="flex items-center justify-between text-sm text-gray-600">
        <div className="space-x-4">
          <span>
            <span className="font-medium">Deck:</span> {original.deckName || 'Unknown'}
          </span>
          <span>
            <span className="font-medium">Note Type:</span> {original.modelName}
          </span>
        </div>
        <div>
          Card {currentIndex + 1} of {queue.length}
        </div>
      </div>

      {/* AI Reasoning */}
      {reasoning && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start gap-2">
            <span className="text-blue-600 text-lg">💡</span>
            <div>
              <p className="text-sm font-medium text-blue-900">AI Reasoning</p>
              <p className="text-sm text-blue-800 mt-1">{reasoning}</p>
            </div>
          </div>
        </div>
      )}

      {/* Field Comparisons */}
      <div className="space-y-4">
        {changedFields.map((fieldName) => (
          <FieldDiff
            key={fieldName}
            fieldName={fieldName}
            originalValue={original.fields[fieldName]?.value}
            suggestedValue={changes[fieldName]}
          />
        ))}

        {changedFields.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No changes detected
          </div>
        )}
      </div>

      {/* Unchanged Fields (Collapsed) */}
      <details className="border border-gray-200 rounded-lg">
        <summary className="px-4 py-3 cursor-pointer hover:bg-gray-50 text-sm font-medium text-gray-700">
          Show unchanged fields ({Object.keys(original.fields).length - changedFields.length})
        </summary>
        <div className="border-t border-gray-200 p-4 space-y-3">
          {Object.entries(original.fields)
            .filter(([fieldName]) => !changedFields.includes(fieldName))
            .map(([fieldName, fieldData]) => (
              <div key={fieldName} className="text-sm">
                <div className="font-medium text-gray-700">{fieldName}</div>
                <div className="text-gray-600 mt-1 whitespace-pre-wrap">
                  {fieldData.value || '(empty)'}
                </div>
              </div>
            ))}
        </div>
      </details>

      {/* Action Buttons */}
      <div className="flex items-center justify-between pt-6 border-t border-gray-200">
        <div className="flex gap-3">
          <button
            onClick={onReject}
            className="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-medium hover:bg-gray-200 transition"
          >
            Reject <span className="text-xs text-gray-500">(R)</span>
          </button>

          <button
            onClick={onSkip}
            className="px-6 py-3 bg-yellow-100 text-yellow-700 rounded-lg font-medium hover:bg-yellow-200 transition"
          >
            Skip <span className="text-xs text-yellow-600">(S)</span>
          </button>
        </div>

        <button
          onClick={onAccept}
          className="px-8 py-3 bg-primary text-white rounded-lg font-medium hover:bg-blue-700 transition text-lg"
        >
          Accept <span className="text-sm opacity-90">(Enter)</span>
        </button>
      </div>

      {/* Keyboard Shortcuts Help */}
      <div className="text-xs text-gray-500 text-center">
        Press <kbd className="px-2 py-1 bg-gray-100 rounded">?</kbd> for all keyboard shortcuts
      </div>
    </div>
  );
}
