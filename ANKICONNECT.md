# AnkiConnect API Documentation

**Official Repository**: https://git.sr.ht/~foosoft/anki-connect
**Version**: 6
**Default Port**: 8765
**Protocol**: HTTP POST with JSON

---

## Overview

AnkiConnect is an Anki addon that exposes a RESTful API for interacting with Anki programmatically. All requests are POST requests to `http://localhost:8765` with JSON payloads.

## Request Format

All requests follow this structure:

```json
{
  "action": "actionName",
  "version": 6,
  "params": {
    // Action-specific parameters
  }
}
```

## Response Format

All responses follow this structure:

```json
{
  "result": <data>,
  "error": null | "error message"
}
```

If `error` is not null, the request failed. Otherwise, `result` contains the response data.

---

## Common API Actions

### Connection & Health

#### `version`

Get the AnkiConnect version.

**Request:**
```json
{
  "action": "version",
  "version": 6
}
```

**Response:**
```json
{
  "result": 6,
  "error": null
}
```

---

### Deck Operations

#### `deckNames`

Get all deck names.

**Request:**
```json
{
  "action": "deckNames",
  "version": 6
}
```

**Response:**
```json
{
  "result": ["Default", "Japanese::JLPT N3", "Japanese::Kanji"],
  "error": null
}
```

#### `deckNamesAndIds`

Get all deck names with their IDs.

**Request:**
```json
{
  "action": "deckNamesAndIds",
  "version": 6
}
```

**Response:**
```json
{
  "result": {
    "Default": 1,
    "Japanese::JLPT N3": 1234567890,
    "Japanese::Kanji": 9876543210
  },
  "error": null
}
```

---

### Note Operations

#### `findNotes`

Find note IDs matching a search query.

**Request:**
```json
{
  "action": "findNotes",
  "version": 6,
  "params": {
    "query": "deck:\"Japanese::JLPT N3\""
  }
}
```

**Response:**
```json
{
  "result": [1483959289817, 1483959291695, 1483959293875],
  "error": null
}
```

**Common Query Syntax:**
- `deck:"DeckName"` - All cards in deck and sub-decks
- `deck:"DeckName" -deck:"DeckName::*"` - Cards only in parent deck, not sub-decks
- `tag:mytag` - Cards with specific tag
- `is:due` - Cards due for review
- `added:1` - Cards added in last 1 day

**Full Query Documentation**: https://docs.ankiweb.net/searching.html

#### `notesInfo`

Get detailed information about notes.

**Request (by note IDs):**
```json
{
  "action": "notesInfo",
  "version": 6,
  "params": {
    "notes": [1502298033753, 1502298033754]
  }
}
```

**Request (by query):**
```json
{
  "action": "notesInfo",
  "version": 6,
  "params": {
    "query": "deck:current"
  }
}
```

**Response:**
```json
{
  "result": [
    {
      "noteId": 1502298033753,
      "modelName": "Basic",
      "tags": ["vocab", "jlpt-n3"],
      "fields": {
        "Front": {
          "value": "あいさつ",
          "order": 0
        },
        "Back": {
          "value": "greeting",
          "order": 1
        }
      },
      "mod": 1629123456,
      "cards": [1502298033755, 1502298033756]
    }
  ],
  "error": null
}
```

**Response Fields:**
- `noteId`: Unique note identifier
- `modelName`: Note type (e.g., "Basic", "Cloze", "Custom Type")
- `tags`: Array of tag strings
- `fields`: Object mapping field names to values and order
- `mod`: Modification timestamp (Unix time)
- `cards`: Array of card IDs associated with this note

**Important Note:** The basic `notesInfo` response does NOT include deck names. Use `cardsInfo` to get deck information.

#### `updateNoteFields`

Update the fields of a specific note.

**Request:**
```json
{
  "action": "updateNoteFields",
  "version": 6,
  "params": {
    "note": {
      "id": 1502298033753,
      "fields": {
        "Front": "こんにちは",
        "Back": "hello"
      }
    }
  }
}
```

**Response:**
```json
{
  "result": null,
  "error": null
}
```

**Notes:**
- Only fields specified in `fields` will be updated
- Other fields remain unchanged
- Field names must match exactly (case-sensitive)

#### `addNote`

Add a new note to Anki.

**Request:**
```json
{
  "action": "addNote",
  "version": 6,
  "params": {
    "note": {
      "deckName": "Japanese::JLPT N3",
      "modelName": "Basic",
      "fields": {
        "Front": "新しい単語",
        "Back": "new word"
      },
      "tags": ["vocab"],
      "options": {
        "allowDuplicate": false
      }
    }
  }
}
```

**Response:**
```json
{
  "result": 1502298033757,
  "error": null
}
```

Returns the new note ID on success.

---

### Card Operations

#### `cardsInfo`

Get detailed information about cards, **including deck names**.

**Request:**
```json
{
  "action": "cardsInfo",
  "version": 6,
  "params": {
    "cards": [1498938915662, 1502098034048]
  }
}
```

**Response:**
```json
{
  "result": [
    {
      "cardId": 1498938915662,
      "deckName": "Japanese::JLPT N3",
      "modelName": "Basic",
      "fieldOrder": 0,
      "fields": {
        "Front": {"value": "こんにちは", "order": 0},
        "Back": {"value": "hello", "order": 1}
      },
      "css": "...",
      "note": 1502298033753,
      "interval": 16,
      "due": 1629234567,
      "reps": 5,
      "lapses": 0,
      "left": 0,
      "mod": 1629123456
    }
  ],
  "error": null
}
```

**Key Fields:**
- `cardId`: Unique card identifier
- `deckName`: **The deck this card belongs to** (THIS IS WHAT WE NEED!)
- `note`: The note ID this card belongs to
- `interval`: Days until next review
- `due`: Unix timestamp when card is due
- `reps`: Number of times reviewed
- `lapses`: Number of times failed

**Use Case:** Since `notesInfo` doesn't include deck names, use `cardsInfo` with the card IDs from a note to determine which deck(s) the note belongs to.

#### `findCards`

Find card IDs matching a search query.

**Request:**
```json
{
  "action": "findCards",
  "version": 6,
  "params": {
    "query": "deck:\"Japanese::JLPT N3\" is:due"
  }
}
```

**Response:**
```json
{
  "result": [1498938915662, 1502098034048],
  "error": null
}
```

---

### Batch Operations

#### `multi`

Execute multiple actions in a single request.

**Request:**
```json
{
  "action": "multi",
  "version": 6,
  "params": {
    "actions": [
      {
        "action": "updateNoteFields",
        "params": {
          "note": {
            "id": 1502298033753,
            "fields": {"Front": "Updated"}
          }
        }
      },
      {
        "action": "updateNoteFields",
        "params": {
          "note": {
            "id": 1502298033754,
            "fields": {"Front": "Also Updated"}
          }
        }
      }
    ]
  }
}
```

**Response:**
```json
{
  "result": [null, null],
  "error": null
}
```

Returns an array of results, one for each action.

---

## Deck Hierarchy in Anki

Anki uses `::` to denote sub-decks:

```
Japanese                    (parent deck)
  └── Japanese::JLPT N3     (sub-deck)
      └── Japanese::JLPT N3::Kanji  (sub-sub-deck)
  └── Japanese::Vocabulary  (sub-deck)
```

**Important Behavior:**
- Query `deck:"Japanese"` returns cards from ALL sub-decks
- Query `deck:"Japanese::JLPT N3"` returns cards from "Japanese::JLPT N3" and "Japanese::JLPT N3::Kanji"
- Each note/card belongs to exactly ONE deck (the most specific one)

**To get only parent deck (no sub-decks):**
```
deck:"Japanese" -deck:"Japanese::*"
```

---

## Error Handling

### Common Errors

**AnkiConnect not running:**
```json
{
  "error": "ECONNREFUSED",
  "message": "Connection refused at localhost:8765"
}
```
**Solution:** Start Anki with AnkiConnect addon installed.

**Invalid action:**
```json
{
  "result": null,
  "error": "unsupported action"
}
```

**Invalid parameters:**
```json
{
  "result": null,
  "error": "missing required param: notes"
}
```

**Note not found:**
```json
{
  "result": null,
  "error": "cannot update note: note not found"
}
```

---

## Performance Considerations

### Batching

- **notesInfo**: Can handle 50-100 note IDs per request efficiently
- **cardsInfo**: Can handle 100+ card IDs per request
- **updateNoteFields**: For multiple updates, use `multi` action

### Large Decks (5000+ cards)

For large decks, batch requests to avoid timeouts:

```javascript
// Good: Batch processing
const BATCH_SIZE = 50;
for (let i = 0; i < noteIds.length; i += BATCH_SIZE) {
  const batch = noteIds.slice(i, i + BATCH_SIZE);
  const notes = await notesInfo(batch);
  // Process batch
}

// Bad: Single large request
const allNotes = await notesInfo(allNoteIds); // May timeout with 5000+ IDs
```

### Timeouts

- Default timeout: Usually 30 seconds
- For large requests, increase timeout in your HTTP client
- Add delays between batches (50-100ms) to avoid overwhelming Anki

---

## Working with Sub-Decks

### Example: Cache by Actual Deck

When fetching a parent deck, determine each note's actual sub-deck:

```javascript
// 1. Get all notes from parent deck
const noteIds = await findNotes('deck:"Japanese"');
const notes = await notesInfo(noteIds);

// 2. Get card info to determine actual deck
const allCardIds = notes.flatMap(note => note.cards);
const cardsInfo = await cardsInfo(allCardIds);

// 3. Map card IDs to deck names
const cardDeckMap = new Map();
for (const card of cardsInfo) {
  cardDeckMap.set(card.cardId, card.deckName);
}

// 4. Assign deck name to each note
for (const note of notes) {
  note.deckName = cardDeckMap.get(note.cards[0]);
}

// 5. Group notes by actual deck
const notesByDeck = new Map();
for (const note of notes) {
  const deck = note.deckName;
  if (!notesByDeck.has(deck)) {
    notesByDeck.set(deck, []);
  }
  notesByDeck.get(deck).push(note);
}

// Now you can cache each sub-deck separately!
```

---

## Configuration

### CORS Settings

AnkiConnect allows CORS by default. To restrict origins, edit the addon config:

1. Anki → Tools → Add-ons
2. Select AnkiConnect → Config
3. Set `webCorsOriginList` to allowed origins

### Binding Address

Default: `127.0.0.1:8765` (localhost only)

To allow remote access, set `ANKICONNECT_BIND_ADDRESS` environment variable.

**Security Warning:** Only bind to `0.0.0.0` on trusted networks!

---

## Useful Resources

- **Official Repository**: https://git.sr.ht/~foosoft/anki-connect
- **Anki Search Syntax**: https://docs.ankiweb.net/searching.html
- **AnkiConnect Actions List**: See repository README for full list of 100+ actions

---

## Quick Reference

| Action | Purpose | Key Parameters |
|--------|---------|----------------|
| `version` | Check connection | None |
| `deckNames` | List all decks | None |
| `deckNamesAndIds` | Decks with IDs | None |
| `findNotes` | Search for notes | `query` |
| `notesInfo` | Get note details | `notes` (IDs) or `query` |
| `cardsInfo` | Get card details + deck | `cards` (IDs) |
| `updateNoteFields` | Update note | `note.id`, `note.fields` |
| `addNote` | Create note | `note` object |
| `multi` | Batch operations | `actions` array |

---

## AnkiDeku-Specific Usage Patterns

### Fetching a Deck with Sub-Deck Support

```typescript
// 1. Find all note IDs in deck (includes sub-decks)
const noteIds = await findNotes(`deck:"${deckName}"`);

// 2. Fetch notes in batches
const notes = await notesInfo(noteIds);

// 3. Enrich with deck information
const cardIds = notes.flatMap(n => n.cards);
const cards = await cardsInfo(cardIds);
const cardDeckMap = new Map(cards.map(c => [c.cardId, c.deckName]));

for (const note of notes) {
  note.deckName = cardDeckMap.get(note.cards[0]);
}

// 4. Group by actual deck for caching
const byDeck = groupBy(notes, n => n.deckName);
for (const [deck, deckNotes] of byDeck) {
  cache.save(deck, deckNotes);
}
```

### Updating Notes

```typescript
// Update single note
await updateNoteFields({
  note: {
    id: noteId,
    fields: { Front: "New Value" }
  }
});

// Batch update multiple notes
await multi({
  actions: updates.map(u => ({
    action: "updateNoteFields",
    params: { note: { id: u.noteId, fields: u.fields } }
  }))
});
```

---

*Last Updated: 2025-11-22*
