# Anki Deck Revision Project

This project aims to create a UI for revising Anki decks with AI assistance, allowing validation, rejection, or modification of suggested changes.

## Overview

The workflow will be:
1. Load Anki deck data in real-time via AnkiConnect
2. Display notes in a custom UI
3. Allow review and modification of content
4. Save approved changes directly back to Anki
5. Track revision history on the filesystem

## AnkiConnect Setup

### Installation

AnkiConnect is an Anki addon that provides an HTTP API for interacting with Anki programmatically.

1. Open Anki
2. Go to Tools → Add-ons → Get Add-ons
3. Enter code: `2055492159`
4. Restart Anki

The addon runs a local HTTP server on `http://localhost:8765`

### API Capabilities

AnkiConnect provides comprehensive access to Anki functionality through JSON-based HTTP requests.

#### Core Actions for Deck Revision

**Reading Deck Data:**
- `findNotes` - Search for notes using queries (e.g., "deck:MyDeck")
- `notesInfo` - Get complete note data (fields, tags, note type, card IDs)
- `deckNames` / `deckNamesAndIds` - List all available decks
- `getTags` - Retrieve all tags in the collection

**Modifying Notes:**
- `updateNoteFields` - Update note field content (key for revisions)
- `addTags` / `removeTags` - Organize notes with tags
- `addNote` / `addNotes` - Create new notes

**Card Management:**
- `findCards` - Find cards by query
- `cardsInfo` - Get card-specific data (review stats, intervals)
- `suspend` / `unsuspend` - Temporarily disable cards
- `cardsToNotes` - Convert card IDs to note IDs

**Workflow Integration:**
- `guiBrowse` - Open Anki's browser to specific cards
- `multi` - Execute multiple actions in one request

#### All Available Action Categories

1. **Deck Management** - Create, delete, configure decks
2. **Note Operations** - Create, read, update notes and fields
3. **Card Models** - Manage note types and templates
4. **Media Management** - Store and retrieve media files
5. **Statistics** - Access review history and performance data
6. **User Interface** - Control Anki's GUI programmatically
7. **Miscellaneous** - Version info, multi-action requests, sync

### Testing AnkiConnect

#### 1. Check Connection

```bash
curl -s http://localhost:8765 -X POST -d "{\"action\": \"version\", \"version\": 6}"
```

Response: `{"result": 6, "error": null}`

#### 2. List Available Note Types

```bash
curl -s http://localhost:8765 -X POST -d "{\"action\": \"modelNames\", \"version\": 6}"
```

#### 3. Create Test Deck

```bash
curl -s http://localhost:8765 -X POST -d "{\"action\": \"createDeck\", \"version\": 6, \"params\": {\"deck\": \"Test Deck\"}}"
```

#### 4. Create Test Note

```bash
curl -s http://localhost:8765 -X POST -d "{\"action\": \"addNote\", \"version\": 6, \"params\": {\"note\": {\"deckName\": \"Test Deck\", \"modelName\": \"JLPT-Voc\", \"fields\": {\"Niveau\": \"TEST\", \"Kana\": \"\\u3066\\u3059\\u3068\", \"Kanji\": \"\\u30c6\\u30b9\\u30c8123\", \"Mot\": \"Test Word\", \"Note\": \"Test note created via API\"}, \"tags\": [\"test\"]}}}"
```

#### 5. Retrieve Note Information

```bash
curl -s http://localhost:8765 -X POST -d "{\"action\": \"notesInfo\", \"version\": 6, \"params\": {\"notes\": [NOTE_ID]}}"
```

### Important Notes

#### Character Encoding

When sending Japanese or other non-ASCII characters via curl on Windows:
- Use Unicode escape sequences (e.g., `\u3066` for て)
- This issue only affects manual curl commands
- Application code (Node.js, Python, etc.) handles UTF-8 automatically

#### Update Restrictions

When using `updateNoteFields`:
- Do NOT view the note in Anki's browser while updating
- Close the browser or switch to a different note before updating
- Otherwise, fields will not update properly

#### Real-time Operation

- Anki must be running with AnkiConnect installed
- Changes appear immediately in Anki (no import/export needed)
- All operations are synchronous

## Workflow Design

### Proposed Revision Process

1. **Load deck**: Use `findNotes` with query to get all note IDs from target deck
2. **Fetch data**: Use `notesInfo` to retrieve all note fields and metadata
3. **Display in UI**: Show notes in custom web interface for review
4. **AI suggestions**: Generate revision suggestions with context
5. **User review**: Approve, reject, or modify suggestions
6. **Update Anki**: Use `updateNoteFields` for approved changes
7. **Track revisions**: Tag revised notes (e.g., "revised-2025-11-22")

### Data Storage

- All revision suggestions saved to filesystem as JSON
- Allows rollback and audit trail
- Enables offline review of changes before applying

## Resources

- [AnkiConnect Official Repository](https://git.sr.ht/~foosoft/anki-connect)
- [AnkiConnect API Reference](https://deepwiki.com/amikey/anki-connect/2.2-api-reference)
- [AnkiConnect Note Actions](https://hexdocs.pm/anki_connect/AnkiConnect.Actions.Note.html)
- [AnkiWeb Addon Page](https://ankiweb.net/shared/info/2055492159)

## Next Steps

1. Design UI architecture
2. Choose tech stack (Node.js/React, Python/Flask, etc.)
3. Implement deck browsing interface
4. Add AI-powered revision suggestions
5. Build approval/rejection workflow
6. Implement filesystem-based change tracking
