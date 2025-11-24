# AnkiDeku LLM Requirements Analysis

**Analysis Date:** 2025-11-24
**Sessions Analyzed:** 20 sessions from Nov 23-24, 2025

## Summary of User Requests

Based on historical session data, users have requested the following types of linguistic tasks:

### 1. **Example Phrase Generation** (Primary Use Case)
- **Frequency:** 3+ sessions (most recent production use)
- **Task:** Generate contextual Japanese example phrases for vocabulary cards
- **Complexity:** HIGH - Requires:
  - Linguistic knowledge of Japanese grammar and usage
  - Contextual appropriateness
  - Multiple meanings disambiguation (up to 3 examples per word)
  - Frequency awareness (skip common/simple words)
  - Mobile display constraints (concise formatting)
- **Volume:** 8,133 cards in production deck

**Example Request:**
> "Add small phrase examples on each card. If a card already has an example in Note field, use that instead (and remove the note). If a word has multiple meanings, you may add up to 3 phrases (beware that the resulting card will be displayed on mobile screen) separated by \n and starting with a hyphen to mark elements in the list for lisibility. You can skip very simple and frequent words (like numbers, person, eat, etc) that a beginner learns quickly. Skip numeral counters too (mai, nin, kan, ...) Only put the japanese version of the phrase, no translation needed."

### 2. **Translation Refinement** (Secondary Use Case)
- **Frequency:** 2 sessions
- **Task:** Improve French translations of Japanese vocabulary
- **Complexity:** HIGH - Requires:
  - Nuance detection (broad → precise meanings)
  - Context disambiguation (e.g., "請求" = billing/claim, not generic request)
  - Cross-lingual expertise (Japanese → French)
  - Annotation cleanup (remove grammar markers like [一段])
  - Judgment calls (using English when French is inadequate)
- **Volume:** 8,132 cards

**Example Request:**
> "Review the deck's Japanese→French vocabulary cards and improve the French glosses wherever needed. Refine meanings when they are too broad, imprecise, misleading, or missing important nuance. Example: 請求 → before: « requête, demande, réclamation » after: « facturation, réclamation, demande (monétaire) » (More accurate: clarifies it's a monetary/billing claim)"

### 3. **Test Runs**
- **Frequency:** ~8 sessions
- **Task:** "Make 5 random suggestions" for testing
- **Volume:** 20 card test deck

---

## Core LLM Capabilities Required

### Must-Have Features:
1. **Linguistic Analysis**
   - Japanese language understanding (grammar, usage, context)
   - French language understanding (nuance, precision)
   - Cross-lingual translation quality assessment
   - Frequency/commonality detection

2. **Content Generation**
   - Natural Japanese example sentence creation
   - Contextually appropriate phrase generation
   - Multi-meaning disambiguation

3. **Smart Filtering**
   - Skip logic (simple words, counters, numbers)
   - Conditional logic (if Note field has X, do Y)
   - Accuracy assessment (only suggest if improvement needed)

4. **Field Manipulation**
   - Move content between fields (Note → Exemple)
   - Clean up annotations
   - Format output (newlines, hyphens for lists)

---

## Scale Characteristics

- **Production Deck Size:** ~8,000 cards
- **Test Deck Size:** ~20 cards
- **Processing Pattern:** Bulk operations on entire deck
- **Acceptance Rate:** Unknown (user reviews each suggestion)

---

## Cost/Efficiency Concerns

### Current Issues:
1. **High Cost:** Processing 8k cards individually with Claude Sonnet is expensive
2. **Slow Processing:** Individual file writes for each card
3. **Token Waste:** Repetitive prompts and context for each card
4. **Quality Variance:** LLM may hallucinate or ignore instructions

### Optimization Opportunities:

#### A. **Batch Processing** (Recommended)
- Send 50-100 cards per prompt instead of individual files
- Use prompt caching to reduce cost by ~90% on subsequent batches
- Single structured JSON response with all suggestions
- **Estimated Savings:** 10-20x cost reduction

#### B. **Model Tiering**
- Use **Haiku** for simple cases (example generation for common words)
- Use **Sonnet** for complex cases (nuance refinement, disambiguation)
- **Estimated Savings:** 15-20x for simple tasks

#### C. **Smart Pre-filtering**
- Filter cards programmatically BEFORE sending to LLM:
  - Skip cards that already have examples (regex check)
  - Skip cards matching frequency lists (numbers, counters, common words)
  - Skip cards matching exclusion patterns
- **Estimated Savings:** 30-70% reduction in cards needing LLM

#### D. **Progressive Processing**
- Process small batch first (100 cards)
- User reviews quality
- If good, continue; if bad, adjust prompt
- Prevents wasting budget on incorrect approach

---

## Recommended Architecture Changes

### Current Flow:
```
User Prompt → Claude spawns agents → Each agent processes cards →
Writes individual suggestion-{noteId}.json files → File watcher detects →
Frontend updates in real-time
```

### Proposed Flow (Batch):
```
User Prompt → Backend pre-filters cards →
Batches of 50-100 cards → Claude (with caching) →
Single JSON response with all suggestions →
Backend writes individual files → File watcher detects →
Frontend updates
```

### Benefits:
- 90% cost reduction via prompt caching
- Faster processing (fewer API calls)
- More consistent output format
- Easier quality control
- Still maintains real-time UI updates

---

## Feature Priorities

### P0 (Critical - Must Work):
1. Example phrase generation for Japanese vocabulary
2. Translation refinement (JP→FR nuance)
3. Conditional logic (field existence checks)
4. Smart skipping (frequency-based)

### P1 (Important):
1. Multi-meaning handling (up to 3 examples)
2. Formatting rules (mobile display constraints)
3. Annotation cleanup

### P2 (Nice to Have):
1. Cross-lingual fallback (use English if French inadequate)
2. Grammar marker removal

---

## Next Steps

1. **Validate with user:** Confirm these are the primary use cases
2. **Prototype batch processing:** Test with 100-card sample
3. **Implement pre-filtering:** Build smart filter for common words/existing examples
4. **Test model tiering:** Compare Haiku vs Sonnet quality/cost
5. **Measure cost savings:** Compare old vs new approach

---

## Questions for User

1. Are there other linguistic tasks you need beyond example generation and translation refinement?
2. What's your acceptable cost per 1000 cards processed?
3. Would you accept 95% accuracy if it meant 10x cost reduction?
4. Do you need real-time streaming updates, or could batch results work?
5. Are there categories of cards that ALWAYS/NEVER need LLM processing?
