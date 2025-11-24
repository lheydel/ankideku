import { useState, useCallback } from 'react';

/**
 * Hook for managing loading state during async actions.
 * Eliminates the repetitive pattern of setLoading(true) -> await action -> setLoading(false)
 */
export function useLoadingAction() {
  const [isLoading, setIsLoading] = useState(false);

  const execute = useCallback(async <T>(action: () => Promise<T>): Promise<T | undefined> => {
    setIsLoading(true);
    try {
      return await action();
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, execute };
}

/**
 * Hook for managing multiple named loading states.
 * Useful when a component has multiple independent loading actions.
 */
export function useMultipleLoadingActions<T extends string>(keys: T[]) {
  const [loadingStates, setLoadingStates] = useState<Record<T, boolean>>(
    () => keys.reduce((acc, key) => ({ ...acc, [key]: false }), {} as Record<T, boolean>)
  );

  const execute = useCallback(async <R>(key: T, action: () => Promise<R>): Promise<R | undefined> => {
    setLoadingStates(prev => ({ ...prev, [key]: true }));
    try {
      return await action();
    } finally {
      setLoadingStates(prev => ({ ...prev, [key]: false }));
    }
  }, []);

  const isLoading = useCallback((key: T) => loadingStates[key], [loadingStates]);
  const isAnyLoading = Object.values(loadingStates).some(Boolean);

  return { isLoading, isAnyLoading, execute };
}
