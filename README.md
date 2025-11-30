# AnkiDeku

**AI-Powered Anki Deck Revision Tool**

AnkiDeku is a native desktop application that uses AI to intelligently suggest improvements to your Anki flashcards. Review suggestions in real-time, accept or reject changes, and apply updates directly to your Anki collection.

---

## Quick Start

### Prerequisites

1. **Anki Desktop** with **AnkiConnect** addon installed
   - Get AnkiConnect: Tools > Add-ons > Get Add-ons > Enter code `2055492159`
   - Restart Anki after installation

2. **Java 17+** (JDK required for development, JRE for running)

3. **Claude Code CLI** installed and authenticated
   - Install from: https://claude.ai/download
   - Run `claude` once to authenticate

### Running from Source

```bash
# Clone the repository
git clone git@github.com:lheydel/ankideku.git
cd ankideku

# Run the application
./gradlew run
```

### Installing from Release

Download the installer for your platform:
- **Windows**: `AnkiDeku-1.0.0.msi`
- **macOS**: `AnkiDeku-1.0.0.dmg`
- **Linux**: `AnkiDeku-1.0.0.deb`

---

## Usage Guide

### Step 1: Select a Deck

1. Open the app - you'll see the **AI Assistant** panel on the right
2. Click the **deck dropdown** and select your target deck
3. Click the **sync button** (circular arrow) to load your cards
   - First sync may take a moment for large decks
   - Progress bar shows sync status for each sub-deck
4. After sync, you'll see card count and token estimate below the dropdown

### Step 2: Create an AI Session

1. Type your improvement request in the text area. Examples:
   - *"Fix typos and improve grammar in the English translations"*
   - *"Standardize the format: put the reading in parentheses after each word"*
   - *"Remove duplicate information between Front and Back fields"*

2. **Optional**: Check "Sync deck before processing" to ensure you have the latest cards

3. Press **Enter** or click Send

4. The AI will process your cards in batches - watch the progress bar

### Step 3: Review Suggestions

As suggestions arrive, you'll see:

- **Queue tab** (left panel): List of all pending suggestions
- **Comparison view** (center): Side-by-side diff of original vs. suggested changes
- **AI reasoning**: Explanation of why each change was suggested

The diff view highlights:
- **Red**: Removed text
- **Green**: Added text
- Field-by-field comparison

### Step 4: Take Action

For each suggestion, you can:

| Action | Button | Effect |
|--------|--------|--------|
| **Accept** | Green checkmark | Apply changes to Anki immediately |
| **Reject** | Red X | Discard the suggestion |
| **Skip** | Arrow | Move to end of queue for later |

### Step 5: Track Progress

- **Queue tab**: See remaining suggestions with search/filter
- **History tab**: Review all accepted/rejected changes
- **Session history**: Click "Back to Sessions" to see all past sessions

### Tips

- **Start small**: Test on a small deck first to see how the AI interprets your prompts
- **Be specific**: The more specific your prompt, the better the results
- **Review carefully**: Always verify suggestions before accepting
- **Sub-decks**: Selecting a parent deck includes all sub-decks automatically

---

## Features

### AI-Powered Card Revision
- Natural language prompts
- Real-time streaming of suggestions via WebSocket
- Batch processing for large decks
- Token usage tracking

### Smart Review Interface
- Side-by-side comparison with character-level diffs
- AI reasoning for each suggestion
- Search and filter in queue/history

### Session Management
- All sessions saved to disk
- Resume or review past sessions
- Cancel running sessions
- View AI output logs

### Deck Sync
- Fast caching with background sync
- Sub-deck support
- Progress tracking per deck
- Token estimation for cost awareness

---

## Development

### Project Structure

```
ankideku/
├── src/main/kotlin/com/ankideku/
│   ├── Main.kt                    # Application entry point
│   ├── di/                        # Koin dependency injection
│   ├── data/
│   │   ├── local/                 # SQLite database (SQLDelight)
│   │   ├── remote/                # AnkiConnect client
│   │   └── repository/            # Data repositories
│   ├── domain/
│   │   ├── model/                 # Domain models
│   │   ├── repository/            # Repository interfaces
│   │   └── usecase/               # Business logic
│   └── ui/
│       ├── components/            # Reusable UI components
│       ├── screens/               # Screen composables
│       └── theme/                 # Material 3 theming
├── src/main/sqldelight/           # Database schema
└── build.gradle.kts               # Build configuration
```

### Build Commands

```bash
# Run in development
./gradlew run

# Build (compile + test)
./gradlew build

# Package for distribution
./gradlew packageMsi    # Windows
./gradlew packageDmg    # macOS
./gradlew packageDeb    # Linux

# Output location
# build/compose/binaries/main/{msi,dmg,deb}/
```

### Tech Stack

- **Kotlin** - Language
- **Compose Desktop** - UI framework (Material 3)
- **SQLDelight** - Type-safe SQLite
- **Koin** - Dependency injection
- **Ktor Client** - HTTP client for AnkiConnect
- **Kotlinx Coroutines** - Async programming
- **Kotlinx Serialization** - JSON parsing

---

## Troubleshooting

### AnkiConnect Not Responding
- Ensure Anki Desktop is running
- Verify AnkiConnect addon is installed (code: `2055492159`)
- Check http://localhost:8765 is accessible
- Restart Anki

### No Suggestions Generated
- Check Claude Code CLI is installed: `claude --version`
- Ensure you're authenticated: run `claude` in terminal
- Check the session log in the sidebar for errors

### Cards Not Updating in Anki
- Don't have the card open in Anki's browser while updating
- Check notification messages for errors
- Sync Anki after accepting changes (Ctrl+Y or Tools > Sync)

### Large Deck Performance
- First sync caches to disk - subsequent syncs are fast
- Token estimation helps predict processing cost
- Consider syncing sub-decks individually for very large collections

---

## Disclaimer

**Personal Project**: AnkiDeku was built for personal use. While functional, it may have edge cases not yet discovered.

**Backup your collection**: Always backup your Anki data before using this tool. Test on a small deck first.

---

## License

MIT License
