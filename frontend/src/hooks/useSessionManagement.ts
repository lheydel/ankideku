import { useCallback } from 'react';
import type { CardSuggestion, SessionRequest, SessionData, SessionMetadata } from '../types';
import { sessionApi } from '../services/sessionApi';
import useStore from '../store/useStore';

export function useSessionManagement() {
  const { currentSession, currentSessionData, sessions, setCurrentSession, setCurrentSessionData, setSessions, setSessionHistory } = useStore();

  const listSessions = useCallback(async (): Promise<SessionMetadata[]> => {
    const { sessions } = await sessionApi.listSessions();
    setSessions(sessions);
    return sessions;
  }, [setSessions]);

  const createSession = useCallback(async (prompt: string, deckName: string): Promise<string> => {
    const { sessionId } = await sessionApi.createSession(prompt, deckName);
    setCurrentSession(sessionId);

    // Load the session data to populate currentSessionData with state
    const data = await sessionApi.loadSession(sessionId);
    setCurrentSessionData(data);

    // Refresh the sessions list to include the new session
    await listSessions();

    return sessionId;
  }, [setCurrentSession, setCurrentSessionData, listSessions]);

  const loadSession = useCallback(async (sessionId: string): Promise<SessionData> => {
    const data: SessionData = await sessionApi.loadSession(sessionId);
    setCurrentSession(sessionId);
    setCurrentSessionData(data);

    // Load history if it exists
    if (data.history) {
      setSessionHistory(data.history);
    } else {
      setSessionHistory([]);
    }

    return data;
  }, [setCurrentSession, setCurrentSessionData, setSessionHistory]);

  const deleteSession = useCallback(async (sessionId: string): Promise<void> => {
    await sessionApi.deleteSession(sessionId);

    // Remove from store state
    setSessions(sessions.filter(session => session.sessionId !== sessionId));

    // Clear current session if it was deleted
    if (currentSession === sessionId) {
      setCurrentSession(null);
      setCurrentSessionData(null);
    }
  }, [sessions, currentSession, setSessions, setCurrentSession, setCurrentSessionData]);

  const cancelSession = useCallback(async (sessionId: string): Promise<void> => {
    await sessionApi.cancelSession(sessionId);
  }, []);

  const clearSession = useCallback((): void => {
    setCurrentSession(null);
    setCurrentSessionData(null);
  }, [setCurrentSession, setCurrentSessionData]);

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
