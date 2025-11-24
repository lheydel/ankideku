import { BulbIcon } from '../ui/Icons';

interface ReasoningCardProps {
  reasoning: string;
}

export function ReasoningCard({ reasoning }: ReasoningCardProps) {
  return (
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
  );
}
