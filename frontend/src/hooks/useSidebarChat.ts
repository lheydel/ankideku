import { useState, useRef, useEffect, useCallback } from 'react';
import { SessionState } from '../types';
import type { SessionData } from '../types';

export interface ChatMessage {
  id: string;
  type: 'user' | 'assistant' | 'system';
  content: string;
  state?: SessionState;
}

export const WELCOME_MESSAGE: ChatMessage = {
  id: '0',
  type: 'system',
  content: 'Welcome! Select a deck and describe what you want to improve.',
};

/**
 * Get contextual tip based on session state
 */
export function getContextualTip(state: SessionState | undefined, suggestionsCount: number): string {
  if (!state || state === SessionState.PENDING || state === SessionState.RUNNING) {
    return suggestionsCount > 0
      ? '← Tip: You can review suggestions as they arrive'
      : 'Processing your request...';
  }
  if (state === SessionState.COMPLETED) {
    return suggestionsCount > 0
      ? '← Review suggestions in the card view'
      : 'No suggestions found for this deck.';
  }
  if (state === SessionState.FAILED) {
    return 'Try again with a different prompt or check the logs.';
  }
  if (state === SessionState.CANCELLED) {
    return suggestionsCount > 0
      ? '← Review the suggestions found before cancellation'
      : 'Session was cancelled before finding suggestions.';
  }
  return '';
}

interface UseSidebarChatOptions {
  currentSessionData: SessionData | null;
  processing: boolean;
  suggestionsCount: number;
  onSelectDeck: (deckName: string) => void;
}

/**
 * Hook for managing sidebar chat messages and state
 */
export function useSidebarChat({
  currentSessionData,
  processing,
  suggestionsCount,
  onSelectDeck,
}: UseSidebarChatOptions) {
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const progressMessageIdRef = useRef<string | null>(null);

  const addMessage = useCallback((type: ChatMessage['type'], content: string) => {
    setMessages((prev) => [
      ...prev,
      {
        id: prev.length.toString(),
        type,
        content,
      },
    ]);
  }, []);

  const resetToWelcome = useCallback(() => {
    setMessages([WELCOME_MESSAGE]);
    progressMessageIdRef.current = null;
  }, []);

  // Restore chat history when session is loaded, or reset when cleared
  useEffect(() => {
    if (currentSessionData) {
      const { sessionId, request, suggestions, cancelled, state } = currentSessionData;
      const currentState = state?.state || (cancelled ? SessionState.CANCELLED : undefined);

      const sessionMessages: ChatMessage[] = [
        {
          id: '0',
          type: 'system',
          content: `${sessionId}\nDeck: ${request.deckName}\n${request.totalCards} cards`,
        },
        {
          id: '1',
          type: 'user',
          content: request.prompt,
        },
        {
          id: '2',
          type: 'assistant',
          content: getContextualTip(currentState, suggestions.length),
          state: currentState,
        },
      ];

      setMessages(sessionMessages);
      onSelectDeck(request.deckName);
      progressMessageIdRef.current = null;
    } else {
      resetToWelcome();
    }
  }, [currentSessionData, onSelectDeck, resetToWelcome]);

  // Update progress message as suggestions arrive
  useEffect(() => {
    if (processing && suggestionsCount > 0) {
      setMessages((prev) => {
        if (progressMessageIdRef.current) {
          return prev.map((msg) =>
            msg.id === progressMessageIdRef.current
              ? {
                  ...msg,
                  content: '← Tip: You can review suggestions as they arrive',
                  state: SessionState.RUNNING,
                }
              : msg
          );
        } else {
          const newMsg: ChatMessage = {
            id: `progress-${Date.now()}`,
            type: 'assistant',
            content: '← Tip: You can review suggestions as they arrive',
            state: SessionState.RUNNING,
          };
          progressMessageIdRef.current = newMsg.id;
          return [...prev, newMsg];
        }
      });
    } else if (!processing && progressMessageIdRef.current) {
      const finalState = currentSessionData?.state?.state || SessionState.COMPLETED;
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === progressMessageIdRef.current
            ? {
                ...msg,
                content: getContextualTip(finalState, suggestionsCount),
                state: finalState,
              }
            : msg
        )
      );
      progressMessageIdRef.current = null;
    }
  }, [suggestionsCount, processing, currentSessionData?.state?.state]);

  const resetProgressTracking = useCallback(() => {
    progressMessageIdRef.current = null;
  }, []);

  return {
    messages,
    addMessage,
    resetToWelcome,
    resetProgressTracking,
  };
}
