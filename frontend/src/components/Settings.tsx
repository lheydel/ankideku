import { useState, useMemo, useEffect } from 'react';
import useStore from '../store/useStore.js';
import { ankiApi } from '../services/api.js';
import { CloseIcon, CheckIcon } from './ui/Icons.js';
import { Button } from './ui/Button.js';
import type { Note } from '../types/index.js';

interface SettingsProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function Settings({ isOpen, onClose }: SettingsProps) {
  const { queue, actionsHistory, fieldDisplayConfig, setFieldDisplayConfig } = useStore();

  // Get all unique model types from queue and history
  const modelTypes = useMemo(() => {
    const models = new Map<string, Note>();

    // Add from queue
    queue.forEach(item => {
      if (!models.has(item.original.modelName)) {
        models.set(item.original.modelName, item.original);
      }
    });

    // Add from history
    actionsHistory.forEach(item => {
      if (item.original && !models.has(item.original.modelName)) {
        models.set(item.original.modelName, item.original);
      }
    });

    return Array.from(models.entries());
  }, [queue, actionsHistory]);

  const [localConfig, setLocalConfig] = useState(fieldDisplayConfig);
  const [saving, setSaving] = useState(false);

  // Sync localConfig with store when modal opens
  useEffect(() => {
    if (isOpen) {
      setLocalConfig(fieldDisplayConfig);
    }
  }, [isOpen, fieldDisplayConfig]);

  const handleSave = async () => {
    try {
      setSaving(true);
      await ankiApi.updateFieldDisplayConfig(localConfig);
      setFieldDisplayConfig(localConfig);
      onClose();
    } catch (error) {
      console.error('Failed to save settings:', error);
      alert('Failed to save settings. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleFieldChange = (modelName: string, fieldName: string) => {
    setLocalConfig(prev => ({ ...prev, [modelName]: fieldName }));
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Display Settings</h2>
            <p className="text-sm text-gray-600 mt-1">Configure which field to display in the sidebar for each note type</p>
          </div>
          <Button
            onClick={onClose}
            variant="ghost"
            icon={<CloseIcon className="w-5 h-5 text-gray-600" />}
            className="hover:bg-gray-100"
          />
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {modelTypes.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500">No note types found. Review some cards to configure display settings.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {modelTypes.map(([modelName, sampleNote]) => {
                const fields = Object.keys(sampleNote.fields).sort(
                  (a, b) => sampleNote.fields[a].order - sampleNote.fields[b].order
                );
                const currentField = localConfig[modelName] || fields[0];

                return (
                  <div key={modelName} className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-gray-900 truncate">{modelName}</h3>
                      <p className="text-xs text-gray-500">{fields.length} field{fields.length !== 1 ? 's' : ''} available</p>
                    </div>
                    <select
                      value={currentField}
                      onChange={(e) => handleFieldChange(modelName, e.target.value)}
                      className="px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent min-w-[200px]"
                    >
                      {fields.map(fieldName => (
                        <option key={fieldName} value={fieldName}>
                          {fieldName}
                        </option>
                      ))}
                    </select>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 flex items-center justify-end gap-3">
          <Button
            onClick={onClose}
            variant="secondary"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={saving}
            variant="primary"
            icon={<CheckIcon className="w-4 h-4" />}
            loading={saving}
          >
            {saving ? 'Saving...' : 'Save Settings'}
          </Button>
        </div>
      </div>
    </div>
  );
}
