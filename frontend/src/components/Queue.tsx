import useStore from '../store/useStore.js';

export default function Queue() {
  const { queue, currentIndex } = useStore();

  if (queue.length === 0) {
    return null;
  }

  const reviewed = currentIndex;
  const total = queue.length;

  return (
    <div className="bg-white border-r border-gray-200 w-64 flex flex-col">
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <h3 className="font-semibold text-gray-900">Review Queue</h3>
        <p className="text-sm text-gray-600 mt-1">
          {reviewed} / {total} reviewed
        </p>
        <div className="mt-2 bg-gray-200 rounded-full h-2">
          <div
            className="bg-primary h-2 rounded-full transition-all"
            style={{ width: `${(reviewed / total) * 100}%` }}
          />
        </div>
      </div>

      {/* Queue items */}
      <div className="flex-1 overflow-y-auto">
        {queue.slice(currentIndex, currentIndex + 5).map((item, idx) => (
          <div
            key={item.noteId}
            className={`p-3 border-b border-gray-100 ${
              idx === 0 ? 'bg-blue-50 border-l-4 border-l-primary' : 'hover:bg-gray-50'
            }`}
          >
            <div className="text-sm font-medium text-gray-900 truncate">
              {Object.values(item.original.fields)[0]?.value || 'No content'}
            </div>
            <div className="text-xs text-gray-500 mt-1">
              {item.changes ? Object.keys(item.changes).length : 0} field(s) changed
            </div>
          </div>
        ))}
      </div>

      {/* Stats */}
      <div className="p-4 border-t border-gray-200 bg-gray-50">
        <div className="text-xs text-gray-600 space-y-1">
          <div className="flex justify-between">
            <span>Remaining:</span>
            <span className="font-medium">{total - reviewed}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
