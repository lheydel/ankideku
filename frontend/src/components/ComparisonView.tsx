import {useMemo, useState, useEffect, useCallback} from 'react';
import useStore from '../store/useStore.js';
import DiffMatchPatch from 'diff-match-patch';
import { WarningIcon, CheckIcon, ArchiveIcon, DeckIcon, BrushIcon, ClipboardIcon, BulbIcon, ClockIcon, CloseIcon, ArrowRightIcon, KeyboardIcon, EditIcon, EyeIcon, RefreshIcon } from './ui/Icons.js';
import { Button } from './ui/Button.js';
import { Breadcrumb } from './ui/Breadcrumb.js';
import ConfirmDialog from './ui/ConfirmDialog.js';
import type { SessionData } from '../types/index.js';
import { isSessionActive } from '../utils/sessionUtils.js';
import { ankiApi } from '../services/api.js';

const dmp = new DiffMatchPatch();

type DiffOperation = -1 | 0 | 1;
type Diff = [DiffOperation, string];

interface CardFieldProps {
  fieldName: string;
  value: string;
  isChanged: boolean;
  showDiff?: 'original' | 'suggested';
  diffs?: Diff[];
  readonly?: boolean;
  onChange?: (value: string) => void;
}

function CardField({ fieldName, value, isChanged, showDiff, diffs, readonly, onChange }: CardFieldProps) {
  return (
    <div className={`py-3 border-b border-gray-100 dark:border-gray-700 last:border-b-0 ${isChanged ? 'bg-yellow-50/30 dark:bg-yellow-900/20' : ''}`}>
      <div className="flex items-center gap-2 mb-1.5">
        {isChanged ? (
          <WarningIcon className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
        ) : (
          <CheckIcon className="w-3.5 h-3.5 text-gray-400 dark:text-gray-500" />
        )}
        <span className={`text-xs font-semibold uppercase tracking-wide leading-none ${isChanged ? 'text-amber-700 dark:text-amber-400' : 'text-gray-500 dark:text-gray-400'}`}>
          {fieldName}
        </span>
      </div>
      {!readonly && onChange ? (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full text-sm text-gray-900 dark:text-gray-100 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md p-2 min-h-[60px] focus:outline-none focus:ring-2 focus:ring-primary-500 dark:focus:ring-primary-400"
          rows={3}
        />
      ) : (
        <div className="text-sm text-gray-900 dark:text-gray-100 whitespace-pre-wrap leading-relaxed">
          {!value
            ? <span className="text-gray-400 dark:text-gray-500 italic text-xs">(empty)</span>
            : showDiff && diffs ? (
            diffs.map((diff, i) => {
              const [operation, text] = diff;
              if (showDiff === 'original') {
                if (operation === 1) return null;
                if (operation === -1) {
                  return (
                    <span key={i} className="bg-red-100 dark:bg-red-900/40 text-red-900 dark:text-red-200 px-0.5 rounded">
                      {text}
                    </span>
                  );
                }
                return <span key={i}>{text}</span>;
              } else {
                if (operation === -1) return null;
                if (operation === 1) {
                  return (
                    <span key={i} className="bg-green-200 dark:bg-green-900/40 text-green-900 dark:text-green-200 px-0.5 rounded font-medium">
                      {text}
                    </span>
                  );
                }
                return <span key={i}>{text}</span>;
              }
            })
          ) : (
            value || <span className="text-gray-400 dark:text-gray-500 italic text-xs">(empty)</span>
          )}
        </div>
      )}
    </div>
  );
}

interface ComparisonViewProps {
  currentSessionData: SessionData | null;
  onBackToSessions: () => void;
  onAccept: (editedChanges?: Record<string, string>) => void;
  onReject: (editedChanges?: Record<string, string>) => void;
  onSkip: () => void;
}

export default function ComparisonView({ currentSessionData, onBackToSessions, onAccept, onReject, onSkip }: ComparisonViewProps) {
  const { selectedCard, queue, currentIndex, currentSession, setSelectedCard, setQueue } = useStore();
  const isProcessing = isSessionActive(currentSessionData);
  const card = selectedCard;

  // State for tracking manual edits and toggle between edited/original suggestions
  const [editedChanges, setEditedChanges] = useState<Record<string, string>>({});
  const [showOriginalSuggestions, setShowOriginalSuggestions] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [showRevertConfirm, setShowRevertConfirm] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);
  const [isAccepting, setIsAccepting] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);

  // Reset state when card changes
  useEffect(() => {
    if (card) {
      // Initialize with editedChanges if available, otherwise use AI changes
      const initialChanges = card.editedChanges || card.changes || {};
      setEditedChanges(initialChanges);
      setShowOriginalSuggestions(false);
      setIsEditing(false);
      setIsAccepting(false);
      setIsRejecting(false);
    }
  }, [card]);

  // Save edited changes to backend when exiting edit mode
  const handleSaveEdits = async () => {
    if (!card || !currentSession) return;

    // Filter out fields that haven't actually changed from the original AI suggestion
    const actuallyEditedChanges: Record<string, string> = {};
    let hasAnyChanges = false;

    Object.entries(editedChanges).forEach(([fieldName, editedValue]) => {
      const originalAISuggestion = card.changes?.[fieldName] || card.original.fields[fieldName]?.value || '';
      if (editedValue !== originalAISuggestion) {
        actuallyEditedChanges[fieldName] = editedValue;
        hasAnyChanges = true;
      }
    });

    // Only save if there are actual changes
    if (hasAnyChanges) {
      try {
        await ankiApi.saveEditedChanges(currentSession, card.noteId, actuallyEditedChanges);
        console.log('Saved edited changes to backend');

        // Update the card in the queue to reflect the saved edits
        const updatedQueue = queue.map(item =>
          item.noteId === card.noteId
            ? { ...item, editedChanges: actuallyEditedChanges }
            : item
        );
        setQueue(updatedQueue);

        // Update selected card in store to reflect the saved edits
        setSelectedCard({
          ...card,
          editedChanges: actuallyEditedChanges
        });
      } catch (error) {
        console.error('Failed to save edited changes:', error);
      }
    }
  };

  if (!card) {
    return (
      <div className="space-y-6">
        {(isProcessing || queue.length > 0) && <Breadcrumb onClick={onBackToSessions} />}
        <div className="card text-center py-16">
          {isProcessing ? (
            <>
              <div className="w-16 h-16 bg-gradient-to-br from-primary-100 to-blue-100 dark:from-primary-900 dark:to-blue-900 rounded-2xl flex items-center justify-center mx-auto mb-6 animate-pulse">
                <BulbIcon className="w-8 h-8 text-primary-600 dark:text-primary-400" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-3">
                Analyzing Cards...
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                AI is processing your deck. Suggestions will appear here as they're generated.
              </p>
            </>
            ) : (
            <>
              <ArchiveIcon className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p className="text-gray-500 dark:text-gray-400 font-medium">No card to review</p>
            </>
          )}
        </div>
      </div>
    );
  }

  const { original, changes, reasoning, editedChanges: loadedEditedChanges } = card;
  const changedFields = Object.keys(changes || {});

  // Check if there are any manual edits
  const hasManualEdits = loadedEditedChanges && Object.keys(loadedEditedChanges).length > 0;

  // Handle accept with loading state
  const handleAcceptClick = async (editedChanges?: Record<string, string>) => {
    setIsAccepting(true);
    try {
      await onAccept(editedChanges);
    } finally {
      setIsAccepting(false);
    }
  };

  // Handle reject with loading state
  const handleRejectClick = async (editedChanges?: Record<string, string>) => {
    setIsRejecting(true);
    try {
      await onReject(editedChanges);
    } finally {
      setIsRejecting(false);
    }
  };

  // Copy current comparison to clipboard for external LLM review
  const handleCopyToClipboard = async () => {
    if (!card) return;

    const displayChanges = showOriginalSuggestions
      ? changes
      : (hasManualEdits ? editedChanges : changes);

    // Build formatted text for LLM
    let text = `# Anki Card Review Request\n\n`;

    if (reasoning) {
      text += `## AI Reasoning\n${reasoning}\n\n`;
    }

    text += `## Original Card\n`;
    Object.entries(original.fields)
      .sort(([, a], [, b]) => a.order - b.order)
      .forEach(([fieldName, fieldData]) => {
        text += `**${fieldName}:** ${fieldData.value || '(empty)'}\n`;
      });

    text += `\n## ${showOriginalSuggestions ? 'AI Suggested Changes' : hasManualEdits ? 'Manually Edited Changes' : 'Suggested Changes'}\n`;
    Object.entries(original.fields)
      .sort(([, a], [, b]) => a.order - b.order)
      .forEach(([fieldName, fieldData]) => {
        const suggestedValue = displayChanges[fieldName] ?? fieldData.value;
        const hasChange = suggestedValue !== fieldData.value;
        text += `**${fieldName}:** ${suggestedValue || '(empty)'}${hasChange ? ' *(modified)*' : ''}\n`;
      });

    try {
      await navigator.clipboard.writeText(text);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
    }
  }

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <Breadcrumb onClick={onBackToSessions} />

      {/* Header Card with Metadata */}
      <div className="card p-5 bg-gradient-to-r from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 text-sm">
              <DeckIcon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
              <span className="font-semibold text-gray-700 dark:text-gray-300">{original.deckName || 'Unknown'}</span>
            </div>
            <div className="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
            <div className="flex items-center gap-2 text-sm">
              <BrushIcon className="w-4 h-4 text-gray-400 dark:text-gray-500" />
              <span className="text-gray-600 dark:text-gray-400">{original.modelName}</span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={handleCopyToClipboard}
              className={`px-3 py-1.5 text-sm font-medium border rounded-md transition-colors flex items-center gap-2 ${
                copySuccess
                  ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 border-green-300 dark:border-green-700'
                  : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-650'
              }`}
              title="Copy to clipboard for external LLM review"
            >
              <ClipboardIcon className="w-4 h-4" />
              {copySuccess ? 'Copied!' : 'Copy for Review'}
            </button>
            <div className="flex items-center gap-2 bg-primary-100 dark:bg-primary-900/30 px-3 py-1.5 rounded-full">
              <ClipboardIcon className="w-4 h-4 text-primary-700 dark:text-primary-400" />
              <span className="text-sm font-bold text-primary-900 dark:text-primary-200">
                {currentIndex + 1} / {queue.length}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* AI Reasoning */}
      {reasoning && (
        <div className="card p-5 bg-gradient-to-br from-primary-50 via-blue-50 to-primary-50 dark:from-primary-900/20 dark:via-blue-900/20 dark:to-primary-900/20 border-primary-200 dark:border-primary-800">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-primary-600 dark:bg-primary-500 rounded-lg flex items-center justify-center flex-shrink-0">
              <BulbIcon className="w-5 h-5 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold text-primary-900 dark:text-primary-200 mb-1 leading-none">AI Reasoning</p>
              <p className="text-sm text-primary-800 dark:text-primary-300 leading-relaxed">{reasoning}</p>
            </div>
          </div>
        </div>
      )}

      {/* Two Separate Cards Side by Side */}
      <div className="grid grid-cols-2 gap-6">
        {/* Original Card */}
        <div className="card overflow-hidden">
          <div className="bg-gradient-to-r from-gray-100 to-gray-50 dark:from-gray-700 dark:to-gray-750 px-5 py-3 border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center gap-2">
              <ClockIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
              <span className="font-semibold text-gray-900 dark:text-gray-100">Original Card</span>
            </div>
          </div>
          <div className="px-5">
            {Object.entries(original.fields)
              .sort(([, a], [, b]) => a.order - b.order)
              .map(([fieldName, fieldData]) => {
                // Determine what's being compared against (AI suggestion or edited version)
                const comparisonValue = showOriginalSuggestions
                  ? (changes[fieldName] ?? fieldData.value)
                  : (editedChanges[fieldName] ?? changes[fieldName] ?? fieldData.value);

                // Calculate diff from original to the comparison value
                const diffs = dmp.diff_main(fieldData.value || '', comparisonValue || '');
                const isChanged = diffs.some(d => d[0] !== 0);

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
        <div className={`card overflow-hidden border-l-4 ${
          showOriginalSuggestions || !hasManualEdits
            ? 'border-l-green-500 dark:border-l-green-600'
            : 'border-l-blue-500 dark:border-l-blue-600'
        }`}>
          <div className={`px-5 py-3 border-b ${
            showOriginalSuggestions || !hasManualEdits
              ? 'bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 border-green-200 dark:border-green-800'
              : 'bg-gradient-to-r from-blue-50 to-sky-50 dark:from-blue-900/20 dark:to-sky-900/20 border-blue-200 dark:border-blue-800'
          }`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckIcon className={`w-5 h-5 ${
                  showOriginalSuggestions || !hasManualEdits
                    ? 'text-green-600 dark:text-green-400'
                    : 'text-blue-600 dark:text-blue-400'
                }`} />
                <span className={`font-semibold ${
                  showOriginalSuggestions || !hasManualEdits
                    ? 'text-green-900 dark:text-green-200'
                    : 'text-blue-900 dark:text-blue-200'
                }`}>
                  {isEditing ? 'Editing Card' : showOriginalSuggestions ? 'Original AI Suggestion' : hasManualEdits ? 'Manually Edited Card' : 'Suggested Card'}
                </span>
              </div>
              {!card.readonly && (
                <div className="flex gap-2">
                  {(isEditing || hasManualEdits) && (
                    <button
                      onClick={() => setShowOriginalSuggestions(!showOriginalSuggestions)}
                      className="px-2.5 py-1.5 text-xs font-medium bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-650 transition-colors flex items-center gap-1.5"
                      title={showOriginalSuggestions ? "Show edited version" : "View original AI suggestion"}
                    >
                      <EyeIcon className="w-3.5 h-3.5" />
                      {showOriginalSuggestions ? 'Edited' : 'AI'}
                    </button>
                  )}
                  <button
                    onClick={async () => {
                      if (isEditing) {
                        // User is clicking "Done" - save changes before exiting edit mode
                        await handleSaveEdits();
                      }
                      setIsEditing(!isEditing);
                    }}
                    className={`px-2.5 py-1.5 text-xs font-medium rounded-md transition-colors flex items-center gap-1.5 ${
                      isEditing
                        ? 'bg-primary-600 dark:bg-primary-500 text-white hover:bg-primary-700 dark:hover:bg-primary-600'
                        : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-650'
                    }`}
                  >
                    <EditIcon className="w-3.5 h-3.5" />
                    {isEditing ? 'Done' : 'Edit'}
                  </button>
                  {hasManualEdits && !isEditing && (
                      <button
                          onClick={() => setShowRevertConfirm(true)}
                          className="px-2.5 py-1.5 text-xs font-medium bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-700 rounded-md hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors flex items-center gap-1.5"
                          title="Revert to AI suggestion"
                      >
                        <RefreshIcon className="w-3.5 h-3.5" />
                      </button>
                  )}
                </div>
              )}
            </div>
          </div>
          <div className="px-5">
            {Object.entries(original.fields)
              .sort(([, a], [, b]) => a.order - b.order)
              .map(([fieldName, fieldData]) => {
                // Use original AI suggestion if showing original, otherwise use edited
                const displayValue = showOriginalSuggestions
                  ? (changes[fieldName] ?? fieldData.value)
                  : (editedChanges[fieldName] ?? changes[fieldName] ?? fieldData.value);

                // Check if this field has been changed (either by AI or by user edits)
                const hasAIChange = changedFields.includes(fieldName);
                const editedValue = editedChanges[fieldName];
                const hasUserEdit = editedValue !== undefined && editedValue !== (changes[fieldName] || fieldData.value);
                const isChanged = hasAIChange || hasUserEdit;

                // Calculate diffs based on current display value vs original
                const diffs = dmp.diff_main(fieldData.value || '', displayValue || '');

                return (
                  <CardField
                    key={fieldName}
                    fieldName={fieldName}
                    value={displayValue}
                    isChanged={isChanged}
                    showDiff={!isEditing && isChanged ? 'suggested' : undefined}
                    diffs={diffs}
                    readonly={!isEditing || showOriginalSuggestions}
                    onChange={(newValue) => {
                      setEditedChanges(prev => ({
                        ...prev,
                        [fieldName]: newValue
                      }));
                    }}
                  />
                );
              })}
          </div>
        </div>
      </div>

      {changedFields.length === 0 && (
        <div className="card text-center py-12">
          <CheckIcon className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-gray-500 dark:text-gray-400 font-medium">No changes detected</p>
        </div>
      )}

      {/* Action Buttons or Status */}
      {card.readonly ? (
        <div className="card p-6 bg-gradient-to-b from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
          <div className={`px-4 py-3 rounded-lg border-2 ${
            card.status === 'accept'
              ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-700'
              : 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-700'
          }`}>
            <div className="flex items-center gap-3">
              {card.status === 'accept' ? (
                <CheckIcon className="w-5 h-5 text-green-600 dark:text-green-400" />
              ) : (
                <CloseIcon className="w-5 h-5 text-red-600 dark:text-red-400" />
              )}
              <div className="flex-1">
                <p className={`text-sm font-semibold ${
                  card.status === 'accept'
                    ? 'text-green-700 dark:text-green-300'
                    : 'text-red-700 dark:text-red-300'
                }`}>
                  {card.status === 'accept' ? 'Accepted' : 'Rejected'}
                </p>
                <p className="text-xs text-gray-600 dark:text-gray-400 mt-0.5">
                  {new Date(card.timestamp || '').toLocaleString()}
                </p>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="card p-6 bg-gradient-to-b from-white to-gray-50 dark:from-gray-800 dark:to-gray-750">
          {/* Warning message when viewing AI suggestion with manual edits */}
          {hasManualEdits && showOriginalSuggestions && (
            <div className="mb-4 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg">
              <div className="flex items-start gap-2">
                <WarningIcon className="w-4 h-4 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
                <p className="text-sm text-amber-800 dark:text-amber-200">
                  You're viewing the original AI suggestion. Switch to "Edited" to review or apply your manual changes.
                </p>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between gap-4">
            <div className="flex gap-3">
              <Button
                onClick={() => {
                  // Only pass fields that were actually edited
                  const actuallyEditedChanges: Record<string, string> = {};
                  Object.entries(editedChanges).forEach(([fieldName, editedValue]) => {
                    const originalAISuggestion = card.changes?.[fieldName] || card.original.fields[fieldName]?.value || '';
                    if (editedValue !== originalAISuggestion) {
                      actuallyEditedChanges[fieldName] = editedValue;
                    }
                  });
                  handleRejectClick(Object.keys(actuallyEditedChanges).length > 0 ? actuallyEditedChanges : undefined);
                }}
                variant="danger"
                icon={<CloseIcon className="w-5 h-5" />}
                disabled={hasManualEdits && showOriginalSuggestions || isAccepting || isRejecting}
                loading={isRejecting}
              >
                Reject
              </Button>

              <Button
                onClick={onSkip}
                variant="warning"
                icon={<ArrowRightIcon className="w-5 h-5" />}
                disabled={hasManualEdits && showOriginalSuggestions || isAccepting || isRejecting}
              >
                Skip
              </Button>
            </div>

            <Button
              onClick={() => {
                // Only pass fields that were actually edited
                const actuallyEditedChanges: Record<string, string> = {};
                Object.entries(editedChanges).forEach(([fieldName, editedValue]) => {
                  const originalAISuggestion = card.changes?.[fieldName] || card.original.fields[fieldName]?.value || '';
                  if (editedValue !== originalAISuggestion) {
                    actuallyEditedChanges[fieldName] = editedValue;
                  }
                });
                handleAcceptClick(Object.keys(actuallyEditedChanges).length > 0 ? actuallyEditedChanges : undefined);
              }}
              variant="primary"
              size="lg"
              icon={<CheckIcon className="w-6 h-6" />}
              className="shadow-lg shadow-green-500/30"
              disabled={hasManualEdits && showOriginalSuggestions || isAccepting || isRejecting}
              loading={isAccepting}
            >
              <span className="font-semibold">Accept Changes</span>
            </Button>
          </div>
        </div>
      )}

      {/* Revert Confirmation Dialog */}
      <ConfirmDialog
        isOpen={showRevertConfirm}
        title="Revert Manual Edits"
        message="Are you sure you want to revert all manual edits? This will restore the original AI suggestion and cannot be undone."
        confirmText="Revert"
        cancelText="Cancel"
        variant="danger"
        onConfirm={async () => {
          if (currentSession && card.noteId) {
            try {
              await ankiApi.revertEditedChanges(currentSession, card.noteId);
              // Reset local state
              setEditedChanges(card.changes || {});
              setShowOriginalSuggestions(false);

              // Update the card in the queue to remove editedChanges
              const updatedQueue = queue.map(item =>
                item.noteId === card.noteId
                  ? { ...item, editedChanges: undefined }
                  : item
              );
              setQueue(updatedQueue);

              // Update selected card in store to remove editedChanges
              if (card) {
                setSelectedCard({
                  ...card,
                  editedChanges: undefined
                });
              }
              setShowRevertConfirm(false);
            } catch (error) {
              console.error('Failed to revert edits:', error);
              setShowRevertConfirm(false);
            }
          }
        }}
        onCancel={() => setShowRevertConfirm(false)}
      />
    </div>
  );
}
