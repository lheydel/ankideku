const BASE_URL = 'http://localhost:3001/api/sessions';

class SessionApiClient {
  private async request<T>(
    endpoint: string,
    options?: RequestInit
  ): Promise<T> {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers
      },
      ...options
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Unknown error' }));
      throw new Error(error.error || `HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  async createSession(prompt: string, deckName: string): Promise<{ sessionId: string }> {
    return this.request('/new', {
      method: 'POST',
      body: JSON.stringify({ prompt, deckName })
    });
  }

  async loadSession(sessionId: string): Promise<any> {
    return this.request(`/${sessionId}`);
  }

  async listSessions(): Promise<{ sessions: Array<{ sessionId: string; timestamp: string; deckName: string; totalCards: number }> }> {
    return this.request('');
  }

  async deleteSession(sessionId: string): Promise<{ success: boolean }> {
    return this.request(`/${sessionId}`, {
      method: 'DELETE'
    });
  }

  async cancelSession(sessionId: string): Promise<{ success: boolean }> {
    return this.request(`/${sessionId}/cancel`, {
      method: 'POST'
    });
  }

  async getSessionStatus(sessionId: string): Promise<any> {
    return this.request(`/${sessionId}/status`);
  }

  async getActiveSessions(): Promise<{ activeSessions: string[]; count: number }> {
    return this.request('/active');
  }
}

export const sessionApi = new SessionApiClient();
