# AnkiDeku - Specification Document

**AI-Powered Anki Deck Revision Interface**

Version 1.0 | Last Updated: 2025-11-22

---

## Vision

Create a delightful, efficient interface for mass-revising Anki decks with AI assistance. Designed for large collections (5k+ cards), the tool processes entire decks at once, presenting only cards with AI-suggested changes for review. Perfect for systematic improvements like typo fixes, consistency updates, or content enhancements across thousands of cards.

## Core Principles

1. **Speed First** - Keyboard-driven workflow for rapid review
2. **Clarity** - Instantly understand what changed and why
3. **Safety** - Easy to undo, nothing happens without confirmation
4. **Context** - Always show enough information to make informed decisions
5. **Flow** - Minimize cognitive load and context switching

---

## User Stories

### Mass Revision Flow
> "As a deck maintainer with 5k+ cards, I want to run AI revisions on entire decks at once, then review only the cards with suggestions, so I can efficiently improve my collection without manually selecting thousands of cards."

### Batch Processing
> "As a language learner, I want to ask 'fix all typos in my JLPT N3 deck' and have the AI process all 5,000 cards automatically, showing me only the cards that need fixing."

### Efficient Review
> "As a user, I want to quickly review AI suggestions with keyboard shortcuts so I can process hundreds of cards in one session without fatigue."

### Audit Trail
> "As a cautious user, I want to see a history of all changes made so that I can understand what was modified and revert if needed."

### Future: Contextual Learning
> "As an advanced user, I want to upload reference materials (textbooks, lessons) to provide context for AI suggestions or generate new cards from source material."

---

## Feature Specification

### 1. Deck Selection & Batch Processing

**Purpose**: Select decks and trigger mass AI processing

**Workflow**:
1. **Select Deck(s)**: Choose one or more decks from dropdown
   - Show card count per deck (e.g., "JLPT N3 Vocabulary (5,247 cards)")
   - Multi-select with checkboxes
   - Save deck presets for quick access

2. **Enter Prompt**: Free-form text box for AI instructions
   - Examples: "Fix spelling errors", "Add example sentences"
   - Prompt history dropdown for reuse

3. **Generate Suggestions**: Click button to start processing
   - AI processes ALL cards in selected deck(s)
   - Progress indicator (e.g., "Processing: 1,234 / 5,247 cards")
   - Runs in background, UI remains responsive

4. **Review Queue Populates**: Only cards WITH suggestions appear in queue
   - If AI finds 237 typos out of 5,247 cards, queue shows only those 237
   - No need to browse through unchanged cards

**Queue Features**:
- Show progress (e.g., "Reviewed: 15 / 237 suggestions")
- Sidebar showing next 5 cards with suggestions
- Persist queue state (survive browser refresh/interruptions)
- "Skip" button to defer difficult decisions
- "Flag" to mark cards needing manual attention later

---

### 2. Comparison View (Main Interface)

**Layout**: Three-column design

```
┌─────────────────────────────────────────────────────────┐
│  [Queue] │    Original Card    │   AI Suggestion      │
│  Sidebar │                     │                      │
│          │   ┌──────────────┐  │  ┌──────────────┐   │
│  Card 1  │   │  Field: Kana │  │  │  Field: Kana │   │
│  Card 2  │   │  こんいちは   │  │  │  こんにちは  │   │
│▶ Card 3  │   │              │  │  │              │   │
│  Card 4  │   │  Note:       │  │  │  Note:       │   │
│  Card 5  │   │  Common      │  │  │  Common      │   │
│          │   │  greeting    │  │  │  greeting    │   │
│          │   │              │  │  │  (typo fixed)│   │
│          │   └──────────────┘  │  └──────────────┘   │
│          │                     │                      │
│  Stats   │   [AI Reasoning]    │  [Action Buttons]   │
└─────────────────────────────────────────────────────────┘
```

**Visual Design**:
- **Diff Highlighting**: Inline character-level diffs
  - Deletions: Red background with strikethrough
  - Additions: Green background
  - Unchanged: Default styling
- **Field-by-field comparison**: Only show fields with changes
- **Collapsible fields**: Hide unchanged fields by default
- **Metadata display**: Note type, deck, last modified

**Interaction**:
- Click field to edit directly (manual modification mode)
- Hover over diff to see character codes (for subtle differences)
- Toggle between inline diff and side-by-side views

---

### 3. AI Reasoning Panel

**Purpose**: Explain why each change is suggested

**Content**:
- **Change summary**: "Fixed typo in Kana field"
- **Confidence level**: High / Medium / Low (visual indicator)
- **Reasoning**: Brief explanation (1-2 sentences)
- **Alternative suggestions**: "Other options considered: ..."
- **Source**: Which rule/model suggested this

**UX**:
- Collapsible panel (default: expanded)
- Icons for change type:
  - 🔧 Typo correction
  - 📝 Content improvement
  - ⚠️ Potential error
  - ✨ Enhancement

---

### 4. Action System

**Quick Actions** (Keyboard shortcuts in parentheses):

1. **Accept** (`Enter` or `A`)
   - Apply suggestion immediately
   - Move to next card
   - Show subtle success notification

2. **Reject** (`Backspace` or `R`)
   - Discard suggestion
   - Keep original
   - Move to next card

3. **Modify** (`E` for Edit)
   - Enter edit mode
   - Allow manual changes
   - Preview changes before applying
   - Shortcuts: `Ctrl+Enter` to save, `Esc` to cancel

4. **Skip** (`S`)
   - Defer decision
   - Move to end of queue
   - Return to later

5. **Flag** (`F`)
   - Mark for manual review
   - Add to "Review Later" list
   - Optionally add note

**Batch Actions**:
- "Accept all remaining" - apply all suggestions in queue
- "Reject all remaining" - discard all suggestions in queue
- Filter by confidence level (if AI provides scores)
- Confirm before bulk operations (safety check)

---

### 5. Edit Mode

**Triggered by**: `E` key or clicking a field

**Features**:
- Inline editing with real-time preview
- Preserve formatting (HTML if present)
- Character counter for fields with limits
- Syntax highlighting for special fields (code, formulas)
- Auto-save drafts (localStorage)

**Smart Suggestions**:
- Field templates/snippets
- Validation (e.g., valid URLs, dates)

**UX Polish**:
- Smooth transition into edit mode
- Clear visual indicator (border highlight)
- Save button vs. keyboard shortcut
- Warning on unsaved changes

---

### 6. Revision History

**Purpose**: Complete audit trail of all changes

**View Options**:
1. **Timeline View** - Chronological list of revisions
2. **Card View** - All changes for a specific card
3. **Session View** - Changes from current session

**Information Displayed**:
- Timestamp
- Card identifier (first field preview)
- Change type (accepted AI / manual edit / rejected)
- Before/after diff
- Reasoning (if AI-suggested)

**Actions**:
- Revert individual change
- Revert entire session
- Export as JSON/CSV
- Search/filter history

**Storage**:
- Save to `revisions/` directory
- One JSON file per session
- Indexed for quick lookup

---

### 7. AI Suggestion Generation (Mass Processing)

**Approach**: Batch process entire decks with single prompt

**Workflow**:
1. User selects deck(s) - e.g., "JLPT N3 Vocabulary" (5,247 cards)
2. Enters prompt - e.g., "Fix spelling and grammar errors"
3. Clicks "Generate Suggestions"
4. AI processes ALL cards in background
5. Queue populates with ONLY cards that have suggestions

**Processing Details**:
- **Async Processing**: Non-blocking, UI remains responsive
- **Progress Updates**: Real-time counter (e.g., "Processing: 1,234 / 5,247")
- **Selective Output**: AI only returns cards with actual changes
  - If only 237 cards have errors, queue shows only those 237
  - User doesn't see the 5,010 cards with no suggestions
- **Cancellable**: Stop button to abort long-running operations

**Common Prompt Examples**:
- "Fix spelling and grammar errors"
- "Add example sentences where missing"
- "Improve explanations for clarity"
- "Check if translations are accurate"
- "Standardize formatting across all fields"
- "Add reading hints (furigana) where helpful"

**AI Integration**:
- **Claude Code Integration**: Leverage existing subscription via programmatic invocation
- **Fallback**: Local LLM or manual API configuration
- **Connection check**: Alert if AI service unavailable
- **Rate Limiting**: Throttle requests to avoid overwhelming API

**Response Format**:
- Structured JSON: `{ noteId, changes: { fieldName: newValue }, reasoning }`
- Only cards with changes returned
- Reasoning included for review transparency

---

### 8. Settings & Configuration

**Deck Selection**:
- Multi-select decks
- Save deck presets ("Daily Review", "JLPT N3", etc.)
- Include/exclude subdecks

**AI Settings**:
- Integration method (Claude Code / Local LLM / Manual API)
- Custom system prompts
- Response format preferences

**UI Preferences**:
- Theme (light/dark/auto)
- Font size
- Keyboard shortcut customization
- Auto-advance delay (0-3 seconds)
- Sound effects (on/off)

**Data Management**:
- Export/import settings
- Clear revision history
- Git push revisions (backup to repo)

**Connection Monitoring**:
- **AnkiConnect Health Check**: Periodic ping to verify connectivity
- **Alert on Disconnect**: Clear warning if AnkiConnect unavailable
- **Retry Mechanism**: Auto-reconnect when service becomes available
- **Concurrent Edit Detection**: Warn if note modified externally
  - Show last modified timestamp
  - Confirmation dialog: "Force update" or "Cancel"
  - Display external changes in diff view

---

### 9. Keyboard Shortcuts

**Global**:
- `?` - Show keyboard shortcuts help
- `/` - Focus search
- `Ctrl+Z` - Undo last action
- `Esc` - Close dialogs/cancel

**Navigation**:
- `J` or `↓` - Next card
- `K` or `↑` - Previous card
- `G G` - Go to first card
- `Shift+G` - Go to last card
- `1-9` - Jump to queue position

**Actions**:
- `Enter` or `A` - Accept
- `Backspace` or `R` - Reject
- `E` - Edit mode
- `S` - Skip
- `F` - Flag for review

**View**:
- `D` - Toggle diff style (inline/side-by-side)
- `H` - Toggle history panel
- `I` - Toggle AI reasoning
- `Tab` - Cycle through fields

**Batch**:
- `Shift+A` - Accept all remaining (with confirmation)
- `Shift+R` - Reject all remaining (with confirmation)

---

### 10. Visual Design System

**Color Palette**:
- Primary: Blue (#3B82F6) - Actions, links
- Success: Green (#10B981) - Accepted changes, additions
- Warning: Amber (#F59E0B) - Flagged items, low confidence
- Danger: Red (#EF4444) - Rejected changes, deletions
- Neutral: Gray (#6B7280) - UI chrome, disabled states

**Typography**:
- Headers: Inter or SF Pro (system font)
- Body: System font stack for performance
- Monospace: JetBrains Mono (for diffs, code)
- Japanese: Noto Sans JP fallback

**Spacing**:
- Use 8px grid system
- Generous whitespace for readability
- Card spacing: 16px minimum

**Animations**:
- Smooth transitions (200-300ms ease-out)
- Micro-interactions on hover
- Success/error toasts (slide in from top-right)
- Loading states (skeleton screens)

---

## Technical Architecture

### Frontend
- **Framework**: React
- **State Management**: Zustand (lightweight)
- **Styling**: Tailwind CSS
- **Routing**: React Router
- **Diff Library**: react-diff-viewer-continued or diff-match-patch

### Backend/API Layer
- **Runtime**: Node.js (Express)
- **API Client**: Axios for AnkiConnect
- **File System**: fs module for revision logs
- **Data Format**: JSON for all storage
- **Version Control**: Git (revisions pushed to repo for backup)

### Data Flow

```
1. User selects deck
   ↓
2. Fetch notes via AnkiConnect (findNotes + notesInfo)
   ↓
3. Generate AI suggestions (async)
   ↓
4. Display in queue with diff view
   ↓
5. User reviews (accept/reject/edit)
   ↓
6. Save to revision log (JSON file)
   ↓
7. Apply changes via AnkiConnect (updateNoteFields)
   ↓
8. Update UI state (next card)
```

### File Structure

```
ankideku/
├── src/
│   ├── components/
│   │   ├── Queue.jsx
│   │   ├── ComparisonView.jsx
│   │   ├── DiffViewer.jsx
│   │   ├── ActionButtons.jsx
│   │   ├── AIReasoning.jsx
│   │   └── EditMode.jsx
│   ├── services/
│   │   ├── ankiConnect.js
│   │   ├── aiSuggestions.js
│   │   └── revisionLog.js
│   ├── store/
│   │   └── revisionStore.js
│   └── utils/
│       ├── diff.js
│       └── keyboard.js
├── revisions/
│   └── session-2025-11-22-143022.json
├── config/
│   └── settings.json
├── public/
└── README.md
```

---

## Success Metrics

**Efficiency**:
- Average time per card review: < 5 seconds
- Keyboard usage rate: > 80% of actions

**Quality**:
- User satisfaction with AI suggestions: > 70% acceptance rate
- False positive rate: < 10%

**Safety**:
- Zero data loss incidents
- Revert success rate: 100%

---

## Project Decisions

**Offline Mode**: Not supported in initial version. Alert displayed if AnkiConnect unavailable.

**Concurrent Edits**: Detection implemented with confirmation dialog allowing force update or cancel.

**Version Control**: Git integration - revisions pushed to repository for backup and history.

**Language**: English-only UI (no internationalization).

**License**: MIT License - Personal use, open source.

---

## AI Integration Strategy

### Claude Code Integration

**Challenge**: Leverage Claude Code subscription instead of separate API costs.

**Potential Approaches**:

1. **Claude Agent SDK** (Recommended)
   - Use the Claude Agent SDK if accessible from Node.js
   - Spawn agent processes programmatically
   - Pass card data and user prompt as context
   - Parse agent output for suggestions

2. **Model Context Protocol (MCP)**
   - If Claude Code exposes MCP server
   - Connect via MCP client in Node.js
   - Send card revision requests

3. **CLI Invocation**
   - Spawn Claude Code CLI process from Node.js
   - Pass prompt via stdin or temp file
   - Capture stdout for responses
   - Handle session management

4. **Fallback Options**
   - Local LLM (Ollama, LM Studio)
   - Manual API configuration (for those with API keys)

**Implementation Note**: Start with simplest viable approach. The free-form text box UX remains the same regardless of underlying AI integration method.

### Prompt Interface (Simplified for MVP)

Instead of predefined suggestion types with confidence scoring, use a straightforward approach:

1. User selects card(s)
2. Types natural language request in text box
3. Clicks "Generate Suggestion"
4. AI processes request with card context
5. Response displayed in comparison view

**Benefits**:
- Simpler to implement
- More flexible for user
- No need for complex categorization
- Easy to iterate based on usage patterns

**Future Enhancement**: Add quick-action buttons ("Fix typos", "Improve", etc.) that populate the text box with preset prompts.

---

## Future Feature: Document Context

**Purpose**: Upload reference materials to enhance AI suggestions or generate new cards

**Use Cases**:
- Upload textbook chapter → "Create flashcards from this content"
- Upload vocabulary list → "Generate example sentences for each word"
- Upload lesson notes → "Verify my card translations against this source"
- Upload grammar guide → "Add grammatical context to relevant cards"

**Implementation Notes**:
- Document upload interface (PDF, TXT, DOCX)
- Store in context for AI prompts
- Reference documents in prompt: "Using [Genki I Ch.3], improve these cards"
- Lower priority than core revision workflow

---

## Timeline Estimate

**Phase 1 - MVP** (Core functionality)
- Deck selection interface (no card picking)
- Free-form prompt input
- Batch AI processing (all cards in deck)
- Queue system (only cards with suggestions)
- Comparison view with diffs
- Accept/reject/skip actions
- AnkiConnect integration
- Local revision logging

**Phase 2 - Polish** (UX improvements)
- Edit mode for manual adjustments
- Keyboard shortcuts for rapid review
- AI reasoning display
- Revision history viewer
- Progress indicators for batch processing
- Settings panel

**Phase 3 - Intelligence** (AI features)
- Claude Code integration
- Prompt history/templates
- Processing optimization (parallel/chunked)
- Confidence scoring (if applicable)

**Phase 4 - Advanced** (Future features)
- Document upload for context
- Card generation from documents
- Batch accept/reject by criteria
- Performance optimization for 5k+ cards
- Git auto-commit revisions

---

## Notes

This specification prioritizes **mass revision efficiency** for large decks (5k+ cards). The core workflow is:

1. **Select deck** → Enter prompt → Generate suggestions (processes ALL cards)
2. **Review queue** → Only cards WITH suggestions appear (no browsing unchanged cards)
3. **Quick decisions** → Accept/reject/edit with keyboard shortcuts
4. **Apply changes** → Updates pushed to Anki via AnkiConnect

**Key Design Philosophy**: User shouldn't manually select individual cards from 20k collection. Tool handles batch processing; user handles decision-making on suggestions.

Focus on getting the deck selection → batch processing → review queue flow working smoothly before adding advanced features. The comparison view and keyboard-driven review are critical for speed.
