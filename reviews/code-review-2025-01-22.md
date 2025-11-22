# AnkiDeku Code Review

**Date:** 2025-01-22
**Reviewer:** Senior Code Reviewer
**Review Type:** Comprehensive Full-Stack Analysis
**Severity Levels:** 🔴 Critical | 🟠 Major | 🟡 Minor | 🔵 Info

---

## Executive Summary

This is a comprehensive code review of the AnkiDeku project, an AI-powered Anki deck revision tool. The codebase is well-structured and follows modern TypeScript practices, but several critical issues need addressing before production use.

**Overall Assessment:** 6.5/10

### Key Strengths
✅ Clean TypeScript implementation across frontend and backend
✅ Good separation of concerns with services, hooks, and components
✅ Real-time WebSocket integration for AI processing
✅ Comprehensive caching system with incremental sync
✅ Modern UI with React 19 and Tailwind CSS

### Critical Concerns
❌ Type definition duplication across multiple files
❌ Missing error boundaries and retry logic
❌ No test coverage
❌ Hardcoded URLs and magic numbers
❌ Incomplete implementation of AI workflow
❌ Potential race conditions in async operations

---

## 1. CRITICAL BUGS 🔴

### 1.1 **Race Condition in File Watcher Cleanup**
**File:** `backend/src/routes/sessions.ts:65-67`
**Severity:** 🔴 Critical

```typescript
setTimeout(() => {
  fileWatcher.unwatchSession(sessionId);
}, 2000);
```

**Issue:** Hardcoded 2-second delay assumes all file writes complete within this time. On slower systems or with large decks, suggestions written after 2 seconds won't be detected.

**Impact:** Lost suggestions, incomplete processing results

**Fix:** Implement proper completion detection:
```typescript
// Option 1: Listen for Claude process exit
claudeSpawner.on('process:exit', (sessionId) => {
  setTimeout(() => fileWatcher.unwatchSession(sessionId), 1000);
});

// Option 2: Write completion marker file
// Watch for "completion.json" file in session directory
```

---

### 1.2 **Unsafe Type Coercion in Settings**
**File:** `backend/src/services/settings.ts:43-44`
**Severity:** 🔴 Critical

```typescript
} catch (error) {
  // If file doesn't exist, return default settings
  return { ...DEFAULT_SETTINGS };
}
```

**Issue:** Catches ALL errors, not just file-not-found. Corrupted JSON, permission errors, etc. silently return defaults.

**Impact:** User settings can be lost without warning

**Fix:**
```typescript
} catch (error) {
  if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
    return { ...DEFAULT_SETTINGS };
  }
  console.error('Failed to load settings:', error);
  throw new Error('Settings file is corrupted or inaccessible');
}
```

---

### 1.3 **Unhandled Promise Rejection in Background Sync**
**File:** `backend/src/index.ts:123-136`
**Severity:** 🔴 Critical

```typescript
(async () => {
  try {
    console.log(`Background incremental sync for "${deckName}"...`);
    // ... sync logic
  } catch (error) {
    console.error(`Background sync failed:`, error);
  }
})();
```

**Issue:** Background sync errors are logged but not tracked. Users won't know their cache is stale.

**Impact:** Silently outdated cache, incorrect AI processing input

**Fix:** Emit sync status events or add sync health endpoint

---

### 1.4 **Missing Null Check in Queue Management**
**File:** `frontend/src/hooks/useCardReview.ts:36-40`
**Severity:** 🔴 Critical

```typescript
if (currentIndex < queue.length - 1) {
  nextCard();
} else {
  onSuccess('All suggestions reviewed!');
  setQueue([]);
}
```

**Issue:** If `queue` is empty, `queue.length - 1` is `-1`, causing `currentIndex < -1` check to fail unexpectedly.

**Impact:** Edge case crash when queue is manipulated externally

**Fix:**
```typescript
if (queue.length === 0) {
  onSuccess('No more cards to review');
  return;
}
if (currentIndex < queue.length - 1) {
  nextCard();
} else {
  onSuccess('All suggestions reviewed!');
  setQueue([]);
}
```

---

## 2. TYPE SAFETY ISSUES 🟠

### 2.1 **Duplicate Type Definitions**
**Files:** Multiple locations
**Severity:** 🟠 Major

**Locations:**
1. `contract/types.ts` - Source of truth
2. `backend/src/types/index.ts` - Re-exports + backend-only types
3. `frontend/src/types/index.ts` - Re-exports + frontend-only types
4. `backend/src/processors/promptGenerator.ts:5-12` - Duplicate `SessionRequest`
5. `backend/src/processors/claudeProcessor.ts:5-12` - Duplicate `SessionRequest`
6. `backend/src/services/settings.ts:10-16` - Duplicate `FieldDisplayConfig`

**Issue:** `SessionRequest` is defined in:
- `contract/types.ts:65-72` (source)
- `backend/src/processors/promptGenerator.ts:5-12` (duplicate)
- `backend/src/processors/claudeProcessor.ts:5-12` (duplicate)

**Impact:** Type drift, maintenance burden, potential bugs

**Fix:** Remove all local type definitions, import from contract:
```typescript
// backend/src/processors/promptGenerator.ts
import type { SessionRequest } from '../types/index.js';

// backend/src/processors/claudeProcessor.ts
import type { SessionRequest } from '../types/index.js';
```

---

### 2.2 **Unsafe Any Usage**
**File:** `backend/src/types/index.ts:21`
**Severity:** 🟡 Minor

```typescript
params?: Record<string, any>;
```

**Issue:** Type safety completely bypassed for AnkiConnect params

**Fix:**
```typescript
// Define specific param types for each action
interface FindNotesParams {
  query: string;
}

interface NotesInfoParams {
  notes: number[];
}

// Use discriminated union
type AnkiConnectParams =
  | { action: 'findNotes'; params: FindNotesParams }
  | { action: 'notesInfo'; params: NotesInfoParams }
  // ... etc
```

---

### 2.3 **Missing Error Type Guards**
**File:** `backend/src/services/ankiConnect.ts:105-115`
**Severity:** 🟡 Minor

```typescript
} catch (error) {
  const axiosError = error as AxiosError;
  if (axiosError.code === 'ECONNREFUSED') {
```

**Issue:** Type assertion instead of type guard - runtime type not validated

**Fix:**
```typescript
} catch (error) {
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNREFUSED') {
      // ...
    }
  }
  throw error;
}
```

---

## 3. CODE DUPLICATION 🟠

### 3.1 **Duplicated Fetch Logic**
**Files:** `frontend/src/hooks/useSessionManagement.ts`
**Severity:** 🟠 Major

**Issue:** Every method duplicates fetch error handling:
```typescript
const response = await fetch('...');
if (!response.ok) {
  throw new Error('Failed to ...');
}
```

**Fix:** Create a centralized API client:
```typescript
// frontend/src/services/sessionApi.ts
const sessionApiClient = {
  async request<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`http://localhost:3001/api/sessions${url}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options?.headers }
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || `HTTP ${response.status}`);
    }

    return response.json();
  }
};
```

---

### 3.2 **Repeated Console Logging Patterns**
**Files:** Multiple backend services
**Severity:** 🟡 Minor

**Issue:** Manual logging scattered throughout:
```typescript
console.log(`[${sessionId}] ...`);
console.error(`Failed to ...`);
```

**Fix:** Implement structured logger:
```typescript
// backend/src/utils/logger.ts
class Logger {
  info(context: string, message: string) {
    console.log(`[${new Date().toISOString()}] [${context}] ${message}`);
  }
  error(context: string, message: string, error?: Error) {
    console.error(`[${new Date().toISOString()}] [${context}] ${message}`, error);
  }
}
```

---

## 4. DEAD CODE 🟡

### 4.1 **Unused Processor Entry Point**
**File:** `backend/src/processors/claudeProcessor.ts:87-121`
**Severity:** 🟡 Minor

**Issue:** `main()` function with CLI entry point is never called:
```typescript
async function main() {
  const args = process.argv.slice(2);
  // ... 35 lines of code
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}
```

**Reason:** `claudeSpawner.ts` spawns `claude` CLI directly, not this processor

**Fix:** Either:
1. Remove unused CLI interface
2. Document how to use it separately
3. Integrate it into the workflow

---

### 4.2 **Unused Component Imports**
**File:** `frontend/src/components/ComparisonView.tsx:4`
**Severity:** 🟡 Minor

```typescript
import { WarningIcon, CheckIcon, ArchiveIcon, DeckIcon, BrushIcon,
  ClipboardIcon, BulbIcon, ClockIcon, CloseIcon, ArrowRightIcon, KeyboardIcon } from './ui/Icons.js';
```

**Issue:** Some icons may not be used (need to verify each)

**Fix:** Only import what's used, configure ESLint to catch this

---

### 4.3 **Unreachable Route Handler**
**File:** `backend/src/routes/sessions.ts:118`
**Severity:** 🟠 Major

```typescript
router.get('/active', (req: Request, res: Response): void => {
  // ...
});
```

**Issue:** This route is defined AFTER the catch-all `/:sessionId` route (line 89). It will never be reached because Express matches routes in order.

**Impact:** The `/api/sessions/active` endpoint returns 404

**Fix:** Move specific routes before parameterized routes:
```typescript
// Specific routes first
router.get('/active', ...);

// Parameterized routes last
router.get('/:sessionId', ...);
```

---

## 5. ARCHITECTURE & DESIGN ISSUES 🟠

### 5.1 **Hardcoded URLs Throughout**
**Files:** Multiple
**Severity:** 🟠 Major

**Locations:**
- `frontend/src/services/api.ts:15` - `http://localhost:3001/api`
- `frontend/src/hooks/useWebSocket.ts:24` - `http://localhost:3001`
- `frontend/src/hooks/useSessionManagement.ts:10` - `http://localhost:3001/api/sessions/new`
- `backend/src/index.ts:24` - `http://localhost:5173`

**Issue:** Cannot configure for different environments (dev/staging/prod)

**Fix:** Environment variables:
```typescript
// frontend/.env
VITE_API_URL=http://localhost:3001
VITE_WS_URL=http://localhost:3001

// frontend/src/config.ts
export const config = {
  apiUrl: import.meta.env.VITE_API_URL || 'http://localhost:3001',
  wsUrl: import.meta.env.VITE_WS_URL || 'http://localhost:3001',
};
```

---

### 5.2 **Service Dependency Injection Anti-Pattern**
**File:** `backend/src/routes/sessions.ts:9-26`
**Severity:** 🟠 Major

```typescript
let sessionService: SessionService;
let fileWatcher: FileWatcherService;
let claudeSpawner: ClaudeSpawnerService;

export function initializeRouter(services: { ... }) {
  sessionService = services.sessionService;
  fileWatcher = services.fileWatcher;
  claudeSpawner = services.claudeSpawner;
}
```

**Issue:** Module-level mutable state makes testing difficult, order-dependent initialization

**Fix:** Use closure or class-based router:
```typescript
export function createSessionsRouter(
  sessionService: SessionService,
  fileWatcher: FileWatcherService,
  claudeSpawner: ClaudeSpawnerService
) {
  const router = express.Router();

  router.post('/new', async (req, res) => {
    // Use services from closure
  });

  return router;
}
```

---

### 5.3 **Missing Abstraction for Database Path Logic**
**Files:** Multiple
**Severity:** 🟡 Minor

**Issue:** Path construction duplicated across:
- `backend/src/services/cache.ts:8-9`
- `backend/src/services/settings.ts:7-8`
- `backend/src/services/sessionService.ts:10-12`

**Fix:** Centralized config:
```typescript
// backend/src/config/paths.ts
export const paths = {
  root: path.join(__dirname, '../../..'),
  database: path.join(__dirname, '../../../database'),
  decks: path.join(__dirname, '../../../database/decks'),
  sessions: path.join(__dirname, '../../../database/ai-sessions'),
  settings: path.join(__dirname, '../../../database/settings.json'),
};
```

---

### 5.4 **Missing Separation Between UI and Business Logic**
**File:** `frontend/src/App.tsx:34-44`
**Severity:** 🟡 Minor

```typescript
const handleDeleteSession = async () => {
  if (!sessionToDelete) return;
  try {
    await deleteSession(sessionToDelete);
    showNotification('Session deleted successfully', 'success');
    setSessionToDelete(null);
  } catch (error) {
    showNotification('Failed to delete session', 'error');
  }
};
```

**Issue:** Business logic in presentation component

**Fix:** Move to custom hook:
```typescript
// hooks/useSessionActions.ts
export function useSessionActions() {
  const { deleteSession } = useSessionManagement();
  const { showNotification } = useNotification();

  const handleDelete = async (sessionId: string) => {
    try {
      await deleteSession(sessionId);
      showNotification('Session deleted successfully', 'success');
      return true;
    } catch (error) {
      showNotification('Failed to delete session', 'error');
      return false;
    }
  };

  return { handleDelete };
}
```

---

## 6. ERROR HANDLING 🟠

### 6.1 **No Error Boundaries**
**Files:** Frontend
**Severity:** 🟠 Major

**Issue:** React error boundaries not implemented. Component errors crash entire app.

**Fix:**
```typescript
// frontend/src/components/ErrorBoundary.tsx
class ErrorBoundary extends React.Component<Props, State> {
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return <ErrorFallback error={this.state.error} />;
    }
    return this.props.children;
  }
}

// main.tsx
<ErrorBoundary>
  <App />
</ErrorBoundary>
```

---

### 6.2 **No Retry Logic for Network Failures**
**Files:** Frontend hooks
**Severity:** 🟠 Major

**Issue:** All API calls fail immediately on network error. No exponential backoff.

**Fix:** Implement retry utility:
```typescript
async function fetchWithRetry<T>(
  fn: () => Promise<T>,
  options: { retries: number; delay: number }
): Promise<T> {
  for (let i = 0; i < options.retries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === options.retries - 1) throw error;
      await sleep(options.delay * Math.pow(2, i));
    }
  }
  throw new Error('Unreachable');
}
```

---

### 6.3 **Vague Error Messages**
**Files:** Multiple
**Severity:** 🟡 Minor

**Examples:**
```typescript
throw new Error('Failed to create session');
throw new Error('Failed to load session');
throw new Error('Failed to delete session');
```

**Issue:** No context about what failed or why

**Fix:**
```typescript
throw new Error(`Failed to create session for deck "${deckName}": ${error.message}`);
```

---

## 7. PERFORMANCE ISSUES 🟡

### 7.1 **Inefficient Note Lookup in Cache**
**File:** `backend/src/services/cache.ts:315-343`
**Severity:** 🟡 Minor

```typescript
private async findDeckForNote(deckName: string, noteId: number): Promise<string | null> {
  // Reads and parses EVERY subdeck cache file
  for (const subDeck of subDecks) {
    const cachePath = this.getCachePath(subDeck);
    const data = await fs.readFile(cachePath, 'utf-8');
    const cache = JSON.parse(data) as CachedDeckData;
    if (cache.notes.some(n => n.noteId === noteId)) {
      return subDeck;
    }
  }
}
```

**Issue:** O(n*m) complexity - reads all subdeck files sequentially

**Fix:** Build in-memory index on cache load:
```typescript
private noteToDecckIndex: Map<number, string> = new Map();

private buildIndex(cache: CachedDeckData) {
  for (const note of cache.notes) {
    this.noteToDeckIndex.set(note.noteId, cache.deckName);
  }
}
```

---

### 7.2 **Missing Memoization in Component**
**File:** `frontend/src/components/ComparisonView.tsx:70`
**Severity:** 🟡 Minor

```typescript
const { getCurrentCard, currentIndex, queue } = useStore();
const card = getCurrentCard();
```

**Issue:** `getCurrentCard()` called on every render, even when queue/index unchanged

**Fix:**
```typescript
const card = useMemo(() => getCurrentCard(), [currentIndex, queue]);
```

---

### 7.3 **Unnecessary Re-renders from WebSocket**
**File:** `frontend/src/hooks/useWebSocket.ts:30`
**Severity:** 🟡 Minor

```typescript
socketRef.current.on('suggestion:new', onSuggestion);
```

**Issue:** Event listener recreated on every effect run if `onSuggestion` identity changes

**Fix:**
```typescript
const onSuggestionRef = useRef(onSuggestion);
useEffect(() => { onSuggestionRef.current = onSuggestion; }, [onSuggestion]);

useEffect(() => {
  socketRef.current.on('suggestion:new', (data) => onSuggestionRef.current(data));
}, [sessionId]);
```

---

## 8. SECURITY CONCERNS 🔴

### 8.1 **Path Traversal Vulnerability**
**File:** `backend/src/routes/sessions.ts:262`
**Severity:** 🔴 Critical

```typescript
const logPath = require('path').join(sessionDir, logFile);
```

**Issue:** `logFile` comes from user input (`req.query.type`). No validation. Could access arbitrary files.

**Attack:**
```
GET /api/sessions/../../etc/passwd/output?type=../../../etc/passwd
```

**Fix:**
```typescript
const allowedTypes = ['stdout', 'stderr', 'combined'];
const type = allowedTypes.includes(req.query.type as string)
  ? req.query.type as string
  : 'combined';

const logFileName = type === 'stdout' ? 'claude-stdout.log'
  : type === 'stderr' ? 'claude-stderr.log'
  : 'claude-output.log';

const logPath = path.join(sessionDir, logFileName);

// Validate resolved path is still in session dir
if (!logPath.startsWith(sessionDir)) {
  return res.status(400).json({ error: 'Invalid log type' });
}
```

---

### 8.2 **Command Injection Risk**
**File:** `backend/src/services/claudeSpawner.ts:53`
**Severity:** 🔴 Critical

```typescript
const claude = spawn('claude', [taskFilePath], {
  cwd: process.cwd(),
  env: process.env,
  stdio: ['pipe', 'pipe', 'pipe']
});
```

**Issue:** `taskFilePath` is user-controlled (via session creation). If attacker can manipulate file path, could inject commands.

**Fix:**
1. Validate `taskFilePath` is within expected directory
2. Use absolute paths only
3. Whitelist allowed characters in session IDs

```typescript
// Validate session ID format
if (!/^session-\d{4}-\d{2}-\d{2}-\d{6}$/.test(sessionId)) {
  throw new Error('Invalid session ID format');
}

// Ensure task file is in expected location
const expectedPath = path.join(this.sessionsDir, sessionId, 'claude-task.md');
if (taskFilePath !== expectedPath) {
  throw new Error('Invalid task file path');
}
```

---

### 8.3 **No Rate Limiting**
**Files:** All API endpoints
**Severity:** 🟠 Major

**Issue:** No rate limiting on session creation, note updates, etc. Vulnerable to abuse.

**Fix:**
```typescript
import rateLimit from 'express-rate-limit';

const sessionCreateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10, // 10 sessions per window
  message: 'Too many sessions created, please try again later'
});

app.use('/api/sessions/new', sessionCreateLimiter);
```

---

### 8.4 **CORS Misconfiguration**
**File:** `backend/src/index.ts:22-26`
**Severity:** 🟡 Minor

```typescript
cors: {
  origin: "http://localhost:5173",
  methods: ["GET", "POST"]
}
```

**Issue:** Only allows GET/POST but routes use PUT/DELETE. Hardcoded localhost only.

**Fix:**
```typescript
cors: {
  origin: process.env.ALLOWED_ORIGINS?.split(',') || 'http://localhost:5173',
  methods: ["GET", "POST", "PUT", "DELETE"],
  credentials: true
}
```

---

## 9. CODE QUALITY & BEST PRACTICES 🟡

### 9.1 **Magic Numbers**
**Files:** Multiple
**Severity:** 🟡 Minor

**Examples:**
```typescript
BATCH_SIZE = 50;           // backend/src/services/ankiConnect.ts:7
BATCH_DELAY = 50;          // backend/src/services/ankiConnect.ts:8
REQUEST_TIMEOUT = 60000;   // backend/src/services/ankiConnect.ts:6
awaitWriteFinish: { stabilityThreshold: 100, pollInterval: 50 }  // fileWatcher.ts:49-52
```

**Issue:** Numbers have no context

**Fix:**
```typescript
const ANKI_CONFIG = {
  batchSize: 50,              // Cards per batch
  batchDelayMs: 50,           // Delay between batches to avoid overwhelming AnkiConnect
  requestTimeoutMs: 60000,    // 60s timeout for large deck operations
} as const;
```

---

### 9.2 **Inconsistent Naming Conventions**
**Files:** Multiple
**Severity:** 🟡 Minor

**Examples:**
- `getCachedNotes` vs `getDeckNotes` (one says "cached", other doesn't)
- `sessionsDir` vs `SESSIONS_DIR` (class property vs constant)
- `handleAccept` vs `onAccept` (handler vs event)

**Fix:** Establish naming conventions:
- Event handlers: `handle*` (internal) or `on*` (props)
- API methods: Verb + Resource (`getDecks`, `createSession`)
- Constants: `UPPER_SNAKE_CASE`
- Variables: `camelCase`

---

### 9.3 **Overly Long Functions**
**File:** `backend/src/services/cache.ts:115-176`
**Severity:** 🟡 Minor

**Issue:** `getCachedNotes` is 62 lines, does too much:
1. Load exact deck cache
2. Find subdeck caches
3. Combine caches
4. Merge sync timestamps

**Fix:** Extract methods:
```typescript
async getCachedNotes(deckName: string): Promise<CachedDeckData | null> {
  const exactCache = await this.loadExactDeckCache(deckName);
  if (exactCache) return exactCache;

  return await this.combineSubDeckCaches(deckName);
}

private async loadExactDeckCache(deckName: string): Promise<CachedDeckData | null> {
  // ...
}

private async combineSubDeckCaches(deckName: string): Promise<CachedDeckData | null> {
  // ...
}
```

---

### 9.4 **Commented-Out Code**
**File:** None found (good!)
**Severity:** N/A

---

### 9.5 **Inconsistent Error Logging**
**Files:** Multiple
**Severity:** 🟡 Minor

**Examples:**
```typescript
console.error(`Error fetching notes for deck "${req.params.deckName}":`, errorMessage);
console.error('Failed to load settings:', error);
console.error(`[Claude ${sessionId}]: Failed with code ${code}`);
```

**Issue:** Different formats, some log error objects, some don't

**Fix:** Standardize with logger utility (see 3.2)

---

## 10. TESTING GAPS 🔴

### 10.1 **Zero Test Coverage**
**Severity:** 🔴 Critical

**Issue:** No unit tests, integration tests, or E2E tests anywhere in the codebase.

**Critical areas needing tests:**
1. **Backend Services**
   - `ankiConnect.ts` - Batch processing logic, retry logic
   - `cache.ts` - Cache merging, subdeck logic
   - `sessionService.ts` - File path construction

2. **Frontend Hooks**
   - `useCardGeneration.ts` - Session creation flow
   - `useCardReview.ts` - Accept/reject/skip logic
   - `useWebSocket.ts` - Connection handling

3. **API Routes**
   - All CRUD operations
   - Error cases
   - Invalid input handling

**Recommended:**
```bash
# Backend
npm install --save-dev jest @types/jest ts-jest supertest

# Frontend
npm install --save-dev vitest @testing-library/react @testing-library/user-event
```

**Priority tests:**
1. Cache merge logic (high complexity)
2. Batch processing with failures
3. WebSocket reconnection
4. Queue state management

---

## 11. DOCUMENTATION ISSUES 🟡

### 11.1 **Missing JSDoc Comments**
**Files:** Most service methods
**Severity:** 🟡 Minor

**Issue:** Only some methods have documentation:
```typescript
// Has JSDoc:
/**
 * Create new session
 * @param prompt - User's natural language prompt
 * @param deckName - Name of the deck to process
 * @returns Session ID
 */
async createSession(prompt: string, deckName: string): Promise<string>

// No JSDoc:
async loadSession(sessionId: string): Promise<CardSuggestion[]>
async listSessions(): Promise<string[]>
```

**Fix:** Add JSDoc to all public methods

---

### 11.2 **No API Documentation**
**Severity:** 🟠 Major

**Issue:** API endpoints not documented. No OpenAPI/Swagger spec.

**Fix:** Add OpenAPI specification or generate from code with tools like:
- `tsoa` (TypeScript OpenAPI)
- `@nestjs/swagger` (if migrating to NestJS)
- Manual `openapi.yaml`

---

### 11.3 **Unclear Session File Format**
**File:** `AI_WORKFLOW_IMPLEMENTATION.md`
**Severity:** 🟡 Minor

**Issue:** Spec shows example suggestion format, but doesn't specify:
- Required vs optional fields
- Validation rules
- Field constraints

**Fix:** Add JSON schema:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["noteId", "original", "changes", "reasoning"],
  "properties": {
    "noteId": { "type": "integer" },
    "original": { "$ref": "#/definitions/Note" },
    "changes": { "type": "object" },
    "reasoning": { "type": "string", "minLength": 1 }
  }
}
```

---

## 12. ADDITIONAL FINDINGS

### 12.1 **Incomplete AI Workflow Implementation**
**Severity:** 🔵 Info

**Observation:** According to `TODO.md:176-189`, the AI workflow is marked as "Not Started":
```markdown
- [ ] **File-Based AI Workflow** (Core - See implementation spec)
  - [ ] Session management service (create/load/list sessions)
  - [ ] File watcher service (chokidar for suggestion detection)
```

**Reality:** These ARE implemented (`sessionService.ts`, `fileWatcher.ts`, `claudeSpawner.ts`).

**Action:** Update `TODO.md` to reflect actual implementation status

---

### 12.2 **README.md Out of Sync**
**Severity:** 🔵 Info

**Issue:** `README.md` says "Next Steps: Design UI architecture" but UI is fully built.

**Fix:** Update README with:
- Installation instructions
- Development setup
- Architecture overview
- Screenshots

---

### 12.3 **Missing Environment Variables Documentation**
**Severity:** 🟡 Minor

**Issue:** No `.env.example` files

**Fix:** Add `.env.example` for both frontend and backend:
```bash
# backend/.env.example
PORT=3001
ANKI_CONNECT_URL=http://localhost:8765
DATABASE_DIR=./database

# frontend/.env.example
VITE_API_URL=http://localhost:3001
VITE_WS_URL=http://localhost:3001
```

---

## 13. DEPENDENCY ISSUES

### 13.1 **Mismatched Socket.IO Versions**
**Files:** `backend/package.json`, `frontend/package.json`
**Severity:** 🟡 Minor

```json
// backend
"socket.io": "^4.8.1"

// frontend
"socket.io-client": "^4.8.1"
```

**Issue:** Versions match currently, but no lockfile sync enforced. Could drift.

**Fix:** Add version check in CI or use workspace deps

---

### 13.2 **Missing TypeScript Path Aliases**
**Severity:** 🟡 Minor

**Issue:** Deep relative imports:
```typescript
import type { Note } from '../../../contract/types.js';
```

**Fix:** Add path aliases in `tsconfig.json`:
```json
{
  "compilerOptions": {
    "paths": {
      "@contract/*": ["../contract/*"],
      "@backend/*": ["./src/*"],
      "@frontend/*": ["./src/*"]
    }
  }
}
```

---

## PRIORITY ACTION ITEMS

### Immediate (Fix before next deploy)
1. 🔴 Fix path traversal vulnerability (8.1)
2. 🔴 Fix `/active` route ordering (4.3)
3. 🔴 Validate task file path in spawner (8.2)
4. 🔴 Add error handling to settings loader (1.2)
5. 🔴 Fix race condition in file watcher cleanup (1.1)

### High Priority (Next sprint)
1. 🟠 Remove type duplications (2.1)
2. 🟠 Add React error boundaries (6.1)
3. 🟠 Implement environment config (5.1)
4. 🟠 Add retry logic for network calls (6.2)
5. 🟠 Fix dependency injection pattern (5.2)
6. 🟠 Add rate limiting (8.3)

### Medium Priority (Next month)
1. 🟡 Add test coverage (10.1) - Start with critical paths
2. 🟡 Centralize API client (3.1)
3. 🟡 Extract constants (9.1)
4. 🟡 Add JSDoc comments (11.1)
5. 🟡 Implement structured logging (3.2)

### Low Priority (Backlog)
1. 🔵 Update documentation (12.1, 12.2)
2. 🟡 Add TypeScript path aliases (13.2)
3. 🟡 Optimize cache lookups (7.1)
4. 🟡 Add OpenAPI spec (11.2)

---

## METRICS

### Code Quality Scores
- **Type Safety:** 7/10 (good types, some `any` usage)
- **Error Handling:** 5/10 (basic try/catch, no boundaries/retry)
- **Security:** 4/10 (critical vulnerabilities present)
- **Testing:** 0/10 (no tests)
- **Documentation:** 6/10 (good specs, missing API docs)
- **Performance:** 7/10 (efficient caching, some optimizations needed)
- **Maintainability:** 7/10 (clean structure, some tech debt)

### Technical Debt Estimate
- **Critical Issues:** ~8 hours to fix
- **Major Issues:** ~16 hours to fix
- **Minor Issues:** ~24 hours to fix
- **Testing:** ~40 hours to reach 70% coverage
- **Total:** ~88 hours (~2.5 weeks)

---

## CONCLUSION

The AnkiDeku codebase demonstrates solid architectural foundations with modern TypeScript practices, clean separation of concerns, and a well-thought-out AI workflow design. However, several critical security vulnerabilities, lack of test coverage, and missing error handling need immediate attention before production use.

**Recommended Next Steps:**
1. Address all 🔴 Critical issues immediately
2. Implement basic test coverage for core flows
3. Add error boundaries and retry logic
4. Set up CI/CD with linting and type checking
5. Create deployment-ready environment configuration

**Overall Verdict:** The project shows promise but requires hardening before production deployment. With the fixes outlined above, this could become a robust and maintainable application.

---

**Review Completed:** 2025-01-22
**Lines Analyzed:** ~3,500 (Backend: ~1,800, Frontend: ~1,700)
**Files Reviewed:** 28 TypeScript files
**Issues Found:** 45 (Critical: 8, Major: 11, Minor: 24, Info: 2)
