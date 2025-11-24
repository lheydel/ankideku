# AnkiDeku Workflow Architecture Analysis

**Analysis Date:** 2025-11-24
**Perspective:** Generic note processing system (not just linguistic)

## Current Workflow: Field Update Workflow

### What It Does:
1. **Read** notes from deck(s)
2. **Analyze** each note (LLM decides if changes needed)
3. **Generate** field updates
4. **Write** suggestion files with:
   - `noteId`
   - `original` (complete note object)
   - `changes` (field name → new value)
   - `reasoning`
5. User reviews and accepts/rejects

### Current Implementation Problems:

#### 1. **Inefficient Data Transfer**
- **Problem:** Every suggestion file contains the FULL original note (all fields, tags, cards, mod timestamp, deck name)
- **Why it's bad:**
  - Original note is already in the deck cache
  - We're duplicating ~500-1000 bytes per suggestion
  - For 8k cards, that's 4-8 MB of redundant data
  - Slows file I/O, wastes disk space
- **Better approach:** Store only `noteId` + `changes` + `reasoning`
  - Frontend fetches original from cache using noteId
  - 90% size reduction per suggestion file

#### 2. **Wasteful Deck Reading**
- **Problem:** LLM re-reads entire deck file(s) for each batch/agent
- **Why it's bad:**
  - 8k card deck = ~5-10 MB JSON file
  - Read 8 times if using 8 parallel agents
  - Token cost for context
- **Better approach:** Backend sends ONLY the cards that need processing
  - Pre-filter, slice into batches
  - Each batch is self-contained
  - No file reading by LLM needed

#### 3. **Suggestion Format Overhead**
- **Problem:** Suggestion file structure is verbose
- **Current:** Individual JSON files with full schema
- **Better approach:** Single structured batch response
  ```json
  {
    "suggestions": [
      { "noteId": 123, "changes": {...}, "reasoning": "..." },
      { "noteId": 456, "changes": {...}, "reasoning": "..." }
    ]
  }
  ```

#### 4. **No Incremental State**
- **Problem:** If LLM fails mid-batch, all work lost
- **Better approach:** Backend tracks which cards were processed
  - Checkpoint after each batch
  - Resume from last successful position
  - LLM doesn't manage state

---

## Generalizable Workflow Types

Looking beyond linguistic tasks, Anki decks could need:

### 1. **Field Update Workflow** (Current)
- **Pattern:** Read note → Analyze → Update fields
- **Examples:**
  - Add example phrases (linguistic)
  - Fix translations (linguistic)
  - Add mnemonics (memory)
  - Tag cards by difficulty (metadata)
  - Normalize formatting (cleanup)
  - Add pronunciation guides (linguistic)

### 2. **Tagging/Classification Workflow** (Future)
- **Pattern:** Read note → Classify → Add/remove tags
- **Examples:**
  - Auto-tag by topic (history, science, etc.)
  - Flag cards for review based on complexity
  - Mark mature vs immature cards
  - Classify by difficulty level (N5, N4, N3...)
  - Detect duplicates or near-duplicates

### 3. **Quality Control Workflow** (Future)
- **Pattern:** Read note → Validate → Flag issues
- **Examples:**
  - Detect formatting errors
  - Find broken HTML/formatting
  - Identify missing required fields
  - Spot inconsistencies (e.g., kanji without kana)
  - Check for inappropriate content

### 4. **Batch Generation Workflow** (Future)
- **Pattern:** Input concept → Generate multiple notes
- **Examples:**
  - Generate cloze deletions from text
  - Create cards from definitions
  - Extract Q&A pairs from documents
  - Generate conjugation cards (verb → all forms)

### 5. **Cross-Note Analysis Workflow** (Future)
- **Pattern:** Read multiple notes → Find relationships → Suggest changes
- **Examples:**
  - Detect duplicate concepts
  - Find related cards that should be linked
  - Identify gaps in coverage
  - Suggest card suspension based on overlaps

---

## Optimization Opportunities (Workflow-Agnostic)

### A. **Batch Processing Architecture**

**Current:**
```
LLM reads deck files → Processes individual cards → Writes individual suggestion files
```

**Optimized:**
```
Backend:
  1. Read deck cache
  2. Apply pre-filters (skip cards based on rules)
  3. Slice into batches (50-100 cards)
  4. Prepare batch payload (cards + context)

LLM:
  1. Receive batch of cards (no file reading)
  2. Process all cards in batch
  3. Return single JSON response with all suggestions

Backend:
  1. Parse batch response
  2. Write individual suggestion files (for UI compatibility)
  3. Trigger file watcher
```

**Benefits:**
- 90% cost reduction (prompt caching)
- No duplicate data transfer
- Faster processing
- Easier error handling
- Works for ANY workflow type

### B. **Suggestion File Format Optimization**

**Current format:**
```json
{
  "noteId": 12345,
  "original": { /* 500+ bytes of duplicate data */ },
  "changes": { "Field": "value" },
  "reasoning": "..."
}
```

**Optimized format:**
```json
{
  "noteId": 12345,
  "changes": { "Field": "value" },
  "reasoning": "..."
}
```

**Frontend adaptation:**
```typescript
// Instead of: suggestion.original
// Do this:
const original = deckCache.getNoteById(suggestion.noteId);
```

**Benefits:**
- 90% smaller files
- Faster file I/O
- Less disk space
- Frontend already has deck cache!

### C. **Pre-Filtering Pipeline**

Create a **filter chain** before LLM processing:

```typescript
interface Filter {
  name: string;
  shouldSkip: (note: Note) => boolean;
}

const filters: Filter[] = [
  // Skip if field already has content
  { name: "hasExample", shouldSkip: n => !!n.fields.Example?.value },

  // Skip common words (frequency list)
  { name: "commonWord", shouldSkip: n => COMMON_WORDS.has(n.fields.Word?.value) },

  // Skip by tag
  { name: "excluded", shouldSkip: n => n.tags.includes("skip-ai") },

  // Custom user rules
  ...userDefinedFilters
];

const cardsToProcess = allCards.filter(card =>
  !filters.some(f => f.shouldSkip(card))
);
```

**Benefits:**
- Reduce LLM calls by 30-70%
- User can define custom skip rules
- Applies to ANY workflow
- Fast programmatic execution

### D. **Workflow Configuration Schema**

Make workflows **declarative and reusable**:

```typescript
interface WorkflowConfig {
  id: string;
  name: string;
  type: 'field-update' | 'tagging' | 'quality-control' | 'generation';

  // Pre-processing
  filters?: FilterConfig[];

  // LLM config
  model: 'haiku' | 'sonnet' | 'opus';
  batchSize: number;

  // Prompt template
  systemPrompt: string;
  userPromptTemplate: string; // Can use variables: {{deckName}}, {{cardCount}}

  // Output schema
  outputSchema: {
    noteId: 'required',
    changes?: Record<string, string>,
    tags?: string[],
    reasoning: 'required',
    // ... extensible
  };

  // Post-processing
  validation?: ValidationRule[];
}
```

**User creates workflows via UI:**
```typescript
const workflow = {
  name: "Add Example Phrases",
  type: "field-update",
  filters: [
    { field: "Example", operator: "isEmpty" },
    { field: "Word", operator: "notInList", value: COMMON_WORDS }
  ],
  model: "haiku", // Cheaper for this task
  batchSize: 100,
  promptTemplate: "Add example phrases for {{deckName}} cards...",
  outputSchema: { changes: { Example: "string" }, reasoning: "string" }
};
```

---

## Proposed Architecture

### 1. **Workflow Executor Service** (Backend)
```typescript
class WorkflowExecutor {
  async execute(workflow: WorkflowConfig, deckName: string): Promise<SessionId> {
    // 1. Load deck from cache
    const notes = await cache.getDeckNotes(deckName);

    // 2. Apply filters
    const filtered = this.applyFilters(notes, workflow.filters);

    // 3. Create batches
    const batches = this.createBatches(filtered, workflow.batchSize);

    // 4. Process batches (with caching)
    for (const batch of batches) {
      const response = await this.processBatch(batch, workflow);
      await this.writeSuggestions(response);
    }
  }

  private async processBatch(batch: Note[], workflow: WorkflowConfig) {
    // Prepare prompt with batch of cards
    const prompt = this.buildPrompt(batch, workflow);

    // Call LLM (with caching enabled)
    const response = await llm.complete(prompt, {
      cache: true,
      model: workflow.model
    });

    return this.parseResponse(response, workflow.outputSchema);
  }
}
```

### 2. **Suggestion Format** (Minimal)
```json
{
  "noteId": 123,
  "type": "field-update",
  "changes": { "Example": "新しい例文" },
  "reasoning": "Added contextual example"
}
```

### 3. **Frontend Adaptation**
```typescript
// Load suggestion
const suggestion = await loadSuggestion(suggestionId);

// Fetch original note from cache (not from suggestion file)
const original = useStore(s => s.deckCache.notes[suggestion.noteId]);

// Display comparison
<ComparisonView
  original={original}
  changes={suggestion.changes}
  reasoning={suggestion.reasoning}
/>
```

---

## Migration Path

### Phase 1: **Backend Batch Processing** (No breaking changes)
- Keep suggestion file format as-is
- Backend handles batching internally
- LLM receives batches, returns batch response
- Backend writes individual files (for compatibility)
- **Result:** 90% cost reduction, no frontend changes needed

### Phase 2: **Optimize Suggestion Format**
- Remove `original` field from suggestions
- Frontend uses deck cache for original note
- **Result:** 90% smaller files, faster I/O

### Phase 3: **Add Pre-Filtering**
- Build filter pipeline
- Let users define skip rules
- **Result:** 30-70% fewer LLM calls

### Phase 4: **Workflow System**
- Create workflow config schema
- Build workflow UI
- Support multiple workflow types
- **Result:** Extensible system for future needs

---

## Immediate Action Items

1. **Prototype batch processing** (Phase 1)
   - Test with 100 cards
   - Measure cost savings
   - Ensure quality maintained

2. **Measure current overhead**
   - File sizes (suggestions vs needed)
   - Disk I/O time
   - Token usage breakdown

3. **Design workflow config schema**
   - Define interfaces
   - Plan UI mockups
   - Get user feedback

4. **Implement smart filters**
   - Common word list
   - Field existence checks
   - User-defined rules

---

## Open Questions

1. **Workflow UI:** How should users create/configure workflows?
2. **Prompt templates:** Fixed per workflow or user-editable?
3. **Model selection:** Automatic based on complexity, or user choice?
4. **Error handling:** Retry failed batches? Skip? Manual review?
5. **Caching strategy:** How long to cache prompts? Per-session or persistent?
6. **Multi-step workflows:** Should workflows support chaining? (e.g., Generate → Validate → Tag)
