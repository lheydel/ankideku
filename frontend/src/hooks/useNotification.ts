import { useState, useCallback } from 'react';

export type NotificationType = 'success' | 'error' | 'info';

export interface Notification {
  message: string;
  type: NotificationType;
}

export function useNotification() {
  const [notification, setNotification] = useState<Notification | null>(null);

  const showNotification = useCallback((message: string, type: NotificationType = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  }, []);

  return { notification, showNotification };
}
