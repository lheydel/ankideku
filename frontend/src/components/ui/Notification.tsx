import { CheckIcon, CloseIcon, InfoIcon } from './Icons';
import type { Notification } from '../../hooks/useNotification';

interface NotificationProps {
  notification: Notification;
}

export default function Notification({ notification }: NotificationProps) {
  const { message, type } = notification;

  const styles = {
    success: 'bg-green-500/95 text-white border-green-400',
    error: 'bg-red-500/95 text-white border-red-400',
    info: 'bg-primary-500/95 text-white border-primary-400',
  };

  const Icon = {
    success: CheckIcon,
    error: CloseIcon,
    info: InfoIcon,
  }[type];

  return (
    <div className="fixed top-6 right-6 z-50 animate-slide-in">
      <div className={`px-6 py-4 rounded-xl shadow-lg backdrop-blur-sm border ${styles[type]}`}>
        <div className="flex items-center gap-3">
          <Icon className="w-5 h-5" />
          <span className="font-medium">{message}</span>
        </div>
      </div>
    </div>
  );
}
