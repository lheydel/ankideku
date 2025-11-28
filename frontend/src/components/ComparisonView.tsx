import { useState } from 'react';
import useStore, { selectSessionProgress } from '../store/useStore';
import { CheckIcon, ClockIcon } from './ui/Icons';
import { Breadcrumb } from './ui/Breadcrumb';
import ConfirmDialog from './ui/ConfirmDialog';
import {
  EmptyState,
  HeaderCard,
  ReasoningCard,
  EditControls,
  ReadonlyStatus,
  ActionButtons,
  FieldsList,
} from './comparison';
import { isSessionActive } from '../utils/sessionUtils';
import { useComparisonEditor } from '../hooks/useComparisonEditor';
import { useMultipleLoadingActions } from '../hooks/useLoadingAction';
import { getSortedFields } from '../utils/cardUtils';
import { getSuggestionCardStyles } from '../utils/styleUtils';
import { getSuggestedCardTitle } from '../utils/textUtils';

interface ComparisonViewProps {
  onBackToSessions: () => void;
  onAccept: (editedChanges?: Record<string, string>) => void;
  onReject: (editedChanges?: Record<string, string>) => void;
  onSkip: () => void;
}

export default function ComparisonView({
  onBackToSessions,
  onAccept,
  onReject,
  onSkip,
}: ComparisonViewProps) {
  const { selectedCard, queue, currentIndex, currentSessionData } = useStore();
  const { processed: processingProgress, total: processingTotal } = useStore(selectSessionProgress);
  const isProcessing = isSessionActive(currentSessionData);
  const card = selectedCard;

  // Edit state management
  const editor = useComparisonEditor({ card });

  // Loading states for accept/reject actions
  const actions = useMultipleLoadingActions(['accept', 'reject'] as const);

  // Copy success state
  const [copySuccess, setCopySuccess] = useState(false);

  // Handle accept with loading state
  const handleAcceptClick = async () => {
    await actions.execute('accept', async () => {
      await onAccept(editor.getChangesForAction());
    });
  };

  // Handle reject with loading state
  const handleRejectClick = async () => {
    await actions.execute('reject', async () => {
      await onReject(editor.getChangesForAction());
    });
  };

  // Copy current comparison to clipboard for external LLM review
  const handleCopyToClipboard = async () => {
    if (!card) return;

    const displayChanges = editor.showOriginalSuggestions
      ? card.changes
      : (editor.hasManualEdits ? editor.editedChanges : card.changes);

    let text = `# Anki Card Review Request\n\n`;

    if (card.reasoning) {
      text += `## AI Reasoning\n${card.reasoning}\n\n`;
    }

    text += `## Original Card\n`;
    getSortedFields(card.original.fields).forEach(([fieldName, fieldData]) => {
      text += `**${fieldName}:** ${fieldData.value || '(empty)'}\n`;
    });

    const sectionTitle = editor.showOriginalSuggestions
      ? 'AI Suggested Changes'
      : editor.hasManualEdits ? 'Manually Edited Changes' : 'Suggested Changes';

    text += `\n## ${sectionTitle}\n`;
    getSortedFields(card.original.fields).forEach(([fieldName, fieldData]) => {
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
  };

  // Empty state
  if (!card) {
    return (
      <div className="space-y-6">
        <Breadcrumb onClick={onBackToSessions} />
        <EmptyState
          isProcessing={isProcessing}
          progress={processingProgress}
          total={processingTotal}
        />
      </div>
    );
  }

  const { original, changes, reasoning } = card;
  const changedFields = Object.keys(changes || {});
  const styles = getSuggestionCardStyles(editor.showOriginalSuggestions, editor.hasManualEdits);
  const isActionsDisabled = editor.isEditing || (editor.hasManualEdits && editor.showOriginalSuggestions) || actions.isAnyLoading;

  return (
    <div className="space-y-6">
      <Breadcrumb onClick={onBackToSessions} />

      <HeaderCard
        deckName={original.deckName}
        modelName={original.modelName}
        currentIndex={currentIndex}
        queueLength={queue.length}
        copySuccess={copySuccess}
        onCopy={handleCopyToClipboard}
      />

      {reasoning && <ReasoningCard reasoning={reasoning} />}

      <div className="grid grid-cols-2 gap-6">
        {/* Original Card */}
        <div className="card overflow-hidden">
          <div className="bg-gradient-to-r from-gray-100 to-gray-50 dark:from-gray-700 dark:to-gray-750 px-5 py-3 border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center gap-2">
              <ClockIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
              <span className="font-semibold text-gray-900 dark:text-gray-100">Original Card</span>
            </div>
          </div>
          <FieldsList
            fields={original.fields}
            changes={changes}
            editedChanges={editor.editedChanges}
            showOriginalSuggestions={editor.showOriginalSuggestions}
            mode="original"
          />
        </div>

        {/* Suggested Card */}
        <div className={`card overflow-hidden border-l-4 ${styles.borderClass}`}>
          <div className={`px-5 py-3 border-b ${styles.headerBgClass}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckIcon className={`w-5 h-5 ${styles.iconClass}`} />
                <span className={`font-semibold ${styles.titleClass}`}>
                  {getSuggestedCardTitle(editor.isEditing, editor.showOriginalSuggestions, editor.hasManualEdits)}
                </span>
              </div>
              {!card.readonly && (
                <EditControls
                  isEditing={editor.isEditing}
                  hasManualEdits={editor.hasManualEdits}
                  showOriginalSuggestions={editor.showOriginalSuggestions}
                  onToggleEdit={editor.toggleEditMode}
                  onToggleOriginal={editor.toggleOriginalSuggestions}
                  onRevert={() => editor.setShowRevertConfirm(true)}
                />
              )}
            </div>
          </div>
          <FieldsList
            fields={original.fields}
            changes={changes}
            editedChanges={editor.editedChanges}
            showOriginalSuggestions={editor.showOriginalSuggestions}
            mode="suggested"
            isEditing={editor.isEditing}
            onFieldChange={editor.updateField}
          />
        </div>
      </div>

      {changedFields.length === 0 && (
        <div className="card text-center py-12">
          <CheckIcon className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-gray-500 dark:text-gray-400 font-medium">No changes detected</p>
        </div>
      )}

      {card.readonly ? (
        <ReadonlyStatus status={card.status} timestamp={card.timestamp} />
      ) : (
        <ActionButtons
          hasManualEdits={editor.hasManualEdits}
          showOriginalSuggestions={editor.showOriginalSuggestions}
          isAccepting={actions.isLoading('accept')}
          isRejecting={actions.isLoading('reject')}
          isDisabled={isActionsDisabled}
          onAccept={handleAcceptClick}
          onReject={handleRejectClick}
          onSkip={onSkip}
        />
      )}

      <ConfirmDialog
        isOpen={editor.showRevertConfirm}
        title="Revert Manual Edits"
        message="Are you sure you want to revert all manual edits? This will restore the original AI suggestion and cannot be undone."
        confirmText="Revert"
        cancelText="Cancel"
        variant="danger"
        onConfirm={editor.revertEdits}
        onCancel={() => editor.setShowRevertConfirm(false)}
      />
    </div>
  );
}
