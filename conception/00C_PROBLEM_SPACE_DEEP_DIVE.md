# Problem Space Deep Dive

**Focus:** Understanding what users REALLY need, not just what they ask for

---

## The Job To Be Done

When a user with 8,000 vocabulary cards says "add example phrases", what job are they hiring AnkiDeku to do?

### Surface Level (What they say):
"Add example phrases to my cards"

### Deeper Level (What they want):
- **Improve card memorability** - examples provide context for better retention
- **Reduce ambiguity** - distinguish between multiple meanings
- **Save time** - don't want to manually write 8,000 examples
- **Maintain quality** - examples should be accurate, natural, contextual
- **Batch operation** - do it once for the whole deck, not card by card

### Deepest Level (The actual goal):
**"I want my deck to be higher quality so I learn better, but I don't have time to improve 8,000 cards manually"**

---

## The Cost/Value/Quality Triangle

Every bulk processing task has three competing factors:

```
           Quality
              /\
             /  \
            /    \
           /      \
          /        \
         /          \
        /   SWEET   \
       /     SPOT    \
      /               \
     /                 \
    /___________________\
  Cost                Time
```

Users want:
- **High quality** - accurate, natural, helpful changes
- **Low cost** - financially sustainable
- **Fast** - don't wait days for 8k cards

**You can optimize for 2, but not all 3:**
- High quality + Low cost = Slow (human review of everything)
- High quality + Fast = Expensive (best LLM on every card)
- Low cost + Fast = Low quality (cheap/fast but error-prone)

**Key Question:** Where on this triangle are users willing to operate?

---

## Breaking Down User Requests

Let's decompose a real request:

> "Add small phrase examples on each card. If a card already has an example in Note field, use that instead (and remove the note). If a word has multiple meanings, you may add up to 3 phrases (beware that the resulting card will be displayed on mobile screen) separated by \n and starting with a hyphen to mark elements in the list for lisibility. You can skip very simple and frequent words (like numbers, person, eat, etc) that a beginner learns quickly. Skip numeral counters too (mai, nin, kan, ...) Only put the japanese version of the phrase, no translation needed."

### Component 1: Core Task (LLM-required)
- "Add small phrase examples" → **Generative linguistic task**
- "If word has multiple meanings, add up to 3 phrases" → **Disambiguation**

### Component 2: Conditional Logic (Rule-based)
- "If card already has example in Note field, use that instead" → **Deterministic**
- "Remove the note" → **Field manipulation**

### Component 3: Filtering (Rule-based with data)
- "Skip very simple and frequent words (numbers, person, eat, etc.)" → **Frequency list lookup**
- "Skip numeral counters (mai, nin, kan...)" → **Pattern matching**

### Component 4: Formatting (Rule-based)
- "Separated by \n and starting with a hyphen" → **String formatting**
- "Beware mobile screen" → **Constraint (length limit)**
- "Only Japanese, no translation" → **Output filter**

### Analysis:
- **~20% needs LLM** (actual phrase generation, disambiguation)
- **~80% is programmable** (conditional logic, filtering, formatting)

**Insight:** Most user requests are hybrid - they describe workflows that combine LLM capabilities with deterministic operations.

---

## The Workflow Abstraction

Users describe workflows in natural language, but those workflows have structure:

```
INPUT: Deck of notes
  ↓
FILTER: Which notes need processing?
  ↓
TRANSFORM: What changes to make?
  ↓
FORMAT: How to structure output?
  ↓
OUTPUT: Suggestions for user review
```

### Example 1: "Add example phrases"

```
INPUT: 8,000 vocabulary cards

FILTER:
  - Exclude if Example field already has content
  - Exclude if word is in common words list
  - Exclude if word matches counter pattern
  ↓ Result: ~2,000 cards

TRANSFORM (LLM):
  - Generate 1-3 contextual Japanese phrases
  - Consider multiple meanings
  - Keep concise for mobile
  ↓ Result: Generated examples

FORMAT:
  - Separate with \n
  - Prefix with "-"
  - Japanese only (no translations)
  ↓ Result: Formatted suggestions

OUTPUT: 2,000 suggestions for review
```

### Example 2: "Refine French translations"

```
INPUT: 8,000 vocabulary cards

FILTER:
  - All cards (no exclusions mentioned)
  ↓ Result: 8,000 cards

TRANSFORM (LLM):
  - Evaluate translation precision
  - Suggest refinements if needed
  - Skip if already accurate
  ↓ Result: ~1,500 improvements (18% hit rate)

FORMAT:
  - Updated French gloss
  - Remove annotations like [一段]
  ↓ Result: Clean glosses

OUTPUT: 1,500 suggestions for review
```

**Observation:** Users specify the WHAT (add examples), not HOW (batch in 100s, use Haiku, cache prompts). The system should figure out HOW.

---

## The Hidden Costs

What's actually expensive in the current approach?

### Token Breakdown for 8,000 Cards (estimated):

**Per-card prompt:**
- System prompt: ~800 tokens (deck context, instructions, format)
- Card data: ~200 tokens (note fields, metadata)
- Response: ~100 tokens (suggestion)
- **Total per card:** ~1,100 tokens

**For 8,000 cards:**
- Input: 8,000,000 tokens (8,000 × 1,000)
- Output: 800,000 tokens (8,000 × 100)
- **Total: ~8.8M tokens**

**At Sonnet pricing (~$3/$15 per M tokens):**
- Input: $24
- Output: $12
- **Total: ~$36 per deck**

**But with inefficiencies:**
- Re-reading deck files: +$10
- Parallel agents duplicating context: +$15
- Failed/retried cards: +$8
- **Actual cost: ~$60-70 per deck**

### Where the waste is:
1. **System prompt duplication** (800 tokens × 8,000 = 6.4M tokens)
2. **Deck context duplication** (repeated deck info)
3. **Filtering done by LLM** (LLM sees all cards, decides to skip many)
4. **Format verbosity** (full original note in every suggestion)

---

## Cost Reduction Opportunities (Conceptual)

### 1. Prompt Caching (90% reduction)
If we batch cards and cache the system prompt:
- First batch: Full cost (~$10 for 100 cards)
- Subsequent batches: 10% cost (~$1 for 100 cards)
- **80 batches total: $10 + $70 = $80... wait, that's MORE!**

**Correction:** Caching works when the SAME prompt prefix is reused:
- Batch 1: $10 (full cost, establishes cache)
- Batches 2-80: $0.50 each (only new cards cost tokens)
- **Total: $10 + $40 = $50... still not 90% savings**

**Reality check:** Prompt caching saves on the SYSTEM prompt, not the card data. If system prompt is 800 tokens and card data is 200 tokens per card:
- Without caching: (800 + 200×100) = 20,800 tokens per batch × 80 batches = 1.66M tokens
- With caching: (800 cached + 200×100) = 20,800 tokens first batch, then 20,000 tokens × 79 batches = 1.6M tokens

**Savings: ~5%, not 90%!**

**Where 90% savings claims come from:**
- If system prompt is HUGE (e.g., 10k tokens of examples)
- And card data is small (e.g., 100 tokens)
- Then caching the 10k saves a lot

**For our use case:** We don't have huge system prompts, so caching helps but isn't a silver bullet.

### 2. Pre-filtering (30-70% reduction)
Remove cards before LLM sees them:
- 8,000 cards → filter → 2,500 cards
- **Cost: $36 → $11** (69% savings)
- **Plus:** No LLM tokens wasted on "skip this card" decisions

### 3. Model Selection (95% reduction for simple tasks)
Haiku vs Sonnet pricing:
- Sonnet: $3/$15 per M tokens
- Haiku: $0.25/$1.25 per M tokens
- **12x cheaper for simple tasks**

If 60% of cards are simple (just add example):
- 4,800 cards with Haiku: $2
- 3,200 cards with Sonnet: $12
- **Total: $14** (vs $36 = 61% savings)

### 4. Smart Batching (10-20% reduction)
Batch similar cards together:
- Cards with same word type process together
- Reduces context switching
- LLM learns pattern within batch
- Better examples, fewer retries

**Combined effect:**
- Pre-filter: 69% reduction → $11
- Use Haiku where possible: 50% of remaining → $6
- Smart batching: 15% of remaining → $5
- **Total: $5 vs $36 = 86% savings**

---

## Quality Considerations

What makes a "good" suggestion?

### For Example Phrases:
✅ Natural Japanese grammar
✅ Contextually appropriate
✅ Uses the word correctly
✅ Distinguishes meanings if multiple
✅ Appropriate length for mobile
❌ Not overly simple
❌ Not too advanced for target level
❌ Not stilted/textbook-sounding

### Quality Spectrum:
- **Native-level (90-100%):** Perfect grammar, natural usage, contextual nuance
- **Proficient (70-90%):** Good grammar, appropriate usage, minor awkwardness
- **Adequate (50-70%):** Understandable, mostly correct, some errors
- **Poor (<50%):** Grammatical errors, unnatural, or wrong usage

**Question:** What's the minimum acceptable quality?
- If 70% quality costs $5, and 90% quality costs $36, which do users prefer?
- Can users detect quality differences?
- Does quality matter if users review each suggestion anyway?

---

## The Review Bottleneck

If system generates 8,000 suggestions:
- User must review each one
- ~10 seconds per review (read, compare, decide)
- **80,000 seconds = 22 hours of review**

**This is the real problem!** Not the cost, but the review time.

### Implications:
1. **Batch acceptance might be needed** - "Accept all that look like this"
2. **Confidence scoring** - Show high-confidence suggestions first
3. **Sampling** - Review 100, auto-accept similar ones
4. **Quality threshold** - Only show suggestions above quality bar

**Key insight:** If users can't practically review 8,000 suggestions, then generating 8,000 suggestions is wasted effort.

---

## Alternative Framing: What if it's not about processing ALL cards?

Current assumption: User wants to improve ALL 8,000 cards.

Alternative: User wants to **improve the deck**, which might mean:
- Fix the worst 20% of cards
- Enhance high-frequency cards
- Sample and improve until diminishing returns
- Improve until user is satisfied with quality

**Different mental model:**
- Not "process all 8,000"
- But "improve deck quality to acceptable level"

This changes the approach:
1. Analyze deck, find cards most in need
2. Process high-value cards first
3. Show user impact metrics
4. Stop when satisfied or budget exhausted

---

## Questions This Raises

1. **Do users want comprehensive coverage or targeted improvement?**
2. **Is review time the real bottleneck, not cost?**
3. **Should we optimize for suggestions generated or suggestions accepted?**
4. **Can we build trust for auto-accept, or is review always necessary?**
5. **What if "good enough" at $5 is better than "perfect" at $36?**

---

## Next: Understanding User Goals More Deeply

We need to know:
- Why are users improving their decks?
- What's the acceptable quality level?
- How much time/money are they willing to invest?
- What does success look like to them?
- Are there patterns we're missing?
