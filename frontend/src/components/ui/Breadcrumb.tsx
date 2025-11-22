import { ChevronLeftIcon } from './Icons';

interface BreadcrumbProps {
  onClick: () => void;
}

export function Breadcrumb({ onClick }: BreadcrumbProps) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
    >
      <ChevronLeftIcon className="w-4 h-4" />
      Back to Sessions
    </button>
  );
}
