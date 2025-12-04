# SEL - Structured Expression Language

SEL is a JSON-based query language for filtering Notes, Suggestions, HistoryEntries, and Sessions. It compiles to SQL for efficient database queries.

## Format

Every operation is a JSON object with exactly **one key** (the operator) and its arguments as the value:

```json
{ "operator": [arg1, arg2, ...] }
// or for single argument:
{ "operator": arg }
```

## Query Structure

A complete query has a `target` entity and a `where` clause:

```json
{
  "target": "Note",
  "where": { "isEmpty": { "field": "example" } }
}
```

### Query Fields

| Field     | Required | Description                                                  |
|-----------|----------|--------------------------------------------------------------|
| `target`  | Yes      | Entity type: `Note`, `Suggestion`, `HistoryEntry`, `Session` |
| `where`   | Yes      | Filter expression                                            |
| `alias`   | No       | Name this query's scope for subquery references              |
| `result`  | No       | What subquery returns (default: `SELECT 1` for EXISTS)       |
| `orderBy` | No       | Sort order: `[{ "field": "createdAt", "desc": true }]`       |
| `limit`   | No       | Maximum rows to return                                       |

---

## Operators

### Property Access

**`prop`** - Access entity column:
```json
{ "prop": "status" }
{ "prop": "noteId" }
```

**`field`** - Access field_value (note fields):
```json
{ "field": "example" }
{ "field": ["example", "changes"] }
{ "field": ["example", ["editedChanges", "changes", "original"]] }
```

Field contexts by entity:
- **Note**: `current`
- **Suggestion**: `original`, `changes`, `editedChanges`
- **HistoryEntry**: `before`, `after`

### Comparison

```json
{ "==": [{ "prop": "status" }, "pending"] }
{ "!=": [{ "field": "kanji" }, ""] }
{ "<": [{ "prop": "createdAt" }, 1701590400000] }
{ "<=": [...] }
{ ">": [...] }
{ ">=": [...] }
```

### Logic

```json
{ "and": [condition1, condition2, ...] }
{ "or": [condition1, condition2, ...] }
{ "not": condition }
```

### String Matching

```json
{ "contains": [haystack, needle] }
{ "startsWith": [string, prefix] }
{ "endsWith": [string, suffix] }
```

SQLite's LIKE is case-insensitive for ASCII.

### Predicates

```json
{ "isEmpty": { "field": "example" } }
{ "isNotEmpty": { "field": "example" } }
{ "isNull": { "prop": "sessionId" } }
{ "isNotNull": { "prop": "sessionId" } }
```

### String Functions

```json
{ "len": { "field": "example" } }
```

### Arithmetic

```json
{ "+": [a, b] }
{ "-": [a, b] }
{ "*": [a, b] }
{ "/": [a, b] }
{ "%": [a, b] }
```

### Aggregates

```json
{ "min": [a, b, ...] }
{ "max": [a, b, ...] }
{ "count": { "query": {...} } }
```

---

## Subqueries

### EXISTS

Check if related records exist:

```json
{
  "target": "Note",
  "alias": "n",
  "where": { "exists": { "query": {
    "target": "Suggestion",
    "where": { "and": [
      { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
      { "==": [{ "prop": "status" }, "accepted"] }
    ]}
  }}}
}
```

### NOT EXISTS

```json
{
  "target": "Note",
  "alias": "n",
  "where": { "not": { "exists": { "query": {
    "target": "Suggestion",
    "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] }
  }}}}
}
```

### Scalar Subquery

Return a value from a subquery:

```json
{
  "target": "Note",
  "alias": "n",
  "where": { "==": [
    { "query": {
      "target": "Suggestion",
      "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
      "result": { "prop": "status" },
      "orderBy": [{ "field": "createdAt", "desc": true }],
      "limit": 1
    }},
    "accepted"
  ]}
}
```

### Scope References

Use `ref` to access parent query properties from within subqueries:

```json
{ "ref": ["n", "id"] }      // n.id from parent Note query
{ "ref": ["s", "noteId"] }  // s.note_id from parent Suggestion query
```

The scope name must match an `alias` from an ancestor query. Duplicate aliases are rejected.

---

## Examples

### Notes with empty "example" field

```json
{
  "target": "Note",
  "where": { "isEmpty": { "field": "example" } }
}
```

### Pending suggestions

```json
{
  "target": "Suggestion",
  "where": { "==": [{ "prop": "status" }, "pending"] }
}
```

### Notes where reading starts with kana

```json
{
  "target": "Note",
  "where": { "startsWith": [
    { "field": "reading" },
    { "field": "kana" }
  ]}
}
```

### Suggestions where changes contain original + 4 chars longer

```json
{
  "target": "Suggestion",
  "where": { "and": [
    { "contains": [
      { "field": ["example", "changes"] },
      { "field": ["kanji", "original"] }
    ]},
    { ">=": [
      { "len": { "field": ["example", "changes"] } },
      { "+": [
        { "len": { "field": ["kanji", "original"] } },
        4
      ]}
    ]}
  ]}
}
```

### Notes with suggestions from a specific session

```json
{
  "target": "Note",
  "alias": "n",
  "where": { "exists": { "query": {
    "target": "Suggestion",
    "where": { "and": [
      { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
      { "==": [{ "prop": "sessionId" }, "session-uuid-here"] }
    ]}
  }}}
}
```

### Multi-level: Notes with suggestions from sessions created today

```json
{
  "target": "Note",
  "alias": "n",
  "where": { "exists": { "query": {
    "target": "Suggestion",
    "alias": "s",
    "where": { "and": [
      { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
      { "exists": { "query": {
        "target": "Session",
        "where": { "and": [
          { "==": [{ "prop": "id" }, { "ref": ["s", "sessionId"] }] },
          { ">=": [{ "prop": "createdAt" }, 1701590400000] }
        ]}
      }}}
    ]}
  }}}
}
```

---

## Entity Properties

### Note
| SEL Key      | SQL Column     | Type   |
|--------------|----------------|--------|
| `id`         | `id`           | Long   |
| `deckId`     | `deck_id`      | Long   |
| `ankiNoteId` | `anki_note_id` | Long   |
| `noteType`   | `note_type`    | String |

### Suggestion
| SEL Key     | SQL Column   | Type      |
|-------------|--------------|-----------|
| `id`        | `id`         | Long      |
| `noteId`    | `note_id`    | Long      |
| `sessionId` | `session_id` | String    |
| `status`    | `status`     | String    |
| `createdAt` | `created_at` | Timestamp |

### Session
| SEL Key     | SQL Column   | Type      |
|-------------|--------------|-----------|
| `id`        | `id`         | String    |
| `deckId`    | `deck_id`    | Long      |
| `status`    | `status`     | String    |
| `createdAt` | `created_at` | Timestamp |

### HistoryEntry
| SEL Key        | SQL Column      | Type      |
|----------------|-----------------|-----------|
| `id`           | `id`            | Long      |
| `noteId`       | `note_id`       | Long      |
| `suggestionId` | `suggestion_id` | Long      |
| `action`       | `action`        | String    |
| `createdAt`    | `created_at`    | Timestamp |

---

## SQL Translation

| SEL                                | SQL                                                     |
|------------------------------------|---------------------------------------------------------|
| `{ "prop": "status" }`             | `s.status`                                              |
| `{ "field": "X" }`                 | `(SELECT fv.field_value FROM field_value fv WHERE ...)` |
| `{ "==": [a, b] }`                 | `(a = b)`                                               |
| `{ "contains": [a, b] }`           | `a LIKE '%' \|\| b \|\| '%'`                            |
| `{ "isEmpty": x }`                 | `COALESCE(x, '') = ''`                                  |
| `{ "len": x }`                     | `COALESCE(LENGTH(x), 0)`                                |
| `{ "and": [...] }`                 | `(... AND ... AND ...)`                                 |
| `{ "exists": { "query": {...} } }` | `EXISTS (SELECT 1 FROM ... WHERE ...)`                  |
| `{ "query": {..., "result": x} }`  | `(SELECT x FROM ... WHERE ...)`                         |
| `{ "ref": ["n", "id"] }`           | `n.id`                                                  |
