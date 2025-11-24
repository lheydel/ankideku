import { EditIcon, EyeIcon, RefreshIcon } from '../ui/Icons';

interface EditControlsProps {
  isEditing: boolean;
  hasManualEdits: boolean;
  showOriginalSuggestions: boolean;
  onToggleEdit: () => void;
  onToggleOriginal: () => void;
  onRevert: () => void;
}

export function EditControls({
  isEditing,
  hasManualEdits,
  showOriginalSuggestions,
  onToggleEdit,
  onToggleOriginal,
  onRevert,
}: EditControlsProps) {
  return (
    <div className="flex gap-2">
      {(isEditing || hasManualEdits) && (
        <button
          onClick={onToggleOriginal}
          className="px-2.5 py-1.5 text-xs font-medium bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-650 transition-colors flex items-center gap-1.5"
          title={showOriginalSuggestions ? "Show edited version" : "View original AI suggestion"}
        >
          <EyeIcon className="w-3.5 h-3.5" />
          {showOriginalSuggestions ? 'Edited' : 'AI'}
        </button>
      )}
      <button
        onClick={onToggleEdit}
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
          onClick={onRevert}
          className="px-2.5 py-1.5 text-xs font-medium bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-700 rounded-md hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors flex items-center gap-1.5"
          title="Revert to AI suggestion"
        >
          <RefreshIcon className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
}
