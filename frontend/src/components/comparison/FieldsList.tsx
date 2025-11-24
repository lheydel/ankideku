import { useMemo } from 'react';
import DiffMatchPatch from 'diff-match-patch';
import { WarningIcon, CheckIcon } from '../ui/Icons';
import { getSortedFields } from '../../utils/cardUtils';
import { getFieldDisplayValue } from '../../utils/editingUtils';
import type { NoteField } from '../../types';

const dmp = new DiffMatchPatch();

interface CardFieldProps {
  fieldName: string;
  value: string;
  isChanged: boolean;
  showDiff?: 'original' | 'suggested';
  diffs?: Array<[number, string]>;
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
        <FieldContent value={value} showDiff={showDiff} diffs={diffs} />
      )}
    </div>
  );
}

interface FieldContentProps {
  value: string;
  showDiff?: 'original' | 'suggested';
  diffs?: Array<[number, string]>;
}

function FieldContent({ value, showDiff, diffs }: FieldContentProps) {
  if (!value) {
    return <span className="text-gray-400 dark:text-gray-500 italic text-xs">(empty)</span>;
  }

  if (showDiff && diffs) {
    return <DiffDisplay diffs={diffs} mode={showDiff} />;
  }

  return (
    <div className="text-sm text-gray-900 dark:text-gray-100 whitespace-pre-wrap leading-relaxed">
      {value}
    </div>
  );
}

interface DiffDisplayProps {
  diffs: Array<[number, string]>;
  mode: 'original' | 'suggested';
}

function DiffDisplay({ diffs, mode }: DiffDisplayProps) {
  return (
    <div className="text-sm text-gray-900 dark:text-gray-100 whitespace-pre-wrap leading-relaxed">
      {diffs.map((diff, i) => {
        const [operation, text] = diff;

        if (mode === 'original') {
          if (operation === 1) return null; // Hide additions in original
          if (operation === -1) {
            return (
              <span key={i} className="bg-red-100 dark:bg-red-900/40 text-red-900 dark:text-red-200 px-0.5 rounded">
                {text}
              </span>
            );
          }
        } else {
          if (operation === -1) return null; // Hide deletions in suggested
          if (operation === 1) {
            return (
              <span key={i} className="bg-green-200 dark:bg-green-900/40 text-green-900 dark:text-green-200 px-0.5 rounded font-medium">
                {text}
              </span>
            );
          }
        }

        return <span key={i}>{text}</span>;
      })}
    </div>
  );
}

interface FieldsListProps {
  fields: Record<string, NoteField>;
  changes?: Record<string, string>;
  editedChanges: Record<string, string>;
  showOriginalSuggestions: boolean;
  mode: 'original' | 'suggested';
  isEditing?: boolean;
  onFieldChange?: (fieldName: string, value: string) => void;
}

/**
 * Renders a list of card fields with diff highlighting.
 * Used by both the Original Card and Suggested Card panels.
 */
export function FieldsList({
  fields,
  changes,
  editedChanges,
  showOriginalSuggestions,
  mode,
  isEditing = false,
  onFieldChange,
}: FieldsListProps) {
  const sortedFields = useMemo(() => getSortedFields(fields), [fields]);

  return (
    <div className="px-5">
      {sortedFields.map(([fieldName, fieldData]) => {
        const displayValue = getFieldDisplayValue(
          fieldName,
          fieldData.value,
          changes,
          editedChanges,
          showOriginalSuggestions
        );

        const diffs = dmp.diff_main(fieldData.value ?? '', displayValue ?? '');
        const isChanged = diffs.some(d => d[0] !== 0);

        const showDiff = mode === 'original'
          ? (isChanged ? 'original' : undefined)
          : (!isEditing && isChanged ? 'suggested' : undefined);

        const isReadonly = mode === 'original' || !isEditing || showOriginalSuggestions;

        return (
          <CardField
            key={fieldName}
            fieldName={fieldName}
            value={mode === 'original' ? fieldData.value : displayValue}
            isChanged={isChanged}
            showDiff={showDiff}
            diffs={diffs}
            readonly={isReadonly}
            onChange={onFieldChange ? (value) => onFieldChange(fieldName, value) : undefined}
          />
        );
      })}
    </div>
  );
}

export { CardField, DiffDisplay };
