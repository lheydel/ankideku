# Implementation Plan: Industry-Standard LLM Processing Pipeline

## Prerequisites

**IMPORTANT**: Before implementing any part of this plan, you MUST read and understand:
1. `conception/04_END_TO_END_WORKFLOW.md` - Full technical specification
2. `conception/CONCEPTION_SUMMARY.md` - Core principles and decisions

These documents contain critical architectural decisions, non-negotiables, and context that inform every implementation choice. Skipping them will lead to misaligned implementations.

---

## Overview

Migrate from the current naive approach (Claude Code agents writing files directly) to an industry-standard architecture with proper separation of concerns.

**Current state**: LLM manages file I/O, batching, workflow execution
**Target state**: Backend orchestrates everything, LLM only does intelligent analysis

---

## Phase 1: LLM Service Abstraction

### Goal
Create a provider-agnostic interface for LLM interactions.

### Tasks

#### 1.1 Define LLM Service Interface
**File**: `backend/src/services/llm/LLMService.ts`

```typescript
interface LLMService {
  analyzeBatch(cards: Note[], userPrompt: string): Promise<LLMResponse>;
  checkHealth(): Promise<LLMHealthStatus>;
}

interface LLMResponse {
  suggestions: LLMSuggestion[];
  usage?: { inputTokens: number; outputTokens: number };
}

interface LLMSuggestion {
  noteId: number;
  changes: Record<string, string>;
  reasoning: string;
}

interface LLMHealthStatus {
  available: boolean;
  error?: string;
  info?: string;
}
```

#### 1.2 Implement ClaudeCodeService
**File**: `backend/src/services/llm/ClaudeCodeService.ts`

- Spawns Claude Code subprocess with clean prompt
- Builds focused prompts (no file I/O instructions, no workflow logic)
- Parses JSON response from Claude Code output
- Implements health check via `claude --version`

#### 1.3 Create LLM Service Factory
**File**: `backend/src/services/llm/LLMServiceFactory.ts`

- Factory pattern for creating LLM service based on config
- Currently only `claude-code` provider
- Extensible for future providers (claude-api, openai, etc.)

#### 1.4 Add LLM Config to Settings
**File**: `backend/src/services/settings.ts` (modify)

Add to settings schema:
```typescript
llm: {
  provider: 'claude-code'
}
```

### Deliverables
- [ ] `LLMService.ts` - Interface definitions
- [ ] `ClaudeCodeService.ts` - Claude Code implementation
- [ ] `LLMServiceFactory.ts` - Factory class
- [ ] Settings schema updated

---

## Phase 2: Prompt Engineering

### Goal
Create clean, efficient prompts that instruct LLM to only do intelligent work.

### Tasks

#### 2.1 Design System Prompt
**File**: `backend/src/services/llm/prompts/systemPrompt.ts`

```typescript
const SYSTEM_PROMPT = `
You are an expert at analyzing Anki flashcards and suggesting improvements.

Your task: Analyze cards and suggest field modifications based on user instructions.

Output format: JSON object with suggestions array.
Schema:
{
  "suggestions": [
    {
      "noteId": number,
      "changes": { "FieldName": "new value" },
      "reasoning": "brief explanation (1 sentence max)"
    }
  ]
}

Rules:
- Only suggest changes for cards that need improvement
- Skip cards that are already good
- Return empty array if no cards need changes: { "suggestions": [] }
- Keep reasoning concise (saves cost)
- Return ONLY valid JSON, no other text
- CRITICAL: Your response will be parsed programmatically. Invalid JSON will cause errors. Ensure proper formatting.
`;
```

#### 2.2 Design Batch Prompt Builder
**File**: `backend/src/services/llm/prompts/batchPrompt.ts`

- Takes cards array, user prompt, and note type model
- Includes full field schema from note type (all available fields with names)
- Formats card data efficiently (omit empty fields to save tokens)
- LLM knows all possible fields even if empty in current cards (e.g., can suggest filling empty "Example" field)

Example prompt structure:
```
Note type: "Japanese Vocabulary"
Available fields: ["Word", "Reading", "Meaning", "Example", "Notes"]

User request: "Add example phrases"

Cards to analyze:
Card 1 (ID: 123):
  Word: 食べる
  Reading: たべる
  Meaning: to eat

Card 2 (ID: 456):
  Word: 飲む
  Reading: のむ
  Meaning: to drink
  Example: 水を飲む
```

#### 2.3 Optimize Token Usage
- Remove empty fields from card data
- Only include fields relevant to task (if detectable)
- Keep reasoning instruction for conciseness

### Deliverables
- [ ] `systemPrompt.ts` - System prompt constant
- [ ] `batchPrompt.ts` - Batch prompt builder
- [ ] Token estimation utility (optional)

---

## Phase 3: Session Orchestrator

### Goal
Backend service that manages the entire session lifecycle.

### Tasks

#### 3.1 Create Session Orchestrator Service
**File**: `backend/src/services/SessionOrchestrator.ts`

```typescript
class SessionOrchestrator {
  constructor(
    private llmService: LLMService,
    private cacheService: CacheService,
    private suggestionWriter: SuggestionWriter,
    private wsService: WebSocketService
  ) {}

  async executeSession(sessionId: string): Promise<void>;
  private async loadDeckCards(deckName: string): Promise<Note[]>;
  private createBatches(cards: Note[], tokenLimit: number): Note[][];
  private async processBatch(sessionId: string, batch: Note[], userPrompt: string): Promise<number>;
  private async handleBatchError(sessionId: string, batch: Note[], error: Error): Promise<void>;
  private async updateProgress(sessionId: string, processedCount: number): Promise<void>;
  private async completeSession(sessionId: string): Promise<void>;
}
```

#### 3.2 Implement Token-Based Batching
- Tokenize cards to estimate token count
- Create batches that fit within token limits
- Consider: ~4000 tokens output limit, ~8000 tokens input reasonable

#### 3.3 Implement Retry Logic
- Retry 1-2 times on parse errors
- Log errors for debugging
- Skip batch after max retries, continue with next

#### 3.4 Integrate with Existing Session Management
- Hook into existing session creation flow
- Replace current agent-spawning logic

### Deliverables
- [ ] `SessionOrchestrator.ts` - Main orchestrator class
- [ ] Token counting utility
- [ ] Batch creation logic
- [ ] Retry/error handling
- [ ] Integration with existing routes

---

## Phase 4: Suggestion Writer & WebSocket

### Goal
Direct suggestion writing and WebSocket emission (remove file watcher dependency).

### Tasks

#### 4.1 Create SuggestionWriter Service
**File**: `backend/src/services/SuggestionWriter.ts`

```typescript
class SuggestionWriter {
  constructor(
    private cacheService: CacheService,
    private wsService: WebSocketService
  ) {}

  async writeSuggestion(sessionId: string, llmSuggestion: LLMSuggestion): Promise<void> {
    // 1. Fetch original note from cache
    // 2. Build complete CardSuggestion object
    // 3. Write to file
    // 4. Emit WebSocket event immediately
  }
}
```

#### 4.2 Modify WebSocket Service
- Add method to emit suggestion directly
- Add method to emit progress updates
- Remove file watcher dependency (if exists)

#### 4.3 Update Frontend WebSocket Handlers
- Ensure handlers work with direct emission
- Test real-time suggestion display

### Deliverables
- [ ] `SuggestionWriter.ts` - Suggestion writing service
- [ ] WebSocket service updates
- [ ] Frontend handler verification

---

## Phase 5: Response Parsing & Validation

### Goal
Robust parsing and validation of LLM responses.

### Tasks

#### 5.1 Create Response Parser
**File**: `backend/src/services/llm/ResponseParser.ts`

```typescript
class ResponseParser {
  parse(rawResponse: string): LLMResponse;
  private extractJSON(text: string): object;
  private validateSchema(data: object): LLMResponse;
}
```

#### 5.2 Implement Schema Validation
- Validate required fields (noteId, changes, reasoning)
- Validate noteId exists in batch
- Filter out malformed suggestions

#### 5.3 Handle Edge Cases
- Empty response
- Partial JSON
- Missing suggestions array
- Invalid noteIds

### Deliverables
- [ ] `ResponseParser.ts` - Response parsing class
- [ ] Schema validation logic
- [ ] Error handling for edge cases

---

## Phase 6: Integration & Migration

### Goal
Replace current implementation with new architecture.

### Tasks

#### 6.1 Update Session Routes
**File**: `backend/src/routes/sessions.ts` (modify)

- Replace agent-spawning logic with SessionOrchestrator

#### 6.2 Add Health Check Endpoint
**File**: `backend/src/routes/llm.ts` (new)

```typescript
GET /api/llm/health - Check LLM provider status
GET /api/llm/config - Get current LLM config
PUT /api/llm/config - Update LLM config
```

#### 6.3 Remove Deprecated Code
- Remove old agent-spawning code
- Remove file watcher (if no longer needed)
- Clean up unused utilities

#### 6.4 Frontend Adaptation
- Update frontend to work with new backend flow if necessary
- Ensure WebSocket handlers match new emission patterns
- Update API calls if endpoints changed
- Test full flow manually: start session → process → review

### Deliverables
- [ ] Updated session routes
- [ ] LLM health/config endpoints
- [ ] Deprecated code removed
- [ ] Frontend adapted to new flow

---

## Phase 7: Progress UI

### Goal
Display real-time processing progress to users during AI sessions.

### Tasks

#### 7.1 Add Progress Event Handler to Frontend
**File**: `frontend/src/hooks/useWebSocket.ts` (modify)

- Add handler for `session:progress` event
- Pass progress data to callback

#### 7.2 Create Progress Display Component
**File**: `frontend/src/components/ui/ProgressIndicator.tsx` (new)

```typescript
interface ProgressIndicatorProps {
  processed: number;
  total: number;
  suggestionsCount: number;
  percentage: number;
}
```

- Show cards processed vs total
- Show percentage progress bar
- Show suggestions generated count
- Animate smoothly between updates

#### 7.3 Integrate Progress into Session View
**File**: `frontend/src/components/SessionView.tsx` or similar (modify)

- Display progress indicator while session is running
- Hide when session completes or fails
- Show "Processing X of Y cards (Z suggestions)" format

#### 7.4 Update Store for Progress State
**File**: `frontend/src/store/useStore.ts` (modify)

- Add progress state: `{ processed: number, total: number, suggestionsCount: number }`
- Add action to update progress from WebSocket

### Deliverables
- [ ] WebSocket handler for `session:progress`
- [ ] `ProgressIndicator.tsx` component
- [ ] Integration in session view
- [ ] Store updates for progress state

---

## Phase 8: Settings UI (Optional)

### Goal
Allow users to configure LLM provider in UI.

### Tasks

#### 8.1 Add LLM Settings Section
**File**: `frontend/src/components/Settings.tsx` (modify)

- Provider selection (currently just Claude Code)
- Connection test button
- Status display

#### 8.2 Implement Health Check UI
- Call `/api/llm/health` on settings load
- Show status indicator
- "Test Connection" button

### Deliverables
- [ ] LLM settings UI component
- [ ] Health check integration
- [ ] Connection test functionality

---

## File Structure (New/Modified)

```
backend/src/
├── services/
│   ├── llm/
│   │   ├── LLMService.ts           # Interface definitions
│   │   ├── ClaudeCodeService.ts    # Claude Code implementation
│   │   ├── LLMServiceFactory.ts    # Factory class
│   │   ├── ResponseParser.ts       # Response parsing
│   │   └── prompts/
│   │       ├── systemPrompt.ts     # System prompt
│   │       └── batchPrompt.ts      # Batch prompt builder
│   ├── SessionOrchestrator.ts      # Main orchestrator
│   ├── SuggestionWriter.ts         # Suggestion writing
│   └── settings.ts                 # Modified for LLM config
├── routes/
│   ├── sessions.ts                 # Modified
│   └── llm.ts                      # New: LLM health/config
└── utils/
    └── tokenizer.ts                # Token counting utility
```

---

## Implementation Order

Recommended order for minimal disruption:

1. **Phase 1** (LLM Service) - Foundation, no breaking changes
2. **Phase 2** (Prompts) - Standalone, can test independently
3. **Phase 5** (Response Parser) - Needed by orchestrator
4. **Phase 4** (Suggestion Writer) - Needed by orchestrator
5. **Phase 3** (Session Orchestrator) - Core logic, uses above
6. **Phase 6** (Integration) - Switch over
7. **Phase 7** (Progress UI) - Frontend progress display
8. **Phase 8** (Settings UI) - Polish, optional

---

## Success Criteria

- [ ] Sessions process without LLM file I/O
- [ ] Suggestions appear in real-time via WebSocket
- [ ] Retry logic handles parse errors gracefully
- [ ] Cost per suggestion reduced (measure before/after)
- [ ] Frontend works unchanged
- [ ] Error recovery works (failed batches don't break session)
