# AnkiDeku

**AI-Powered Anki Deck Revision Tool**

AnkiDeku is a modern web application that uses AI (Claude Code) to intelligently suggest improvements to your Anki flashcards. Review suggestions in real-time, accept or reject changes, and apply updates directly to your Anki collection - all through an intuitive chat-based interface.

---

## ✨ Features

### 🤖 AI-Powered Card Revision
- **Natural language prompts**: Describe what you want to improve in plain English
- **Real-time processing**: See suggestions appear as Claude analyzes your cards
- **Intelligent suggestions**: AI understands context and provides meaningful improvements
- **File-based workflow**: All sessions are saved and can be reviewed later

### 🎯 Smart Review Interface
- **Side-by-side comparison**: Original vs. suggested changes with visual diffs
- **Field-level highlighting**: Character-by-character diff showing exactly what changed
- **AI reasoning**: See why each change was suggested
- **Quick actions**: Accept, reject, or skip suggestions with a click

### 📊 Session Management
- **Live updates**: Real-time WebSocket connection shows suggestions as they're generated
- **Session history**: Load and review previous AI sessions
- **Chat interface**: View the original prompt and session details in the sidebar
- **Progress tracking**: Track how many cards you've reviewed

### 💾 Persistent Data
- **Cache system**: Fast deck loading with automatic background sync
- **Session storage**: All AI sessions saved to `database/ai-sessions/`
- **Settings persistence**: Field display preferences saved per note type
- **History tracking**: Review all accepted/rejected/skipped changes

### 🎨 Modern UI
- **Dark theme**: Easy on the eyes with persistent theme preference
- **Responsive design**: Clean, modern interface built with Tailwind CSS
- **Search & filter**: Find cards quickly in the queue and history
- **Queue management**: Click to jump to any card, see progress at a glance

---

## 🚀 Quick Start

### Prerequisites

1. **Anki Desktop** with **AnkiConnect** addon installed
   - Get AnkiConnect: Tools → Add-ons → Get Add-ons → Enter code `2055492159`
   - Restart Anki after installation

2. **Node.js** v20+ (v20.18.0 or higher recommended)

3. **Claude Code CLI** (for AI features)
   - Install from: https://claude.com/claude-code

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd ankideku
   ```

2. **Install backend dependencies**
   ```bash
   cd backend
   npm install
   ```

3. **Install frontend dependencies**
   ```bash
   cd ../frontend
   npm install
   ```

4. **Ensure Anki is running** with AnkiConnect enabled

### Running the Application

You'll need **three terminal windows**:

**Terminal 1 - Anki (if not already running)**
```bash
# Start Anki Desktop
# AnkiConnect will run on http://localhost:8765
```

**Terminal 2 - Backend**
```bash
cd backend
npm run dev
# Runs on http://localhost:3001
```

**Terminal 3 - Frontend**
```bash
cd frontend
npm run dev
# Runs on http://localhost:5173
```

**Access the app**: Open http://localhost:5173 in your browser

---

## 📖 How to Use

### 1. **Select a Deck**
   - Open the AI Assistant sidebar (right panel)
   - Choose your deck from the dropdown
   - The app will load and cache your cards for fast access

### 2. **Create an AI Session**
   - Type your improvement request in natural language:
     - *"Fix typos and improve grammar"*
     - *"Add JLPT level tags to all cards"*
     - *"Standardize formatting across all fields"*
   - Press Enter to start processing

### 3. **Review Suggestions**
   - Suggestions appear in real-time as Claude processes your deck
   - Each card shows:
     - **Original content** (left) vs. **Suggested changes** (right)
     - **Visual diff** highlighting what changed
     - **AI reasoning** explaining why the change was suggested

### 4. **Take Action**
   - **Accept** (✓): Apply changes to Anki immediately
   - **Reject** (✗): Discard the suggestion
   - **Skip** (⏭): Move to end of queue to review later

### 5. **Track Progress**
   - **Queue tab**: See remaining suggestions
   - **History tab**: Review all actions taken
   - **Progress bar**: Track completion status

### 6. **Load Previous Sessions**
   - Click "Back to Sessions" to see all past AI sessions
   - Click any session to review its suggestions
   - View Claude output logs with the "View Output" button

---

## 🏗️ Project Structure

```
ankideku/
├── backend/                 # Express.js TypeScript backend
│   ├── src/
│   │   ├── index.ts        # Main server file with WebSocket
│   │   ├── routes/         # API routes
│   │   │   └── sessions.ts # AI session endpoints
│   │   ├── services/       # Business logic
│   │   │   ├── ankiConnect.ts      # Anki API wrapper
│   │   │   ├── cache.ts            # Deck caching
│   │   │   ├── settings.ts         # User settings
│   │   │   ├── sessionService.ts   # Session management
│   │   │   ├── fileWatcher.ts      # Watch for new suggestions
│   │   │   └── claudeSpawner.ts    # Spawn Claude CLI
│   │   ├── processors/     # AI processing
│   │   │   ├── claudeProcessor.ts  # Main processor
│   │   │   └── promptGenerator.ts  # Generate prompts
│   │   └── types/          # TypeScript types
│   └── package.json
│
├── frontend/               # React + Vite TypeScript frontend
│   ├── src/
│   │   ├── App.tsx        # Main application
│   │   ├── components/    # React components
│   │   │   ├── ComparisonView.tsx  # Side-by-side diff
│   │   │   ├── Queue.tsx           # Queue & history sidebar
│   │   │   ├── Sidebar.tsx         # AI chat interface
│   │   │   ├── Settings.tsx        # Settings modal
│   │   │   └── ui/                 # Reusable UI components
│   │   ├── hooks/         # Custom React hooks
│   │   │   ├── useCardGeneration.ts
│   │   │   ├── useCardReview.ts
│   │   │   ├── useSessionManagement.ts
│   │   │   └── useWebSocket.ts
│   │   ├── services/      # API clients
│   │   │   ├── api.ts              # Main API client
│   │   │   └── sessionApi.ts       # Session API
│   │   ├── store/         # Zustand state management
│   │   └── types/         # TypeScript types
│   └── package.json
│
├── contract/              # Shared TypeScript types
│   └── types.ts          # Type definitions for both frontend & backend
│
├── database/             # Persistent data storage
│   ├── decks/           # Cached Anki deck data (JSON)
│   ├── ai-sessions/     # AI processing sessions
│   │   └── session-{timestamp}/
│   │       ├── request.json        # Original prompt & metadata
│   │       ├── claude-task.md      # Generated task for Claude
│   │       ├── suggestions/        # Individual suggestions
│   │       └── logs/               # Claude output logs
│   └── settings.json    # User preferences
│
├── TODO.md              # Development roadmap
└── README.md           # This file
```

---

## 🛠️ Technology Stack

### Backend
- **Runtime**: Node.js + TypeScript
- **Framework**: Express.js
- **WebSocket**: Socket.IO (real-time updates)
- **File Watching**: Chokidar (detect new suggestions)
- **AI Integration**: Claude Code CLI via child process

### Frontend
- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **WebSocket Client**: Socket.IO Client
- **Diff Engine**: diff-match-patch

### Data & APIs
- **Anki Integration**: AnkiConnect REST API (port 8765)
- **Caching**: Filesystem-based JSON cache
- **Session Storage**: File-based with JSON

---

## 🔧 Development

### Available Scripts

**Backend:**
```bash
npm run dev        # Start development server with hot reload
npm run build      # Compile TypeScript to JavaScript
npm start          # Run production build
```

**Frontend:**
```bash
npm run dev        # Start Vite dev server
npm run build      # Build for production
npm run preview    # Preview production build
```

### API Endpoints

**AnkiConnect Integration:**
- `GET /api/health` - Server health check
- `GET /api/anki/ping` - Check AnkiConnect connection
- `GET /api/decks` - List all Anki decks
- `GET /api/decks/:deckName/notes` - Get deck cards (with cache)
- `POST /api/decks/:deckName/sync` - Force cache refresh
- `PUT /api/notes/:noteId` - Update single card
- `POST /api/notes/batch-update` - Batch update cards

**AI Session Management:**
- `POST /api/sessions/new` - Create new AI session
- `GET /api/sessions` - List all sessions with metadata
- `GET /api/sessions/:id` - Load session with suggestions
- `GET /api/sessions/:id/status` - Get session status
- `GET /api/sessions/:id/output` - Get Claude output logs
- `POST /api/sessions/:id/cancel` - Cancel running session
- `DELETE /api/sessions/:id` - Delete session

**Settings:**
- `GET /api/settings` - Get user settings
- `PUT /api/settings/field-display` - Update field display config

### WebSocket Events

**Client → Server:**
- `subscribe:session` - Subscribe to session updates
- `unsubscribe:session` - Unsubscribe from session

**Server → Client:**
- `suggestion:new` - New card suggestion available
- `session:complete` - Session processing finished
- `session:error` - Error during processing

---

## 🐛 Troubleshooting

### AnkiConnect Not Responding
- Ensure Anki Desktop is running
- Verify AnkiConnect addon is installed (code: `2055492159`)
- Check http://localhost:8765 is accessible
- Restart Anki if needed

### Cards Not Updating
- Don't have the card open in Anki's browser while updating
- Check the notification messages for errors
- Verify Anki is not suspended/paused

### Session Not Creating Suggestions
- Check Claude Code CLI is installed and accessible
- View output logs using the "View Output" button
- Check `database/ai-sessions/{session-id}/logs/` for errors

### Performance Issues with Large Decks
- The app caches decks to disk for fast loading
- First load may be slow for 10k+ card decks
- Subsequent loads use the cache and are instant

---

## ⚠️ Disclaimer

**Personal Project**: AnkiDeku was built primarily for personal use and developed with AI assistance (Claude Code) in approximately one day. While functional and tested with my own decks, it may have rough edges or edge cases not yet discovered.

**Use at your own risk**: Always backup your Anki collection before using this tool. Test on a small subset of cards first.

---

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **AnkiConnect** by FooSoft - Essential API for Anki integration
- **Claude Code** by Anthropic - AI-powered card analysis
- **Anki** - The amazing spaced repetition software
