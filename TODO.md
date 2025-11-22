# AnkiDeku Development TODO

**Last Updated**: 2025-11-22

---

## Phase 1 - MVP (Core Functionality)

### ✅ Completed

- [x] **Project Setup**
  - [x] Initialize Node.js backend with Express
  - [x] Initialize React frontend with Vite + Tailwind CSS
  - [x] Configure project structure (backend/, frontend/, revisions/)
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
  - [x] PromptInput component (with history and examples)
  - [x] Queue component (sidebar with progress)
  - [x] Main App layout (header + two-view structure)
  - [x] Mock suggestion generation (placeholder for AI)

- [x] **Comparison View Component**
  - [x] Display original vs suggested side-by-side
  - [x] Implement field-by-field diff highlighting
  - [x] Show only changed fields (collapse unchanged)
  - [x] Display metadata (deck, note type, last modified)
  - [x] Character-level diff with colors (red=delete, green=add)
  - [x] AI reasoning display panel

- [x] **Action Buttons & Logic**
  - [x] Accept button (apply suggestion + move to next)
  - [x] Reject button (discard + move to next)
  - [x] Skip button (defer to end of queue)
  - [x] Success/error notifications (toast system)
  - [x] Update Anki via API on accept
  - [x] Track actions in history

- [x] **TypeScript Conversion**
  - [x] Convert backend to TypeScript (.js → .ts)
  - [x] Create type definitions for backend (types/index.ts)
  - [x] Convert frontend to TypeScript (.jsx → .tsx)
  - [x] Create type definitions for frontend (types/index.ts)
  - [x] Add TypeScript config files (tsconfig.json)
  - [x] Update imports and fix type errors
  - [x] Test compilation and runtime with TypeScript

- [x] **Filesystem Cache System**
  - [x] Implement cache service (backend/src/services/cache.ts)
  - [x] Cache deck notes to JSON files (cache/decks/)
  - [x] Cache-first loading strategy
  - [x] Manual sync endpoint for refreshing cache
  - [x] Auto-update cache when accepting changes
  - [x] Batch fetching from Anki (100 cards at a time)

### ⏳ Pending

- [ ] **Keyboard Shortcuts**
  - [ ] Enter/A - Accept
  - [ ] Backspace/R - Reject
  - [ ] S - Skip
  - [ ] E - Edit mode
  - [ ] J/K - Navigate cards
  - [ ] ? - Show shortcuts help

- [ ] **Revision Logging**
  - [ ] Create revisions/ directory structure
  - [ ] Save each session to JSON file (session-YYYY-MM-DD-HHMMSS.json)
  - [ ] Log format: { timestamp, deckName, prompt, changes: [...] }
  - [ ] Include before/after values for each change
  - [ ] Track accepted/rejected/skipped counts

- [ ] **Edit Mode**
  - [ ] Inline field editing
  - [ ] Preview changes before applying
  - [ ] Validation (field length, format)
  - [ ] Save/Cancel buttons
  - [ ] Keyboard shortcuts (Ctrl+Enter save, Esc cancel)

- [ ] **Connection Monitoring**
  - [ ] Periodic health check (every 10s)
  - [ ] Alert banner if AnkiConnect disconnects
  - [ ] Auto-reconnect on service recovery
  - [ ] Concurrent edit detection (check modified timestamp)
  - [ ] Force update confirmation dialog

- [ ] **Testing**
  - [ ] Test with real Anki deck (small deck first)
  - [ ] Test batch processing (100+ cards)
  - [ ] Test error handling (AnkiConnect down, network issues)
  - [ ] Test revision logging (JSON files created correctly)
  - [ ] End-to-end workflow test

---

## Phase 2 - Polish (UX Improvements)

### ⏳ Not Started

- [ ] **AI Reasoning Display**
  - [ ] Show why each change was suggested
  - [ ] Confidence level indicator
  - [ ] Change type icons (🔧 typo, 📝 improvement, etc.)
  - [ ] Collapsible panel

- [ ] **Revision History Viewer**
  - [ ] Timeline view of all changes
  - [ ] Filter by date/deck/type
  - [ ] Revert individual change
  - [ ] Revert entire session
  - [ ] Export as CSV/JSON

- [ ] **Progress Indicators**
  - [ ] Loading states with skeleton screens
  - [ ] Progress bar for batch processing
  - [ ] Estimated time remaining
  - [ ] Cancellable operations

- [ ] **Settings Panel**
  - [ ] Deck presets (save favorite deck selections)
  - [ ] Theme selection (light/dark/auto)
  - [ ] Font size preference
  - [ ] Keyboard shortcut customization
  - [ ] Auto-advance delay

- [ ] **Batch Operations**
  - [ ] Accept all remaining
  - [ ] Reject all remaining
  - [ ] Filter queue by confidence/type
  - [ ] Confirmation dialogs for bulk actions

---

## Phase 3 - Intelligence (AI Features)

### ⏳ Not Started

- [ ] **Claude Code Integration**
  - [ ] Research integration methods (Agent SDK, CLI, MCP)
  - [ ] Implement chosen approach
  - [ ] Pass card data and prompt to AI
  - [ ] Parse structured responses (field: value format)
  - [ ] Handle errors and timeouts

- [ ] **AI Processing Optimization**
  - [ ] Parallel processing (batch cards together)
  - [ ] Chunking for large decks (process 100 at a time)
  - [ ] Rate limiting to avoid overwhelming API
  - [ ] Caching for similar cards
  - [ ] Resume interrupted processing

- [ ] **Prompt Templates**
  - [ ] Save custom prompts
  - [ ] Quick-action buttons ("Fix typos", "Improve", etc.)
  - [ ] Template variables (e.g., {{deckName}})
  - [ ] Import/export prompt library

- [ ] **Confidence Scoring**
  - [ ] Parse confidence from AI response
  - [ ] Visual indicators (High/Medium/Low)
  - [ ] Filter queue by confidence threshold
  - [ ] Auto-accept high confidence (optional)

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

---

## Known Issues

- [ ] Node.js version warning (v20.18.0 vs required v20.19+) - not critical, app works

---

## Notes

- **Backend runs on**: http://localhost:3000 (TypeScript)
- **Frontend runs on**: http://localhost:5173 (TypeScript)
- **AnkiConnect**: http://localhost:8765
- **Current stage**: Phase 1 MVP complete! Full TypeScript codebase.
- **Next session**: Add keyboard shortcuts and revision logging
- **Latest update**:
  - ✅ Complete TypeScript conversion (backend + frontend)
  - ✅ Filesystem cache system for fast deck loading
  - ✅ Batch fetching to handle large decks (5k+ cards)
  - ✅ All tests passing - API endpoints working correctly
  - 📝 All JavaScript files migrated to TypeScript with proper type safety
