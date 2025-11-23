# AnkiDeku Development TODO

**Last Updated**: 2025-11-23

---

## Phase 1 - MVP (Core Functionality)

### ✅ Completed

- [x] **Project Setup**
  - [x] Initialize Node.js backend with Express
  - [x] Initialize React frontend with Vite + Tailwind CSS
  - [x] Configure project structure (backend/, frontend/, database/)
  - [x] Install dependencies (axios, cors, zustand, diff-match-patch)

- [x] **AnkiConnect Service Layer**
  - [x] Create AnkiConnect API wrapper (backend/src/services/ankiConnect.js)
  - [x] Implement connection health check (ping)
  - [x] Implement getDeckNames and getDeckNamesAndIds
  - [x] Implement findNotes and notesInfo
  - [x] Implement updateNoteFields
  - [x] Implement batchUpdateNotes for bulk operations
  - [x] Error handling for connection failures

- [x] **Backend API Routes**
  - [x] GET /api/health - Health check
  - [x] GET /api/anki/ping - Check AnkiConnect connection
  - [x] GET /api/decks - Get all decks with IDs
  - [x] GET /api/decks/:deckName/notes - Get all notes from deck
  - [x] PUT /api/notes/:noteId - Update single note
  - [x] POST /api/notes/batch-update - Batch update notes

- [x] **State Management (Zustand)**
  - [x] Connection state (ankiConnected)
  - [x] Deck selection state
  - [x] Prompt input and history
  - [x] Processing state and progress tracking
  - [x] Queue management (suggestions only)
  - [x] Current card index
  - [x] Actions history

- [x] **UI Components - Initial**
  - [x] DeckSelector component (with connection error handling)
  - [x] Queue component (sidebar with progress)
  - [x] Main App layout (header + two-view structure)
  - [x] Chat-based AI Assistant sidebar
  - [x] Session selector UI

- [x] **Comparison View Component**
  - [x] Display original vs suggested side-by-side
  - [x] Implement field-by-field diff highlighting
  - [x] Show only changed fields (collapse unchanged)
  - [x] Display metadata (deck, note type, last modified)
  - [x] Character-level diff with colors (red=delete, green=add)
  - [x] AI reasoning display panel
  - [x] Edit mode with manual field editing
  - [x] Toggle between original AI suggestions and edited versions
  - [x] Auto-save edited changes to backend (debounced)
  - [x] Preserve edits when accepting/rejecting cards

- [x] **Action Buttons & Logic**
  - [x] Accept button (apply suggestion + move to next)
  - [x] Reject button (discard + move to next)
  - [x] Skip button (defer to end of queue)
  - [x] Success/error notifications (toast system)
  - [x] Update Anki via API on accept
  - [x] Track actions in history
  - [x] Remove accepted/rejected cards from queue

- [x] **TypeScript Conversion**
  - [x] Convert backend to TypeScript (.js → .ts)
  - [x] Create type definitions for backend (types/index.ts)
  - [x] Convert frontend to TypeScript (.jsx → .tsx)
  - [x] Create type definitions for frontend (types/index.ts)
  - [x] Add TypeScript config files (tsconfig.json)
  - [x] Update imports and fix type errors
  - [x] Test compilation and runtime with TypeScript
  - [x] Shared type contract (contract/types.ts)

- [x] **Filesystem Cache System**
  - [x] Implement cache service (backend/src/services/cache.ts)
  - [x] Cache deck notes to JSON files (database/decks/)
  - [x] Cache-first loading strategy
  - [x] Manual sync endpoint for refreshing cache
  - [x] Auto-update cache when accepting changes
  - [x] Batch fetching from Anki (100 cards at a time)
  - [x] Background incremental sync using edited:N query
  - [x] Hierarchical caching with sub-deck support

- [x] **UI Redesign - Single Page Layout**
  - [x] Remove welcome/setup page
  - [x] Create chat-based AI Assistant sidebar (Sidebar.tsx)
  - [x] Deck selector integrated into sidebar
  - [x] Processing progress display in sidebar
  - [x] Multiline textarea with Shift+Enter for new line
  - [x] Hideable sidebar with toggle button
  - [x] Dark theme support with persistent preference

- [x] **Queue Enhancement**
  - [x] Two tabs: Queue and History
  - [x] Search functionality for both tabs
  - [x] Color-coded history items (green=accept, red=reject, gray=skip)
  - [x] Display field configuration per note type
  - [x] Click to navigate to specific card
  - [x] Show all queue items (not just upcoming)
  - [x] Auto-scroll to current item
  - [x] Progress tracking based on actual reviewed count

- [x] **Settings System**
  - [x] Settings modal component with field display config
  - [x] Backend settings service (backend/src/services/settings.ts)
  - [x] Settings persistence in database/settings.json
  - [x] GET /api/settings endpoint
  - [x] PUT /api/settings/field-display endpoint
  - [x] Compact dropdown UI for field selection
  - [x] Auto-sync settings when modal opens

- [x] **Database Structure**
  - [x] Rename cache/ directory to database/
  - [x] database/decks/ for cached notes
  - [x] database/settings.json for user settings
  - [x] database/ai-sessions/ for AI processing sessions

### ⏳ Pending

- [ ] **Connection Monitoring**
  - [ ] Periodic health check (every 10s)
  - [ ] Alert banner if AnkiConnect disconnects
  - [ ] Auto-reconnect on service recovery
  - [ ] Concurrent edit detection (check modified timestamp)
  - [ ] Force update confirmation dialog

---

## Phase 2 - Polish (UX Improvements)

### ⏳ Not Started

- [ ] **Revision History Viewer**
  - [ ] Timeline view of all changes
  - [ ] Filter by date/deck/type
  - [ ] Revert individual change
  - [ ] Revert entire session
  - [ ] Export as CSV/JSON

- [ ] **Progress Indicators**
  - [ ] Loading states with skeleton screens
  - [ ] Estimated time remaining
  - [ ] Better cancellation UX

---

## Phase 3 - Intelligence (AI Features)

**📖 See detailed implementation spec**: `AI_WORKFLOW_IMPLEMENTATION.md`

### ✅ Completed

- [x] **File-Based AI Workflow** (Core)
  - [x] Session management service (create/load/list/delete sessions)
  - [x] File watcher service (chokidar for suggestion detection)
  - [x] Claude spawner service (spawn CLI process in background)
  - [x] WebSocket integration (socket.io for real-time updates)
  - [x] Session API routes (POST /new, GET /:id, GET /, DELETE /:id)
  - [x] Frontend WebSocket hook (useWebSocket)
  - [x] Session management hook (useSessionManagement)
  - [x] Real-time queue updates from WebSocket
  - [x] Session selector UI component
  - [x] Update useCardGeneration to use real API

- [x] **Session Management**
  - [x] Create new session with prompt and deck
  - [x] Load existing session with suggestions
  - [x] List all sessions with metadata
  - [x] Delete session and cleanup files
  - [x] Cancel running session
  - [x] Session status endpoint
  - [x] Output log viewer (stdout/stderr/combined)
  - [x] Session chat history in sidebar
  - [x] "New Session" button to clear and start fresh
  - [x] "Back to Sessions" navigation

- [x] **Prompt Generation**
  - [x] Generate Claude task prompt from user input
  - [x] Include deck context and card count
  - [x] Write prompt to claude-task.md
  - [x] Pass deck file paths for processing

- [x] **Real-time Processing**
  - [x] Watch for new suggestion files
  - [x] Emit WebSocket events on new suggestions
  - [x] Update queue in real-time as suggestions arrive
  - [x] Progress tracking during processing
  - [x] Session completion notification
  - [x] Auto-select first suggestion when it arrives (UX improvement)
  - [x] Prevent auto-selection when viewing history items

- [x] **Session Directory Structure**
  - [x] database/ai-sessions/{sessionId}/
  - [x] request.json - Original prompt and metadata
  - [x] claude-task.md - Generated task for Claude
  - [x] suggestions/ - Individual suggestion files
  - [x] logs/ - Claude output logs (stdout, stderr, combined)

### ⏳ Pending

- [ ] **History Tab Enhancement**
  - [ ] Toggle between "Current Session" and "All History"
  - [ ] Display session metadata (timestamp, prompt, deck)
  - [ ] Session filtering and search

- [ ] **Phase 3.1 - Progress Tracking** (Future)
  - [ ] Add metadata.json to track detailed progress
  - [ ] Real-time progress percentage in UI
  - [ ] Better estimated time remaining

- [ ] **Phase 3.2 - Prompt Templates** (Future)
  - [ ] Save custom prompts
  - [ ] Quick-action buttons ("Fix typos", "Improve", etc.)
  - [ ] Template variables (e.g., {{deckName}}, {{cardCount}})
  - [ ] Import/export prompt library

- [ ] **Phase 3.3 - Resume Capability** (Future)
  - [ ] Detect incomplete sessions
  - [ ] Resume from last processed card
  - [ ] Handle interruptions gracefully

---

## Phase 4 - Advanced Features (Future)

### 💡 Ideas for Later

- [ ] **Document Context**
  - [ ] Upload PDF/TXT/DOCX files
  - [ ] Extract text for AI context
  - [ ] Reference documents in prompts
  - [ ] Store in context/ directory

- [ ] **Card Generation**
  - [ ] Generate new cards from uploaded documents
  - [ ] Preview before adding to Anki
  - [ ] Bulk import

- [ ] **Performance Optimization**
  - [ ] Optimize for 20k+ card decks
  - [ ] Lazy loading in queue
  - [ ] Virtual scrolling
  - [ ] Web worker for diff processing

- [ ] **Multi-Session History**
  - [ ] Global history view across all sessions
  - [ ] Per-session history toggle
  - [ ] History persistence and export

---

## Known Issues

- [ ] Node.js version warning (v20.18.0 vs required v20.19+) - not critical, app works

---

## Notes

- **Backend runs on**: http://localhost:3001 (TypeScript)
- **Frontend runs on**: http://localhost:5173 (TypeScript)
- **AnkiConnect**: http://localhost:8765
- **Current stage**: Phase 3 AI integration complete! Full real-time AI workflow operational with edit mode.
- **Next priorities**: Connection monitoring, prompt templates, progress tracking enhancements
- **Latest update** (2025-11-23):
  - ✅ **Edit Mode Implementation**
    - Manual field editing in comparison view
    - Toggle between original AI suggestions and edited versions
    - Auto-save edited changes to backend (debounced, 1s delay)
    - Preserve edits when accepting/rejecting cards
    - Warning indicator when viewing AI suggestion with manual edits
    - Disable actions when viewing AI suggestion (must switch to edited version first)
  - ✅ **UX Improvements**
    - Auto-select first suggestion when it arrives in empty queue
    - Prevent auto-selection interruption when viewing history
    - Removed keyboard shortcut hints from UI (cleaner interface)
  - ✅ **History Enhancement**
    - Click history items to view readonly comparison
    - Proper handling of edited changes in history
    - Timestamp display for accepted/rejected cards
- **Previous update** (2025-11-22):
  - ✅ **Phase 3 AI Integration COMPLETE!**
    - Real-time AI processing with Claude Code CLI
    - WebSocket-based live suggestion updates
    - Session management (create/load/delete/cancel)
    - File watcher for incremental suggestion detection
    - Output log viewer for debugging
  - ✅ **Session UI Enhancements**
    - Session chat history display in sidebar
    - Load previous sessions with context restoration
    - "New Session" button to start fresh
    - "Back to Sessions" navigation
    - Session selector dropdown in queue
  - ✅ **Queue Improvements**
    - Fixed: Show all items instead of hiding reviewed ones
    - Fixed: Progress tracking uses actual reviewed count
    - Fixed: Accept/Reject now properly removes cards from queue
    - Added: Auto-scroll to current item
    - Added: Click any item to jump to it
  - ✅ **UI Polish**
    - Dark theme support throughout
    - Improved session list cards with timestamps
    - Output viewer modal for Claude logs
    - Better session metadata display
- **Previous updates**:
  - ✅ UI redesigned to single-page layout with chat-based sidebar
  - ✅ Queue enhanced with tabs (Queue/History) and search
  - ✅ Settings system for field display configuration
  - ✅ Complete TypeScript conversion (backend + frontend)
  - ✅ Filesystem cache system for fast deck loading
  - ✅ Hierarchical caching with sub-deck support
  - ✅ Background incremental sync using Anki's edited:N query
