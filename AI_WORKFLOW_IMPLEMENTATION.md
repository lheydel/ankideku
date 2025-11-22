# AI Workflow Implementation Specification

**Last Updated**: 2025-01-22

This document details the implementation of the AI-powered card suggestion workflow using file-based async processing with Claude Code CLI.

---

## Architecture Overview

### Core Concept

Instead of direct API calls, we use a **file-based async queue system** where:
1. Backend creates session folders with request files
2. Claude Code CLI processes requests by reading/writing files
3. Backend watches for suggestion files and streams to frontend in real-time
4. Frontend updates queue as suggestions arrive via WebSocket

### Why File-Based?

- **No API keys required** - Uses existing Claude Code subscription
- **Natural async processing** - File watching enables real-time updates
- **Session persistence** - Each session is a folder with all artifacts
- **Traceability** - Complete audit trail in filesystem
- **Simple debugging** - Inspect files at any point in process

---

## File Structure

```
database/
  decks/
    deck-name.json              # Cached card data (existing)
  settings.json                 # User settings (existing)
  ai-sessions/
    session-2025-01-22-143022/
      request.json              # Prompt + deck info + parameters
      suggestion-123.json       # One file per card needing changes
      suggestion-456.json
      suggestion-789.json
      metadata.json             # Session stats (optional)
```

### Session Folder Contents

**request.json** - Created by backend
```json
{
  "sessionId": "session-2025-01-22-143022",
  "prompt": "Fix spelling and grammar errors",
  "deckName": "JLPT N3 Vocabulary",
  "deckPath": "database/decks/JLPT N3 Vocabulary.json",
  "totalCards": 5247,
  "timestamp": "2025-01-22T14:30:22.000Z"
}
```

**suggestion-{noteId}.json** - Created by Claude Code
```json
{
  "noteId": 123,
  "original": {
    "fields": {
      "Kana": { "value": "こんいちは", "order": 0 },
      "Meaning": { "value": "Hello", "order": 1 }
    }
  },
  "changes": {
    "Kana": "こんにちは"
  },
  "reasoning": "Fixed typo: いち → に in greeting こんにちは"
}
```

---

## Workflow Sequence

### 1. User Initiates Session (Frontend)

```
User clicks "Generate Suggestions"
  ↓
Frontend calls POST /api/sessions/new
  with { prompt, deckName }
  ↓
Receives { sessionId }
  ↓
Connects to WebSocket for real-time updates
  ↓
Displays "Processing..." state
```

### 2. Backend Creates Session

```
Receive POST /api/sessions/new
  ↓
Generate sessionId (timestamp-based)
  ↓
Create directory: database/ai-sessions/{sessionId}/
  ↓
Write request.json with prompt + deck info
  ↓
Start file watcher on session directory
  ↓
Spawn Claude Code CLI with session path
  ↓
Return { sessionId } to frontend
```

### 3. Claude Code Processes Request

```
Claude spawned with: --session database/ai-sessions/{sessionId}
  ↓
Read database/ai-sessions/{sessionId}/request.json
  ↓
Extract prompt, deckName, deckPath
  ↓
Read card data from database/decks/{deckName}.json
  ↓
Process cards based on prompt
  ↓
For each card needing changes:
  Write suggestion-{noteId}.json to session folder
  ↓
Exit when complete
```

### 4. Backend Streams Suggestions

```
File watcher detects new suggestion-*.json
  ↓
Read file contents
  ↓
Parse suggestion data
  ↓
Emit WebSocket event: "suggestion:new"
  with suggestion data
  ↓
Frontend receives and adds to queue
```

### 5. Frontend Updates Queue

```
WebSocket receives "suggestion:new" event
  ↓
Add suggestion to queue state
  ↓
UI updates automatically (React state)
  ↓
User sees new card appear in queue
  ↓
Can start reviewing immediately (no need to wait for all)
```

---

## Backend Implementation

### New Services

#### 1. Session Service (`backend/src/services/sessionService.ts`)

```typescript
interface SessionRequest {
  sessionId: string;
  prompt: string;
  deckName: string;
  deckPath: string;
  totalCards: number;
  timestamp: string;
}

interface Suggestion {
  noteId: number;
  original: Note;
  changes: Record<string, string>;
  reasoning: string;
}

class SessionService {
  private sessionsDir = 'database/ai-sessions';

  // Create new session
  async createSession(prompt: string, deckName: string): Promise<string> {
    const sessionId = `session-${this.generateTimestamp()}`;
    const sessionDir = path.join(this.sessionsDir, sessionId);

    await fs.mkdir(sessionDir, { recursive: true });

    const request: SessionRequest = {
      sessionId,
      prompt,
      deckName,
      deckPath: `database/decks/${deckName}.json`,
      totalCards: await this.getCardCount(deckName),
      timestamp: new Date().toISOString()
    };

    await fs.writeFile(
      path.join(sessionDir, 'request.json'),
      JSON.stringify(request, null, 2)
    );

    return sessionId;
  }

  // Load existing session
  async loadSession(sessionId: string): Promise<Suggestion[]> {
    const sessionDir = path.join(this.sessionsDir, sessionId);
    const files = await fs.readdir(sessionDir);

    const suggestions: Suggestion[] = [];
    for (const file of files) {
      if (file.startsWith('suggestion-')) {
        const content = await fs.readFile(
          path.join(sessionDir, file),
          'utf-8'
        );
        suggestions.push(JSON.parse(content));
      }
    }

    return suggestions;
  }

  // List all sessions
  async listSessions(): Promise<string[]> {
    const sessions = await fs.readdir(this.sessionsDir);
    return sessions.filter(s => s.startsWith('session-'));
  }

  private generateTimestamp(): string {
    return new Date().toISOString()
      .replace(/[:.]/g, '-')
      .replace('T', '-')
      .split('.')[0];
  }

  private async getCardCount(deckName: string): Promise<number> {
    const deckPath = `database/decks/${deckName}.json`;
    const content = await fs.readFile(deckPath, 'utf-8');
    const deck = JSON.parse(content);
    return deck.notes?.length || 0;
  }
}
```

#### 2. File Watcher Service (`backend/src/services/fileWatcher.ts`)

```typescript
import chokidar from 'chokidar';
import { EventEmitter } from 'events';

class FileWatcherService extends EventEmitter {
  private watchers: Map<string, chokidar.FSWatcher> = new Map();

  // Watch a session directory for new suggestions
  watchSession(sessionId: string): void {
    const sessionDir = path.join('database/ai-sessions', sessionId);

    const watcher = chokidar.watch(sessionDir, {
      ignored: /request\.json/,
      persistent: true,
      ignoreInitial: true
    });

    watcher.on('add', async (filePath) => {
      if (path.basename(filePath).startsWith('suggestion-')) {
        const content = await fs.readFile(filePath, 'utf-8');
        const suggestion = JSON.parse(content);

        this.emit('suggestion:new', {
          sessionId,
          suggestion
        });
      }
    });

    this.watchers.set(sessionId, watcher);
  }

  // Stop watching a session
  unwatchSession(sessionId: string): void {
    const watcher = this.watchers.get(sessionId);
    if (watcher) {
      watcher.close();
      this.watchers.delete(sessionId);
    }
  }
}
```

#### 3. Claude Spawner Service (`backend/src/services/claudeSpawner.ts`)

```typescript
import { spawn } from 'child_process';

class ClaudeSpawnerService {
  async spawnClaude(sessionId: string): Promise<void> {
    const sessionPath = path.join('database/ai-sessions', sessionId);

    return new Promise((resolve, reject) => {
      // Spawn Claude Code CLI with session path argument
      const claude = spawn('claude', [
        'process-cards',  // Custom command/script
        sessionPath
      ]);

      claude.stdout.on('data', (data) => {
        console.log(`[Claude ${sessionId}]: ${data}`);
      });

      claude.stderr.on('data', (data) => {
        console.error(`[Claude ${sessionId} Error]: ${data}`);
      });

      claude.on('close', (code) => {
        if (code === 0) {
          console.log(`[Claude ${sessionId}]: Completed successfully`);
          resolve();
        } else {
          reject(new Error(`Claude exited with code ${code}`));
        }
      });
    });
  }
}
```

### API Routes

#### Sessions Router (`backend/src/routes/sessions.ts`)

```typescript
import express from 'express';

const router = express.Router();

// Create new session
router.post('/new', async (req, res) => {
  try {
    const { prompt, deckName } = req.body;

    // Create session
    const sessionId = await sessionService.createSession(prompt, deckName);

    // Start watching for suggestions
    fileWatcher.watchSession(sessionId);

    // Spawn Claude Code CLI (non-blocking)
    claudeSpawner.spawnClaude(sessionId).catch(err => {
      console.error(`Failed to spawn Claude for ${sessionId}:`, err);
    });

    res.json({ sessionId });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Load existing session
router.get('/:sessionId', async (req, res) => {
  try {
    const { sessionId } = req.params;
    const suggestions = await sessionService.loadSession(sessionId);
    res.json({ suggestions });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// List all sessions
router.get('/', async (req, res) => {
  try {
    const sessions = await sessionService.listSessions();
    res.json({ sessions });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

export default router;
```

### WebSocket Integration

#### Main Server (`backend/src/index.ts`)

```typescript
import { Server } from 'socket.io';
import http from 'http';

const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "http://localhost:5173",
    methods: ["GET", "POST"]
  }
});

// WebSocket connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  socket.on('subscribe:session', (sessionId) => {
    socket.join(sessionId);
    console.log(`Client ${socket.id} subscribed to ${sessionId}`);
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Listen to file watcher events
fileWatcher.on('suggestion:new', ({ sessionId, suggestion }) => {
  io.to(sessionId).emit('suggestion:new', suggestion);
  console.log(`Sent suggestion to session ${sessionId}`);
});
```

---

## Claude Code Processing Logic

### Processing Script/Command

When spawned with `claude process-cards database/ai-sessions/{sessionId}`, Claude should:

1. **Read request.json**
   ```typescript
   const request = JSON.parse(
     fs.readFileSync('database/ai-sessions/{sessionId}/request.json', 'utf-8')
   );
   ```

2. **Load card data**
   ```typescript
   const deck = JSON.parse(
     fs.readFileSync(request.deckPath, 'utf-8')
   );
   const notes = deck.notes;
   ```

3. **Process cards based on prompt**
   - Analyze each card according to user prompt
   - Determine if changes are needed
   - Generate new field values

4. **Write suggestions incrementally**
   ```typescript
   for (const note of notes) {
     const changes = analyzeCard(note, request.prompt);

     if (Object.keys(changes).length > 0) {
       const suggestion = {
         noteId: note.noteId,
         original: note,
         changes,
         reasoning: generateReasoning(changes)
       };

       fs.writeFileSync(
         `database/ai-sessions/${sessionId}/suggestion-${note.noteId}.json`,
         JSON.stringify(suggestion, null, 2)
       );
     }
   }
   ```

### Batching Strategy

For large decks (5k+ cards):
- Process in chunks (e.g., 50 cards at a time)
- Write suggestions as they're generated (incremental)
- Allows frontend to start showing results immediately
- User doesn't wait for entire deck to complete

---

## Frontend Implementation

### New Hooks

#### 1. WebSocket Hook (`frontend/src/hooks/useWebSocket.ts`)

```typescript
import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';

export function useWebSocket(sessionId: string | null, onSuggestion: (suggestion: Suggestion) => void) {
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!sessionId) return;

    // Connect to WebSocket
    socketRef.current = io('http://localhost:3001');

    // Subscribe to session
    socketRef.current.emit('subscribe:session', sessionId);

    // Listen for suggestions
    socketRef.current.on('suggestion:new', onSuggestion);

    return () => {
      socketRef.current?.disconnect();
    };
  }, [sessionId, onSuggestion]);

  return socketRef.current;
}
```

#### 2. Session Management Hook (`frontend/src/hooks/useSessionManagement.ts`)

```typescript
export function useSessionManagement() {
  const [currentSession, setCurrentSession] = useState<string | null>(null);
  const [sessions, setSessions] = useState<string[]>([]);

  const createSession = async (prompt: string, deckName: string) => {
    const response = await fetch('http://localhost:3001/api/sessions/new', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt, deckName })
    });

    const { sessionId } = await response.json();
    setCurrentSession(sessionId);
    return sessionId;
  };

  const loadSession = async (sessionId: string) => {
    const response = await fetch(`http://localhost:3001/api/sessions/${sessionId}`);
    const { suggestions } = await response.json();
    setCurrentSession(sessionId);
    return suggestions;
  };

  const listSessions = async () => {
    const response = await fetch('http://localhost:3001/api/sessions');
    const { sessions } = await response.json();
    setSessions(sessions);
    return sessions;
  };

  return {
    currentSession,
    sessions,
    createSession,
    loadSession,
    listSessions
  };
}
```

### Updated Components

#### Update `useCardGeneration.ts`

Replace mock data generation (lines 38-64) with:

```typescript
const handleGenerateSuggestions = async () => {
  if (!selectedDeck || !prompt.trim()) return;

  setIsProcessing(true);
  setQueue([]);

  try {
    // Create new session
    const sessionId = await createSession(prompt, selectedDeck);

    // WebSocket will handle incoming suggestions
    // Queue updates automatically as suggestions arrive

  } catch (error) {
    console.error('Failed to generate suggestions:', error);
    // Show error notification
  }
};

// Handle real-time suggestions from WebSocket
const handleNewSuggestion = useCallback((suggestion: CardSuggestion) => {
  setQueue(prev => [...prev, suggestion]);
}, []);

// Set up WebSocket listener
useWebSocket(currentSession, handleNewSuggestion);
```

#### Update Queue Component

Add session selector:

```typescript
<div className="session-selector">
  <select onChange={(e) => loadSession(e.target.value)}>
    <option value="">Current Session</option>
    {sessions.map(sessionId => (
      <option key={sessionId} value={sessionId}>
        {sessionId}
      </option>
    ))}
  </select>
</div>
```

### History Tab Enhancements

**Two modes:**

1. **Current Session History**
   - Show accepted/rejected/skipped from current session only
   - Filtered by currentSession state

2. **All History**
   - Load all sessions from backend
   - Display modifications across all time
   - Better traceability

---

## Dependencies

### Backend

```json
{
  "dependencies": {
    "socket.io": "^4.7.0",
    "chokidar": "^3.6.0"
  }
}
```

### Frontend

```json
{
  "dependencies": {
    "socket.io-client": "^4.7.0"
  }
}
```

---

## Testing Plan

### Manual Testing Checklist

- [ ] Create new session → verify folder + request.json created
- [ ] Spawn Claude → verify suggestion files appear
- [ ] File watcher → verify backend detects new files
- [ ] WebSocket → verify frontend receives suggestions in real-time
- [ ] Queue updates → verify UI updates as suggestions arrive
- [ ] Load previous session → verify suggestions load correctly
- [ ] Session history → verify all sessions listed
- [ ] Large deck (5k+ cards) → verify streaming works smoothly

### Edge Cases

- [ ] User closes browser during processing → session persists
- [ ] Backend restarts → can resume watching existing sessions
- [ ] No suggestions generated → handle empty session gracefully
- [ ] Malformed suggestion file → error handling

---

## Performance Considerations

### Scalability

- **Large decks (20k+ cards)**:
  - Process in chunks
  - Stream suggestions incrementally
  - Frontend lazy-loads queue

- **Multiple concurrent sessions**:
  - Backend can handle multiple file watchers
  - WebSocket rooms isolate sessions
  - Claude spawns separate processes

### Optimization Opportunities

1. **Debouncing**: Batch multiple file events if suggestions written rapidly
2. **Caching**: Store processed sessions in memory
3. **Cleanup**: Auto-delete old sessions after X days
4. **Compression**: Gzip suggestion files for storage efficiency

---

## Future Enhancements

### Phase 3.1 - Progress Tracking
- Add `metadata.json` to track progress
- Update in real-time as cards processed
- Show percentage complete in UI

### Phase 3.2 - Prompt Templates
- Save frequently used prompts
- Quick-action buttons that populate prompt field
- Template variables ({{deckName}}, {{cardCount}})

### Phase 3.3 - Resume Capability
- Detect incomplete sessions
- Resume from last processed card
- Handle interruptions gracefully

---

## Success Criteria

- ✅ User can create new session from UI
- ✅ Backend creates session folder and spawns Claude
- ✅ Claude generates suggestions incrementally
- ✅ Frontend receives suggestions in real-time via WebSocket
- ✅ Queue updates as suggestions arrive
- ✅ User can load previous sessions
- ✅ History shows both current and all-time modifications
- ✅ System handles large decks (5k+) smoothly
- ✅ No API keys required (uses Claude Code subscription)

---

## References

- **Main Spec**: `SPEC.md` (lines 225-488) - AI Integration Strategy
- **TODO**: `TODO.md` - Phase 3 - Intelligence (AI Features)
- **Project Instructions**: `CLAUDE.md` - Server management and code practices
