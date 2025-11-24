# 01 - Core Requirements & Principles

**Based on:** User clarifications on 2025-11-24

---

## Fundamental Requirements (Non-Negotiable)

### 1. Core Value Proposition
**"Help users improve their decks with AI-powered, context-aware suggestions"**

Improvement can mean:
- Enhancing existing cards (add examples, refine translations, fix errors)
- Creating new cards from input (generate from text, documents, concepts)
- Quality control (detect issues, suggest fixes)
- Content enrichment (add context, mnemonics, visual aids)

### 2. Target Users
- **Language learners** managing large vocabulary decks
- **Content creators/teachers** maintaining educational materials
- **Students** optimizing study decks across any subject
- **Domain experts** reviewing/improving specialized content

**Common need:** Bulk improvements that require intelligence, not just pattern matching.

### 3. LLM-First Philosophy
- **Primary focus:** Tasks that REQUIRE LLM capabilities (linguistic analysis, generation, evaluation, context understanding)
- **Rule-based operations:** May be used internally by the system, but NOT the main value proposition
- **Tool calling:** LLM can invoke deterministic tools when it deems appropriate (e.g., "move field A to field B if empty")

**Key principle:** If it can be done with regex or simple rules, it's not worth building AnkiDeku for. The value is in intelligent, context-aware processing.

### 4. Safety-First Workflow
**CRITICAL:** User must feel safe connecting their deck without risk of corruption.

Requirements:
- Manual review of EACH AND EVERY suggestion (no bulk auto-accept)
- Clear before/after comparison for every change
- Ability to reject suggestions without consequence
- History feature to undo accepted changes
- No changes written to Anki until user explicitly accepts

**User quote:** "I want to feel safe about connecting my deck without any risk of corrupting it."

### 5. Natural Language Interface
Users describe what they want in plain language, not technical syntax:
- ✅ "Add example phrases to vocabulary cards"
- ✅ "Refine French translations that are too broad"
- ❌ "Apply regex /pattern/ to field X if Y matches Z"

The system interprets intent and figures out HOW to execute.

---

## Success Criteria

### Definition of Success (Task-Dependent)
Generally: **High precision is better**

- Suggestion quality must be worth user's time reviewing
- Better to show 100 excellent suggestions than 1000 mediocre ones
- Mis-validating a suggestion is acceptable (history allows undo)
- But showing bad suggestions wastes user time → erodes trust

**Quality bar:** "Would an expert in this domain accept this suggestion?"

### What "High Precision" Means
For different tasks:

**Example Generation:**
- Natural, grammatically correct
- Contextually appropriate
- Uses target word correctly
- Appropriate difficulty level

**Translation Refinement:**
- More precise than original
- Captures nuance correctly
- Natural in target language
- No introduced errors

**Content Creation:**
- Accurate information
- Appropriate formatting
- Consistent with deck style
- Pedagogically sound

---

## Workflow Pattern (Current & Future)

### Current Flow (To Preserve)
1. User selects deck
2. User describes desired improvement (natural language)
3. System processes deck with LLM
4. User reviews each suggestion individually
5. User accepts or rejects each one
6. Accepted changes written to Anki
7. History maintained for undo capability

### Workflow Flexibility
- **Core requirements:** Manual review, safety, natural language input
- **Negotiable details:** How processing happens internally (batching, filtering, model selection)
- **Intermediary steps:** User providing input during processing may be acceptable (e.g., clarifications, confirmations, choices)
- **Key insight:** User cares about INPUT (what they ask for) and OUTPUT (suggestions to review), but intermediate interactions are allowed if they add value

### Suggestion Granularity (Critical)
- **1 suggestion = 1 card** - This is a fundamental principle
- Each suggestion represents changes to a single note
- User validates suggestions card-by-card (no bulk operations)
- **Implementation detail:** 1 suggestion = 1 file in database
  - This is an internal storage format
  - LLM doesn't need to know about files
  - LLM can return batch responses (e.g., JSON array of suggestions)
  - Backend writes individual files automatically
  - Database directory is a database - LLM doesn't manage it

**Future:** UI/UX may link related suggestions for cross-reference tasks, but validation remains per-card

### What Can Change
- Internal batching strategies
- Pre-filtering logic
- Model selection (Haiku vs Sonnet)
- Parallel processing
- Caching strategies
- Suggestion file formats

### What Cannot Change
- Manual review requirement
- Natural language input
- Safety guarantees
- Individual accept/reject
- Before/after clarity

---

## Mental Model

**"An AI assistant that helps me improve my deck with context-aware capabilities"**

This means:
- **Assistant, not automation:** Suggests, doesn't auto-apply
- **Context-aware:** Understands the content, not just patterns
- **Deck improvement:** Focus on quality, not just bulk operations
- **Intelligent:** Uses LLM capabilities, not simple rules

**Not:**
- A bulk find-and-replace tool
- An automation system that works unsupervised
- A rule-based card editor
- A regex engine with UI

---

## Scope Boundaries

### In Scope
- LLM-powered content analysis
- Intelligent suggestion generation
- Context-aware improvements
- Natural language task description
- Complex linguistic tasks
- Cross-card pattern detection
- Quality assessment
- Content generation

### Out of Scope (Primary)
- Pure rule-based operations (unless as LLM tools)
- Regex find-and-replace
- Bulk auto-apply
- Offline-only operation
- Real-time processing requirements
- Unsupervised automation

### Hybrid Zone (LLM Can Use Tools)
- Field manipulation (move, copy, delete)
- Pattern matching (when LLM deems useful)
- Formatting operations
- Data extraction from structured fields

**Principle:** If LLM decides "I can solve this with a simple rule", it can invoke that tool. But the primary value is LLM intelligence.

---

## Quality vs Cost Trade-offs (TBD)

Questions to explore later:
- What quality level is acceptable at what cost?
- Would users pay for higher quality?
- Is there a freemium model?
- What's the cost ceiling per deck?
- How do we balance cost with suggestion quality?

**Deferred:** Will address when we understand technical options better.

---

## Key Insights from User Clarifications

1. **Safety is paramount** - No risk of deck corruption, ever
2. **Review is non-negotiable** - Manual oversight required for every suggestion
3. **LLM is the core value** - Not trying to be a better bulk editor, but a smarter one
4. **Quality over quantity** - Better to show fewer, better suggestions
5. **Natural language is key** - Users shouldn't need to learn query syntax
6. **Flexibility in implementation** - How we achieve goals internally can evolve
7. **History as safety net** - Mistakes can be undone, so review errors are tolerable

---

## Design Principles Derived

1. **Preserve user control at all times**
2. **Make review process efficient and informed**
3. **Optimize for suggestion quality, not just quantity**
4. **Keep natural language input simple and powerful**
5. **Fail safe - never corrupt the deck**
6. **Make intelligence visible - user should see WHY suggestions are made**
7. **Support undo at every level**

---

## Open Questions (Updated)

~~1. What is core value? → ANSWERED~~
~~2. Who is the user? → ANSWERED~~
~~3. LLM vs rule-based? → ANSWERED~~
~~4. Success definition? → ANSWERED~~
~~5. Workflow patterns? → ANSWERED~~
~~8. What makes this different? → ANSWERED~~
~~9. Mental model? → ANSWERED~~

**Still Open:**
6. **Quality vs Cost balance** - What are users willing to pay? What quality is acceptable?
7. **Quality requirements specifics** - How do we measure "worth reviewing"?
10. **Natural boundaries** - Offline? Free tier? Processing limits?

**New Questions:**
- How do we make reviewing 8,000 suggestions practical?
- How do we measure/communicate suggestion quality?
- How do we handle partial deck processing (start, stop, resume)?
- What's the unit of work? (per-card, per-batch, per-pattern?)
