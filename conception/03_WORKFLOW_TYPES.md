# 03 - Workflow Types

**Core insight:** AnkiDeku supports different types of LLM-powered workflows, each with distinct patterns and requirements.

---

## Workflow 1: Field Update

### Description
LLM takes existing cards and improves them based on user instructions.

### Pattern
```
INPUT: Existing cards from deck
PROCESS: LLM analyzes and suggests field modifications
OUTPUT: Suggestions to update fields
```

### Examples
- Add example phrases to vocabulary cards
- Refine translations for better nuance
- Add mnemonics to difficult cards
- Improve card formatting/structure
- Add pronunciation guides
- Enhance definitions

### Characteristics
- **Input:** Existing deck notes
- **Output:** Field modifications (changes to existing cards)
- **Granularity:** 1 suggestion = 1 card modified
- **User action:** Accept/reject each field change
- **Deck impact:** Cards improved, count stays same

### LLM Task Type
- **Primarily:** Evaluative + Generative
- Evaluate current card quality/content
- Generate improved content
- Apply user-defined improvement criteria

---

## Workflow 2: Content Creation

### Description
User provides source documents (lessons, books, PDFs, text), LLM suggests new cards to create.

### Pattern
```
INPUT: Documents/content + (optionally) existing deck for context
PROCESS: LLM extracts concepts and generates cards
OUTPUT: Suggestions to create new cards
```

### Examples
- Generate vocabulary cards from textbook chapter
- Extract Q&A pairs from lecture notes
- Create cloze deletions from article
- Generate conjugation cards from grammar guide
- Build card set from definition list

### Characteristics
- **Input:** External documents/text
- **Output:** Complete new cards (all fields populated)
- **Granularity:** 1 suggestion = 1 new card to create
- **User action:** Accept/reject each new card
- **Deck impact:** Cards added, count increases

### LLM Task Type
- **Primarily:** Generative + Analytical
- Extract key concepts from content
- Structure information as flashcards
- Generate appropriate fields (front/back, examples, etc.)
- Ensure pedagogical soundness

### Additional Complexity
- Must parse/understand various document formats
- May need to chunk large documents
- Context management (what's already been extracted)

---

## Workflow 2.5: Cross-Reference Correction

### Description
Hybrid of Workflow 1 and 2. User provides reference documents, LLM cross-references existing cards against authoritative sources to find and correct errors.

### Pattern
```
INPUT: Existing deck + Reference documents
PROCESS: LLM compares cards to reference material, finds discrepancies
OUTPUT: Suggestions to correct/improve cards based on references
```

### Examples
- Check vocabulary cards against dictionary definitions
- Verify historical facts against textbook
- Validate translations against authoritative sources
- Correct scientific terminology using textbook
- Update outdated information from new edition

### Characteristics
- **Input:** Existing deck + reference materials
- **Output:** Field modifications with citations/reasoning
- **Granularity:** 1 suggestion = 1 card corrected
- **User action:** Accept/reject each correction
- **Deck impact:** Cards improved (corrections), count stays same

### LLM Task Type
- **Primarily:** Evaluative + Comparative
- Compare existing content to reference
- Detect discrepancies or errors
- Suggest corrections based on authoritative source
- Provide reasoning/citation

### Additional Complexity
- Must track which reference supports which correction
- Handle conflicting sources
- Distinguish errors from valid alternatives
- Provide clear reasoning (why reference is correct)

---

## Workflow 3: Deduplication & Disambiguation

### Description
LLM analyzes deck for duplicates or overly similar cards, suggests consolidation or removal.

### Pattern
```
INPUT: Existing deck (cross-card analysis)
PROCESS: LLM identifies duplicates/near-duplicates, determines best action
OUTPUT: Suggestions to modify/merge/remove cards
```

### Examples
**Exact duplicates:**
- Two cards with identical content → suggest removing one

**Near-duplicates:**
- "ashi → jambe" and "ashi → pied" → suggest merging to "ashi → jambe, pied" and removing other

**Semantic duplicates:**
- "大きい → grand" and "大きな → grand" → flag as potentially redundant, suggest differentiation

**Overlapping concepts:**
- Multiple cards for same word with different nuances → suggest consolidation or clarification

### Characteristics
- **Input:** Entire deck (must analyze relationships)
- **Output:** Suggestions to modify, merge, or delete cards
- **Granularity:** 1 suggestion file per card affected, but suggestions are grouped/linked
  - Example: Merge operation creates 3 suggestion files:
    - Suggestion for card A (modify to "jambe, pied")
    - Suggestion for card B (delete)
    - Both linked via groupId for coordinated UX
- **User action:** Accept/reject each suggestion (but UI shows grouped context)
- **Deck impact:** Cards modified AND count may decrease

### LLM Task Type
- **Primarily:** Analytical + Comparative
- Find similar cards across deck
- Determine if similarity is problematic
- Decide best resolution (merge vs differentiate vs remove)
- Maintain card quality while reducing redundancy

### Additional Complexity
- **Cross-card dependencies:** Related suggestions must be linked via groupId
- **Suggestion types vary:** Modify, merge, delete (not just field updates)
- **UI complexity:** Must show grouped suggestions together for context
- **Safety concerns:** Deletion is more dangerous than modification
- **Undo complexity:** Merging cards harder to reverse than field edits
- **Suggestion linking:** Need mechanism to group related suggestions (e.g., groupId field)
  - Each suggestion file remains independent (1 file = 1 card action)
  - But UI presents them together for informed decision-making

---

## Comparison Matrix

| Aspect | Field Update | Content Creation | Cross-Reference | Deduplication |
|--------|-------------|------------------|-----------------|---------------|
| **Input** | Existing cards | Documents | Cards + Docs | Entire deck |
| **Analysis scope** | Single card | Document chunks | Card vs reference | Cross-card |
| **Output type** | Modify fields | New cards | Modify fields | Modify/merge/delete |
| **Deck count** | Unchanged | Increases | Unchanged | Decreases |
| **LLM task** | Evaluate + Generate | Generate + Extract | Compare + Evaluate | Analyze + Compare |
| **Complexity** | Low | Medium | Medium-High | High |
| **Safety risk** | Low (non-destructive) | Low (additive) | Low (corrections) | Medium (deletions) |

---

## Shared Patterns Across Workflows

### Common elements:
1. **User provides natural language instructions**
2. **LLM processes based on instructions**
3. **Suggestions generated (1 per card or action)**
4. **User reviews each suggestion individually**
5. **Changes applied only after acceptance**
6. **History maintained for undo**

### Key differences:
1. **Input sources vary** (deck, documents, deck+docs, entire deck)
2. **Analysis scope varies** (single card, document, card-vs-ref, cross-card)
3. **Output actions vary** (modify, create, merge, delete)
4. **Complexity varies** (simple to complex)

---

## Architecture Implications

### Must support:
- **Variable input sources** (deck cache, document upload, combined)
- **Different analysis scopes** (per-card, per-document-chunk, cross-card)
- **Multiple suggestion types** (modify field, create card, merge cards, delete card)
- **Cross-card references** (for deduplication workflow)
- **Document parsing** (for content creation/cross-reference)

### Suggestion schema must handle:
```typescript
// Current implementation (Workflow 1 - Field Update)
interface CardSuggestion {
  noteId: number;
  original: Note;  // Snapshot of card at suggestion-time (gives review context)
  changes: Record<string, string>;  // AI-suggested field changes
  reasoning: string;  // Why the AI made these suggestions
  accepted?: boolean | null;  // null = pending, true = accepted, false = rejected
  editedChanges?: Record<string, string>;  // User manual edits before accepting
}

// Extended for all workflow types
interface Suggestion extends CardSuggestion {
  type?: 'modify' | 'create' | 'delete';  // Defaults to 'modify' for backwards compatibility
  groupId?: string;  // Links related suggestions (for deduplication)
  groupContext?: {
    relatedNoteIds: number[];  // Other cards in this group
    groupReasoning: string;     // Why these are grouped
  };
}

// Key principles:
// - `original` provides review-time context (what card looked like when AI analyzed it)
// - `changes` are AI suggestions (immutable once created)
// - `editedChanges` are user modifications (can edit before accepting)
// - Backend auto-populates `original` when saving suggestion
// - 1 file per card affected, linked via groupId if needed

// Examples:
// Workflow 1 (Field Update):
// { noteId: 123, original: {...}, changes: { Example: "新しい例" }, reasoning: "..." }

// Workflow 2 (Content Creation):
// { noteId: NEW_ID, type: 'create', changes: { Front: "word", Back: "definition" }, reasoning: "..." }
// Note: `original` would be empty/minimal for new cards

// Workflow 3 (Deduplication merge):
// File 1: { noteId: 123, type: 'modify', changes: { Back: 'jambe, pied' }, groupId: 'dup-001', ... }
// File 2: { noteId: 456, type: 'delete', groupId: 'dup-001', groupContext: { relatedNoteIds: [123], ... } }
```

### UI must handle:
- **Single card view** (Field Update)
- **New card preview** (Content Creation)
- **Card + reference view** (Cross-Reference)
- **Multi-card comparison** (Deduplication)

---

## Workflow Priority & Dependencies

### Phase 1 (Current): Field Update
- Simplest workflow
- Most common use case
- Establishes core patterns
- Foundation for others

### Phase 2: Content Creation
- Adds document input
- New suggestion type (create vs modify)
- Builds on Field Update patterns

### Phase 3: Cross-Reference
- Combines Field Update + Content Creation concepts
- Adds comparative analysis
- More complex reasoning required

### Phase 4: Deduplication
- Most complex (cross-card analysis)
- New suggestion types (merge, delete)
- Requires multi-card UI
- Builds on all previous learnings

---

## Open Questions Per Workflow

### Field Update (Workflow 1):
- ✅ Well understood
- ✅ Currently implemented
- Optimization opportunities identified

### Content Creation (Workflow 2):
- What document formats to support? (PDF, TXT, DOCX, MD?)
- How to chunk large documents?
- How to handle context (previous chunks)?
- How does user specify which deck to add to?

### Cross-Reference (Workflow 2.5):
- How to link corrections to reference sources?
- How to handle conflicting references?
- Should citations be added to cards?
- How to present "reference says X, card says Y" comparison?

### Deduplication (Workflow 3):
- How to define "too similar"? (user configurable? LLM judgment?)
- How to show grouped suggestions in UI? (expand/collapse group view?)
- How to generate groupId for related suggestions?
- What happens if user accepts one suggestion in group but rejects another?
- How to handle merge operations safely?
- How to undo merges/deletions?
- Should deduplication be automatic detection or user-initiated?

---

## Next Steps

1. **Validate workflow definitions** - Do these cover the main use cases?
2. **Explore Content Creation workflow** - Document parsing, card generation patterns
3. **Define suggestion schema** - Unified format for all workflow types
4. **Consider UI implications** - How each workflow is presented
5. **Identify shared infrastructure** - What's common across workflows?
