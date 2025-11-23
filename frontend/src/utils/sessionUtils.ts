import { SessionState, type SessionStateData, type SessionData } from '../types';

/**
 * Check if a session is currently active (pending or running)
 */
export function isSessionActive(sessionData: SessionData | null): boolean {
  if (!sessionData?.state) return false;
  const state = sessionData.state.state;
  return state === SessionState.PENDING || state === SessionState.RUNNING;
}

/**
 * Check if a session has completed (success, failed, or cancelled)
 */
export function isSessionComplete(sessionData: SessionData | null): boolean {
  if (!sessionData?.state) return false;
  const state = sessionData.state.state;
  return state === SessionState.COMPLETED || state === SessionState.FAILED || state === SessionState.CANCELLED;
}

/**
 * Get a human-readable status message for a session
 */
export function getSessionStatusMessage(sessionData: SessionData | null): string {
  if (!sessionData) return 'No active session';
  if (!sessionData.state) return 'Unknown status';

  const { state, message } = sessionData.state;

  switch (state) {
    case SessionState.PENDING:
      return 'Session pending...';
    case SessionState.RUNNING:
      return 'Processing cards...';
    case SessionState.COMPLETED:
      return `Complete! ${sessionData.suggestions.length} suggestions found`;
    case SessionState.FAILED:
      return message || 'Session failed';
    case SessionState.CANCELLED:
      return 'Session cancelled';
    default:
      return 'Unknown status';
  }
}
