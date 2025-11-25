import { SessionStateBadge } from '../ui/SessionStateBadge';
import type { ChatMessage as ChatMessageType } from '../../hooks/useSidebarChat';

interface ChatMessageProps {
  message: ChatMessageType;
}

const MESSAGE_STYLES = {
  user: 'bg-primary-600 dark:bg-primary-500 text-white',
  system: 'bg-amber-50 dark:bg-amber-900/30 text-amber-900 dark:text-amber-200 border border-amber-200 dark:border-amber-700',
  assistant: 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100',
} as const;

export function ChatMessage({ message }: ChatMessageProps) {
  const alignment = message.type === 'user' ? 'justify-end' : 'justify-start';
  const styleClass = MESSAGE_STYLES[message.type];

  return (
    <div className={`flex ${alignment}`}>
      <div className={`max-w-[80%] rounded-lg px-4 py-2 ${styleClass}`}>
        {message.state && (
          <div className="mb-2">
            <SessionStateBadge state={message.state} />
          </div>
        )}
        <p className="text-sm mb-1 whitespace-pre-wrap">{message.content}</p>
      </div>
    </div>
  );
}
