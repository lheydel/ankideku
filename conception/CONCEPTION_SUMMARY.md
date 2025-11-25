# AnkiDeku Conception Summary

## Core Value Proposition
AI-powered Anki deck improvement with context-aware suggestions. Users describe improvements in natural language; system generates suggestions for manual review.

**Key differentiator**: Intelligence, not automation. LLM analyzes content contextually (not regex/pattern matching).

## Non-Negotiables

1. **Safety-first**: No auto-apply. Every suggestion manually reviewed. History for undo.
2. **1 suggestion = 1 card**: Atomic unit of work. Accept/reject individually.
3. **Natural language input**: Users describe intent, system figures out execution.
4. **LLM-first**: Focus on tasks requiring intelligence (generation, evaluation, linguistic analysis).

## Target Users
- Language learners with large vocabulary decks (8k+ cards)
- Content creators maintaining educational materials
- Students optimizing study decks

## Value Proposition Math
- Manual work: 4-8 min/card × 8000 cards = **500-1000 hours**
- AnkiDeku: LLM generates + user reviews (10-15 sec/card) = **~25 hours**
- **Time savings: 95-97%** while maintaining/improving quality

## Workflow Types

### Workflow 1: Field Update (Current)
- Input: Existing deck
- Output: Field modifications
- Example: Add example phrases, refine translations

### Workflow 2: Content Creation (Future)
- Input: Documents/text
- Output: New cards
- Example: Generate cards from textbook

### Workflow 2.5: Cross-Reference (Future)
- Input: Deck + reference documents
- Output: Corrections based on authoritative sources

### Workflow 3: Deduplication (Future)
- Input: Entire deck (cross-card analysis)
- Output: Merge/delete suggestions with groupId linking

## Architecture: Industry Standard

### Separation of Concerns
```
UI Layer: Deck selection, prompt input, suggestion review
    ↓
Backend Orchestration: Load deck, batch by tokens, manage LLM calls, write suggestions, emit WebSocket
    ↓
LLM Layer (via LLMService interface): Only does intelligent analysis, returns JSON array
```

### Key Principles
- LLM does ONLY intelligent work (no file I/O, no workflow decisions)
- Backend handles ALL orchestration (batching, I/O, state tracking)
- Token-based dynamic batching (not fixed card count)
- Direct WebSocket emission (no file watcher)
- Retry logic: 1-2 retries on parse errors

### Suggestion Schema
```typescript
interface CardSuggestion {
  noteId: number;
  original: Note;           // Auto-populated by backend from cache
  changes: Record<string, string>;
  reasoning: string;
  accepted?: boolean | null;
  editedChanges?: Record<string, string>;  // User modifications
  type?: 'modify' | 'create' | 'delete';   // Future workflows
  groupId?: string;                         // Links related suggestions
}
```

### LLM Service Interface
```typescript
interface LLMService {
  analyzeBatch(cards: Note[], userPrompt: string): Promise<LLMResponse>;
  checkHealth(): Promise<LLMHealthStatus>;
}
// Implementations: ClaudeCodeService (current), ClaudeAPIService (future), etc.
```

## Cost Optimization (Priority Order)

### 1. Output Token Reduction (73% of cost)
- Concise reasoning: ~15 tokens vs ~30 tokens per suggestion
- Potential savings: 50% output reduction

### 2. Input Data Trimming (27% of cost)
- Remove empty fields
- Only send task-relevant fields
- Savings: ~10-15%

### 3. Batch Size Optimization
- Token-based batching maximizes efficiency
- Savings: ~5-10%

### Prompt Caching
- Skip for Workflow 1 (only ~3% savings)
- Critical for Workflow 2.5 with large reference documents

## Quality Requirements
- Suggestions must be worth user's review time
- High precision > high quantity
- "Would an expert accept this?" quality bar
- Acceptance rate = key quality metric

## What NOT to Do
- Don't auto-apply changes
- Don't minimize review time (it's the value delivery)
- Don't over-filter (show all worthwhile suggestions)
- Don't use LLM for rule-based operations (unless as tool calls)
- Don't sacrifice quality for speed

## Key Decisions Made
- No rule-based pre-filtering (send whole deck in batches)
- Model selection deferred (use Sonnet consistently for now)
- Progress tracking: already supported (review while processing)
- Partial completion: already supported

## Open Questions
- Tokenizer choice for batch size calculation
- How to instruct LLM for concise reasoning without quality loss
- Progress granularity (per-batch? time-based?)
- Response validation strictness

## Next Implementation Steps
1. Design SessionOrchestrator service
2. Design LLM service abstraction (LLMService interface)
3. Optimize prompts (remove orchestration logic)
4. Define error handling strategy
5. Plan migration from current naive implementation
