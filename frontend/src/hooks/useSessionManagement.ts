import { useState } from 'react';
import type { CardSuggestion, SessionRequest, SessionData, SessionMetadata } from '../types';
import { sessionApi } from '../services/sessionApi';

export function useSessionManagement() {
  const [currentSession, setCurrentSession] = useState<string | null>(null);
  const [sessions, setSessions] = useState<SessionMetadata[]>([]);
  const [currentSessionData, setCurrentSessionData] = useState<SessionData | null>(null);

  const createSession = async (prompt: string, deckName: string): Promise<string> => {
    const { sessionId } = await sessionApi.createSession(prompt, deckName);
    setCurrentSession(sessionId);
    return sessionId;
  };

  const loadSession = async (sessionId: string): Promise<SessionData> => {
    const data: SessionData = await sessionApi.loadSession(sessionId);
    setCurrentSession(sessionId);
    setCurrentSessionData(data);
    return data;
  };

  const listSessions = async (): Promise<SessionMetadata[]> => {
    const { sessions } = await sessionApi.listSessions();
    setSessions(sessions);
    return sessions;
  };

  const deleteSession = async (sessionId: string): Promise<void> => {
    await sessionApi.deleteSession(sessionId);

    // Remove from local state
    setSessions(prev => prev.filter(session => session.sessionId !== sessionId));

    // Clear current session if it was deleted
    if (currentSession === sessionId) {
      setCurrentSession(null);
    }
  };

  const cancelSession = async (sessionId: string): Promise<void> => {
    await sessionApi.cancelSession(sessionId);
  };

  const clearSession = (): void => {
    setCurrentSession(null);
    setCurrentSessionData(null);
  };

  return {
    currentSession,
    currentSessionData,
    sessions,
    createSession,
    loadSession,
    listSessions,
    deleteSession,
    cancelSession,
    clearSession
  };
}
