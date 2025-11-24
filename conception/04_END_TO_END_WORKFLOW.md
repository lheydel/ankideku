# 04 - End-to-End Workflow: Field Update (Workflow 1)

**Goal:** Replace the current naive "spawn Claude agents to write files" approach with an industry-standard LLM processing pipeline.

---

## Current State (Naive Implementation)

### What Happens Now:
1. User selects deck, writes prompt
2. Backend creates session, generates prompt for Claude Code
3. Claude Code spawns Task agents
4. Each agent reads deck files directly
5. Each agent writes individual suggestion JSON files
6. File watcher detects new files
7. Backend emits WebSocket events
8. Frontend receives suggestions in real-time
9. User reviews each suggestion

### Problems with Current Approach:
- ❌ LLM manages file I/O (should be backend's job)
- ❌ LLM decides workflow execution (batching, parallelization)
- ❌ Verbose prompts with unnecessary instructions
- ❌ LLM sees full deck files (redundant data transfer)
- ❌ No separation of concerns (LLM does everything)
- ❌ Difficult to optimize (all logic in LLM prompt)
- ❌ Hard to test/debug (black box execution)
- ❌ Inconsistent output format (LLM doesn't always follow schema)

---

## Desired State (Industry Standard)

### Separation of Concerns:

```
┌─────────────────────────────────────────────────────────────┐
│ USER INTERFACE LAYER                                        │
│ - Deck selection                                            │
│ - Natural language prompt input                             │
│ - Real-time suggestion display                              │
│ - Review/edit/accept/reject                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ SESSION ORCHESTRATION LAYER (Backend)                       │
│ - Load entire deck from cache (no pre-filtering)           │
│ - Tokenize cards to calculate batch sizes                  │
│ - Prepare token-based batches                               │
│ - Manage LLM calls via LLMService interface                 │
│ - Parse & validate LLM responses (retry 1-2x on errors)    │
│ - Write suggestion files (SuggestionWriter)                 │
│ - Emit real-time WebSocket updates (direct, no watcher)    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ LLM LAYER (via LLMService interface)                       │
│ - Current: ClaudeCodeService (spawns Claude Code)          │
│ - Future: ClaudeAPIService, LocalModelService, etc.        │
│                                                             │
│ What LLM receives:                                          │
│ - Batch of cards + user instructions                        │
│ - Clean, focused prompt (no orchestration logic)            │
│                                                             │
│ What LLM returns:                                           │
│ - Structured JSON array of suggestions                      │
│ - Format: [{ noteId, changes, reasoning }, ...]            │
│                                                             │
│ NO file I/O, NO workflow decisions                          │
└─────────────────────────────────────────────────────────────┘
```

### Key Principles:
1. **LLM does ONLY the intelligent work** (analysis, generation)
2. **Backend handles ALL orchestration** (batching, filtering, I/O)
3. **Clear interfaces** between layers
4. **Testable components** at each layer
5. **Optimizable independently** (change batching without changing LLM prompts)

---

## Detailed End-to-End Flow

### Phase 1: Session Initialization

**User Action:**
- Selects deck: "JP Voc"
- Enters prompt: "Add example phrases to cards"
- Clicks "Start"

**Backend (Session Service):**
```typescript
1. Create session ID
2. Store session request {
     sessionId,
     prompt: "Add example phrases...",
     deckName: "JP Voc",
     timestamp
   }
3. Load deck notes from cache (entire deck, no pre-filtering)
4. Tokenize cards to determine batch sizes dynamically
5. Create batches based on token count (not fixed card count)
6. Create initial session state:
   {
     state: 'pending',
     totalCards: 8133,
     processedCards: 0,
     totalSuggestions: 0
   }
7. Emit session:start event to frontend
8. Start processing worker
```

**Frontend:**
- Shows "Session started" message
- Displays progress: "0 / 8,133 cards processed"
- Queue is empty, waiting for suggestions

---

### Phase 2: Batch Processing

**Backend (Processing Worker):**

```typescript
async function processBatch(cards: Note[], sessionId: string, userPrompt: string) {
  // 1. Prepare batch prompt
  const batchPrompt = buildBatchPrompt({
    userRequest: userPrompt,
    cards: cards,
    outputFormat: SUGGESTION_SCHEMA
  });

  // 2. Call LLM Service (abstracted)
  let response;
  let retries = 0;
  const MAX_RETRIES = 2;

  while (retries <= MAX_RETRIES) {
    try {
      response = await llmService.analyzeBatch(batchPrompt, {
        temperature: 0.3, // consistent results
        maxTokens: 4000
      });
      break; // Success, exit retry loop
    } catch (error) {
      if (error.type === 'parse_error' && retries < MAX_RETRIES) {
        retries++;
        console.log(`Parse error, retrying (${retries}/${MAX_RETRIES})...`);
        continue;
      }
      throw error; // Give up after max retries or other error types
    }
  }

  // 3. Parse and validate response
  const suggestions = parseLLMResponse(response);
  const validSuggestions = suggestions.filter(validateSuggestion);

  // 4. Write suggestions in real-time and emit WebSocket events
  for (const suggestion of validSuggestions) {
    // Write suggestion file
    await suggestionWriter.writeSuggestion(sessionId, suggestion);

    // Immediately emit WebSocket event (no file watcher needed)
    wsServer.emit('suggestion:new', {
      sessionId,
      suggestion: await loadCompleteSuggestion(sessionId, suggestion.noteId)
    });
  }

  // 5. Update session state
  await updateSessionState(sessionId, {
    processedCards: previousCount + cards.length,
    totalSuggestions: previousCount + validSuggestions.length
  });

  return validSuggestions.length;
}
```

**Key Optimizations:**
- **Dynamic batching:** Batch size determined by tokenizing cards (not fixed count)
- **Structured output:** LLM returns array of suggestions, not files
- **Validation & retry:** Backend validates schema, retries 1-2 times on parse errors
- **Real-time updates:** Suggestions written and WebSocket emitted immediately (no file watcher)
- **Progress tracking:** State updated after each batch
- **User can review while processing continues** (already implemented)

---

### Phase 3: LLM Interaction

**What LLM Receives:**

```typescript
// System Prompt (cached across batches)
const systemPrompt = `
You are an expert at analyzing Anki flashcards and suggesting improvements.

Your task: Analyze cards and suggest field modifications based on user instructions.

Output format: JSON array of suggestions, one per card needing changes.
Schema:
{
  "suggestions": [
    {
      "noteId": number,
      "changes": { "FieldName": "new value" },
      "reasoning": "why this change improves the card"
    }
  ]
}

Only suggest changes for cards that need improvement. Skip cards that are already good.
`;

// User Prompt (per batch, changes)
const userPrompt = `
User request: "${userRequest}"

Cards to analyze:
${cards.map((card, idx) => `
Card ${idx + 1}:
  ID: ${card.noteId}
  Model: ${card.modelName}
  Fields: ${JSON.stringify(card.fields, null, 2)}
`).join('\n')}

Analyze these cards and suggest improvements following the user's request.
Return ONLY the JSON output, no other text.
`;
```

**What LLM Returns:**

```json
{
  "suggestions": [
    {
      "noteId": 1234,
      "changes": {
        "Example": "この言葉を使う"
      },
      "reasoning": "Added contextual example phrase using the vocabulary word"
    },
    {
      "noteId": 5678,
      "changes": {
        "Example": "- 彼は走る\n- 毎日走る"
      },
      "reasoning": "Added multiple examples showing different contexts (subject + daily routine)"
    }
  ]
}
```

**LLM Does NOT:**
- Read/write files
- Manage batching
- Track progress
- Handle errors
- Make workflow decisions

**LLM ONLY:**
- Analyzes cards
- Generates suggestions
- Returns structured data

---

### Phase 4: Suggestion Storage

**Backend (after receiving LLM response):**

```typescript
async function writeSuggestionFile(sessionId: string, suggestion: LLMSuggestion) {
  // 1. Fetch original note from cache
  const originalNote = await cache.getNoteById(suggestion.noteId);

  // 2. Build complete suggestion object
  const completeSuggestion: CardSuggestion = {
    noteId: suggestion.noteId,
    original: originalNote, // Auto-populated from cache
    changes: suggestion.changes,
    reasoning: suggestion.reasoning,
    accepted: null
  };

  // 3. Write to file
  const filePath = `database/ai-sessions/${sessionId}/suggestions/suggestion-${noteId}.json`;
  await fs.writeFile(filePath, JSON.stringify(completeSuggestion, null, 2));

  // File watcher detects this and emits WebSocket event
}
```

**Key Points:**
- LLM only provides: noteId, changes, reasoning
- Backend adds: original note (from cache)
- Backend manages: file paths, JSON formatting
- File watcher handles: WebSocket notifications

---

### Phase 5: Real-Time Updates

**Backend (Direct WebSocket Emission):**
```typescript
// No file watcher needed - emit directly after writing suggestion
async function emitSuggestion(sessionId: string, suggestion: CardSuggestion) {
  wsServer.emit('suggestion:new', {
    sessionId,
    suggestion
  });
}

// Periodically emit state updates
setInterval(() => {
  wsServer.emit('state:change', {
    sessionId,
    state: getCurrentSessionState(sessionId)
  });
}, 1000);
```

**Frontend (WebSocket Handler):**
```typescript
useWebSocket({
  onSuggestion: (suggestion) => {
    addToQueue(suggestion);
    if (!selectedCard) setSelectedCard(suggestion);
  },

  onStateChange: (state) => {
    updateProgress(state.processedCards, state.totalCards);
    if (state.state === 'completed') {
      showNotification('Processing complete');
    }
  }
});
```

**Key Change:**
- Removed file watcher dependency
- SuggestionWriter service calls WebSocket emit directly after writing file
- Simpler, more direct flow with fewer moving parts

---

### Phase 6: User Review

**Frontend UI:**
- Queue shows all suggestions (left panel)
- Comparison view shows current suggestion (center)
  - Original fields
  - Suggested changes (highlighted)
  - Reasoning
- User can:
  - Accept (as-is)
  - Edit (modify changes before accepting)
  - Reject
- Keyboard shortcuts: `A` (accept), `R` (reject), `E` (edit)

**User Actions:**
```typescript
// Accept
onAccept(suggestion) => {
  await api.updateNote(suggestion.noteId, suggestion.changes);
  await api.saveHistory(sessionId, {
    action: 'accept',
    noteId: suggestion.noteId,
    changes: suggestion.changes
  });
  removeFromQueue(suggestion);
  showNextSuggestion();
}

// Edit then Accept
onEditAccept(suggestion, editedChanges) => {
  await api.updateNote(suggestion.noteId, editedChanges);
  await api.saveHistory(sessionId, {
    action: 'accept',
    noteId: suggestion.noteId,
    changes: suggestion.changes,
    editedChanges: editedChanges
  });
  removeFromQueue(suggestion);
  showNextSuggestion();
}

// Reject
onReject(suggestion) => {
  await api.saveHistory(sessionId, {
    action: 'reject',
    noteId: suggestion.noteId,
    changes: suggestion.changes
  });
  removeFromQueue(suggestion);
  showNextSuggestion();
}
```

---

## Comparison: Naive vs Industry Standard

| Aspect | Naive (Current) | Industry Standard (Proposed) |
|--------|-----------------|------------------------------|
| **LLM Role** | Orchestrates everything | Only does intelligent analysis |
| **File I/O** | LLM writes files | Backend writes files + emits WebSocket |
| **Batching** | LLM decides | Backend manages (token-based) |
| **Data Transfer** | LLM reads full deck files | Backend sends batches of cards |
| **Output Format** | LLM writes JSON files | LLM returns JSON array |
| **Error Handling** | LLM retries | Backend validates & retries (1-2x) |
| **Progress Tracking** | LLM manages state | Backend tracks state |
| **Real-time Updates** | File watcher → WebSocket | Direct WebSocket emission |
| **LLM Provider** | Hardcoded Claude Code | Abstracted via LLMService interface |
| **Optimization** | Change prompt | Change backend logic independently |
| **Testing** | Hard (LLM black box) | Easy (test each layer in isolation) |
| **Cost** | High (verbose prompts, no batching) | Lower (token-based batching) |
| **Reliability** | LLM might fail to follow instructions | Backend validates/corrects, retry logic |

---

## Key Architectural Changes

### 1. Backend Orchestration Service

```typescript
class SessionOrchestrator {
  async executeSession(sessionId: string): Promise<void> {
    const session = await this.loadSession(sessionId);
    const cards = await this.loadDeckCards(session.deckName);
    const filtered = await this.applyFilters(cards, session.filters);
    const batches = this.createBatches(filtered, BATCH_SIZE);

    for (const batch of batches) {
      try {
        await this.processBatch(sessionId, batch, session.prompt);
      } catch (error) {
        await this.handleBatchError(sessionId, batch, error);
      }
    }

    await this.completeSession(sessionId);
  }

  private async processBatch(
    sessionId: string,
    cards: Note[],
    userPrompt: string
  ): Promise<void> {
    const llmResponse = await this.llmService.analyzeBatch(cards, userPrompt);
    const suggestions = this.parseSuggestions(llmResponse);

    for (const suggestion of suggestions) {
      await this.saveSuggestion(sessionId, suggestion);
    }

    await this.updateProgress(sessionId, cards.length);
  }
}
```

### 2. LLM Service (Abstraction)

```typescript
/**
 * Provider-agnostic interface for LLM interactions
 * Implementations can use Claude Code, API calls, local models, etc.
 */
interface LLMService {
  analyzeBatch(
    cards: Note[],
    userPrompt: string,
    options?: LLMOptions
  ): Promise<LLMResponse>;

  // Health check - verify provider is available and configured
  checkHealth(): Promise<LLMHealthStatus>;
}

interface LLMHealthStatus {
  available: boolean;
  error?: string;
  info?: string; // e.g., "Claude Code v1.2.3 installed"
}

/**
 * Claude Code implementation - spawns Claude Code subprocess
 * Current constraint: Only available option with Claude Code subscription
 */
class ClaudeCodeService implements LLMService {
  async analyzeBatch(cards: Note[], userPrompt: string): Promise<LLMResponse> {
    const prompt = this.buildPrompt(cards, userPrompt);

    // Spawn Claude Code subprocess
    const response = await this.spawnClaudeCode({
      prompt,
      systemPrompt: this.getSystemPrompt(),
      temperature: 0.3,
      maxTokens: 4000
    });

    return this.parseResponse(response);
  }

  async checkHealth(): Promise<LLMHealthStatus> {
    try {
      // Check if Claude Code CLI is installed
      const result = await exec('claude --version');
      return {
        available: true,
        info: `Claude Code ${result.stdout.trim()} installed`
      };
    } catch (error) {
      return {
        available: false,
        error: 'Claude Code not installed or not in PATH'
      };
    }
  }

  private buildPrompt(cards: Note[], userPrompt: string): string {
    // Clean, focused prompt
    // No file I/O instructions
    // No workflow management
    // Just: "here are cards, user wants X, return suggestions"
  }

  private async spawnClaudeCode(options: ClaudeCodeOptions): Promise<string> {
    // Implementation that spawns Claude Code process
    // Returns the LLM response as string
  }
}

/**
 * Future: Direct API implementation (when available)
 */
class ClaudeAPIService implements LLMService {
  constructor(private apiKey: string) {}

  async analyzeBatch(cards: Note[], userPrompt: string): Promise<LLMResponse> {
    const prompt = this.buildPrompt(cards, userPrompt);

    const response = await this.client.complete({
      model: 'claude-sonnet-4',
      system: this.getSystemPrompt(),
      messages: [{ role: 'user', content: prompt }],
      temperature: 0.3,
      max_tokens: 4000
    });

    return this.parseResponse(response);
  }

  async checkHealth(): Promise<LLMHealthStatus> {
    if (!this.apiKey) {
      return {
        available: false,
        error: 'API key not configured'
      };
    }

    try {
      // Test API connection with minimal request
      await this.client.testConnection();
      return {
        available: true,
        info: 'API connection successful'
      };
    } catch (error) {
      return {
        available: false,
        error: `API error: ${error.message}`
      };
    }
  }

  private buildPrompt(cards: Note[], userPrompt: string): string {
    // Same clean prompt structure
  }
}

/**
 * LLM Service Factory - creates appropriate service based on config
 */
class LLMServiceFactory {
  static create(config: LLMConfig): LLMService {
    switch (config.provider) {
      case 'claude-code':
        return new ClaudeCodeService();
      case 'claude-api':
        return new ClaudeAPIService(config.apiKey!);
      case 'openai':
        return new OpenAIService(config.apiKey!);
      default:
        throw new Error(`Unknown LLM provider: ${config.provider}`);
    }
  }
}
```

### 3. Suggestion Writer & WebSocket Service

```typescript
class SuggestionWriter {
  constructor(
    private cache: CacheService,
    private wsService: WebSocketService
  ) {}

  async writeSuggestion(
    sessionId: string,
    llmSuggestion: LLMSuggestion
  ): Promise<void> {
    // 1. Fetch original note from cache
    const original = await this.cache.getNoteById(llmSuggestion.noteId);

    // 2. Build complete suggestion
    const suggestion: CardSuggestion = {
      noteId: llmSuggestion.noteId,
      original, // Added by backend
      changes: llmSuggestion.changes,
      reasoning: llmSuggestion.reasoning,
      accepted: null
    };

    // 3. Write file to disk
    const path = this.getSuggestionPath(sessionId, llmSuggestion.noteId);
    await fs.writeFile(path, JSON.stringify(suggestion, null, 2));

    // 4. Immediately emit WebSocket event (no file watcher needed)
    this.wsService.emitSuggestion(sessionId, suggestion);
  }
}
```

---

## LLM Provider Configuration (UI)

### Settings UI Requirements

**Location**: Settings page or modal

**Configuration per provider:**

```typescript
interface LLMConfig {
  provider: 'claude-code' | 'claude-api' | 'openai' | 'local-model';
  apiKey?: string;        // For API-based providers
  endpoint?: string;      // For custom/local models
  model?: string;         // Model selection (e.g., 'sonnet', 'opus', 'gpt-4')
  maxTokens?: number;     // Optional override
  temperature?: number;   // Optional override
}
```

### UI Components

#### 1. Provider Selection
```
┌─────────────────────────────────────────┐
│ LLM Provider                            │
│                                         │
│ ◉ Claude Code (Local)                  │
│   ○ Claude API                          │
│   ○ OpenAI API                          │
│   ○ Custom Endpoint                     │
└─────────────────────────────────────────┘
```

#### 2. Provider-Specific Configuration

**Claude Code:**
```
┌─────────────────────────────────────────┐
│ Claude Code Configuration               │
│                                         │
│ Status: ✓ Installed (v1.2.3)          │
│                                         │
│ [Test Connection]                       │
│                                         │
│ ℹ️ No additional configuration needed  │
└─────────────────────────────────────────┘
```

**Claude API:**
```
┌─────────────────────────────────────────┐
│ Claude API Configuration                │
│                                         │
│ API Key:                                │
│ [sk-ant-api03-************************] │
│                                         │
│ Model:                                  │
│ [Sonnet 4.5 ▼]                         │
│   - Sonnet 4.5 (Recommended)           │
│   - Opus 3.5                            │
│   - Haiku 3.5                           │
│                                         │
│ Status: ⚠️ Not tested                  │
│ [Test Connection]                       │
└─────────────────────────────────────────┘
```

**OpenAI API:**
```
┌─────────────────────────────────────────┐
│ OpenAI API Configuration                │
│                                         │
│ API Key:                                │
│ [sk-************************]          │
│                                         │
│ Model:                                  │
│ [GPT-4 Turbo ▼]                        │
│   - GPT-4 Turbo                         │
│   - GPT-4                               │
│   - GPT-3.5 Turbo                       │
│                                         │
│ Status: ✓ Connected                    │
│ [Test Connection]                       │
└─────────────────────────────────────────┘
```

#### 3. Advanced Settings (Collapsible)

```
┌─────────────────────────────────────────┐
│ ▼ Advanced Settings                     │
│                                         │
│ Temperature: [0.3]  (0.0 - 1.0)        │
│ Max Tokens:  [4000]                    │
│                                         │
│ ⚠️ Changing these may affect quality   │
└─────────────────────────────────────────┘
```

### Backend Storage

**Settings stored in**: `database/settings.json`

```json
{
  "llm": {
    "provider": "claude-code",
    "apiKey": null,
    "model": "sonnet",
    "temperature": 0.3,
    "maxTokens": 4000
  },
  "fieldDisplay": {
    // existing field display config
  }
}
```

**Security considerations:**
- API keys should be encrypted at rest
- Never log API keys
- Mask in UI (show only last 4 chars)

### Test Connection Flow

**When user clicks "Test Connection":**

1. Frontend calls `/api/llm/test` endpoint
2. Backend calls `llmService.checkHealth()`
3. Returns status to UI:
   - ✓ Success: "Connected successfully"
   - ⚠️ Warning: "Connected but slow response"
   - ✗ Error: "Failed: {error message}"

**UI feedback:**
```
Testing connection... [spinner]
✓ Connected successfully!
  Claude Code v1.2.3 detected
```

or

```
✗ Connection failed
  Error: Claude Code not installed or not in PATH

  [View Installation Guide]
```

### New Session Flow with Provider Check

**Before starting session:**
1. Check if LLM provider is configured
2. If not configured or health check fails:
   - Show warning modal
   - Offer to go to settings
   - Don't allow session start

```
┌─────────────────────────────────────────┐
│ ⚠️ LLM Provider Not Configured          │
│                                         │
│ Please configure an LLM provider before │
│ starting a session.                     │
│                                         │
│ [Go to Settings]  [Cancel]             │
└─────────────────────────────────────────┘
```

---

## Benefits of Industry Standard Approach

### 1. Separation of Concerns
- LLM: Intelligence (analysis, generation)
- Backend: Orchestration (batching, I/O, state)
- Frontend: Presentation (review UI)

### 2. Testability
```typescript
// Can test LLM service in isolation
test('LLM generates valid suggestions', async () => {
  const cards = [mockCard1, mockCard2];
  const response = await llmService.analyzeBatch(cards, "add examples");
  expect(response.suggestions).toHaveLength(2);
  expect(response.suggestions[0]).toMatchSchema(suggestionSchema);
});

// Can test orchestrator logic
test('Orchestrator batches cards correctly', () => {
  const cards = generateMockCards(250);
  const batches = orchestrator.createBatches(cards, 50);
  expect(batches).toHaveLength(5);
  expect(batches[0]).toHaveLength(50);
});
```

### 3. Optimization Independence
- Change batch size without changing prompts
- Add pre-filters without LLM knowledge
- Switch LLM providers without changing orchestration
- Optimize prompts without changing backend logic

### 4. Error Handling
```typescript
// Backend can retry, fall back, or skip
try {
  await processBatch(batch);
} catch (error) {
  if (error.type === 'rate_limit') {
    await sleep(60000);
    await processBatch(batch); // Retry
  } else if (error.type === 'invalid_response') {
    await logError(error);
    // Skip this batch, continue with next
  }
}
```

### 5. Cost Optimization
- Token-based dynamic batching maximizes efficiency
- Efficient batch processing reduces redundant context
- Retry logic minimizes wasted failed calls
- LLM service abstraction enables future optimizations (API vs Claude Code)

---

## Migration Path

### Phase 1: Backend Orchestration (No Breaking Changes)
- Keep current suggestion file format
- Add SessionOrchestrator service
- Migrate to backend-managed batching
- LLM still gets full prompt, but backend controls flow

### Phase 2: LLM Service Abstraction
- Extract LLM calls into dedicated service
- Optimize prompts for efficiency
- Add response validation
- Implement error handling

### Phase 3: Cost Optimizations
- Implement concise reasoning instructions
- Trim empty/irrelevant fields from input
- Fine-tune batch sizes based on real usage data
- Test output quality vs token reduction trade-offs

### Phase 4: Monitoring & Analytics
- Track acceptance rates
- Measure cost per suggestion
- Identify optimization opportunities
- A/B test different approaches

---

## Cost Optimization: What Actually Matters

**Token cost breakdown for 8,133 cards:**
- Input tokens: ~377k × $3/1M = **$1.13 (27%)**
- Output tokens: ~195k × $15/1M = **$2.93 (73%)**
- **Total: ~$4.06**

### Optimization Priority (High to Low):

#### 1. **Output Token Reduction** (73% of cost)
**Current**: ~30 tokens per suggestion
```json
{
  "noteId": 1300624515957,
  "changes": { "Exemple": "亜熱帯地域で多く見られる (commonly seen in subtropical regions)" },
  "reasoning": "Added contextual example phrase demonstrating natural usage of the kanji in a compound word"
}
```

**Optimized**: ~15 tokens per suggestion
```json
{
  "noteId": 1300624515957,
  "changes": { "Exemple": "亜熱帯地域で多く見られる" },
  "reasoning": "Natural usage in compound"
}
```

**Savings**: 50% output reduction = **$1.47 saved** → New cost: **$2.59**

#### 2. **Input Data Trimming** (27% of cost)
- Remove empty fields from card data sent to LLM
- Only send fields relevant to the task
- Example: Don't send empty "Note" field

**Savings**: ~10-15% input reduction = **$0.11-0.17 saved**

#### 3. **Batch Size Optimization** (affects both)
- Token-based batching to maximize cards per call
- Reduce per-batch overhead (context repetition)

**Savings**: ~5-10% total = **$0.20-0.40 saved**

### What About Prompt Caching?

**Reality check**: Cacheable content = ~250 tokens (system prompt + context)
- Cache savings: 250 × 203 batches × ($3 - $0.30)/1M = **$0.13 (3% of total)**
- **Not worth the complexity for Workflow 1**

**However**: Prompt caching becomes valuable for **Workflow 2.5** (Cross-Reference):
- Reference documents could be 50k-200k tokens
- Reused across all batches
- Cache savings: 100k × 200 batches × $2.70/1M = **$54.00** (massive impact!)

**Conclusion**: Skip caching for Workflow 1, revisit for Workflow 2.5 with reference documents.

---

## Open Questions

1. **Batch size optimization:** ✅ **RESOLVED** - Token-based dynamic batching (tokenize cards, batch by token count)
2. **Error recovery:** ✅ **RESOLVED** - Retry 1-2 times on parse errors, emit suggestions in real-time
3. **Progress granularity:** Update every batch? Every N batches? Time-based?
4. **Model selection:** ❌ **DEFERRED** - Not prioritizing for now (use Sonnet consistently)
5. **Prompt caching:** ✅ **RESOLVED** - Skip for Workflow 1 (only 3% savings), revisit for Workflow 2.5 with reference documents
6. **Pre-filtering:** ❌ **DEFERRED** - No rule-based filtering, send whole deck in batches (LLM can analyze user prompt if needed later)
7. **Response validation:** How strict? Auto-correct vs reject? (Currently: validate schema, retry on parse errors)
8. **Partial completion:** ✅ **CONFIRMED** - Already supported (user reviews while processing continues)
9. **Tokenizer choice:** Which tokenizer to use for batch size calculation? Claude tokenizer? Generic estimator?
10. **Output optimization:** How to instruct LLM to keep reasoning concise without losing quality?

---

## Next Steps

1. **Design SessionOrchestrator service** - Core logic and interfaces
2. **Design LLM service abstraction** - Provider-agnostic interface
3. **Optimize prompts** - Remove orchestration logic, focus on analysis
4. **Define error handling strategy** - Retry, fallback, skip logic
5. **Plan migration** - How to transition from current to new architecture
