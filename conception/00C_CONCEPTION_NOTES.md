# AnkiDeku - Conception & Problem Space Analysis

**Date:** 2025-11-24
**Status:** Conception phase - understanding requirements, not designing solutions

---

## What We Know

### Core Use Case
AnkiDeku is a system that:
- Reads Anki notes from decks
- Applies some kind of processing/analysis (currently via LLM)
- Suggests changes to those notes
- Lets users review and accept/reject changes

### Current Reality
- **Workflow:** Field updates (add examples, refine translations)
- **Scale:** 8,000+ card decks
- **Cost Issue:** Current LLM approach is financially unsustainable
- **Quality Issue:** LLMs don't always follow instructions correctly

---

## Open Questions to Explore

### 1. **What is the core value proposition?**
- Is it "AI-powered card improvement"?
- Or "Bulk card editing with intelligent assistance"?
- Or "Card quality control and enhancement"?
- What problem are we REALLY solving for users?

### 2. **Who is the user?**
- Language learners managing large decks?
- Content creators maintaining card quality?
- Students optimizing their study materials?
- What are their pain points beyond "I need examples on my cards"?

### 3. **What makes tasks LLM-worthy vs rule-based?**

Current examples:
- **LLM-worthy:** "Add contextual Japanese example phrases" (needs linguistic creativity)
- **LLM-worthy:** "Refine French translation nuance" (needs cross-lingual expertise)
- **Rule-based?:** "Move Note field to Example if Example is empty" (deterministic)
- **Rule-based?:** "Remove [一段] annotations" (pattern matching)
- **Hybrid?:** "Skip common words" (frequency list + judgment call)

**Question:** What % of user requests are truly LLM-dependent vs. could be rules/scripts?

### 4. **What does "success" look like?**
- User creates 8k suggestions, accepts 90%? (high precision needed)
- User creates 8k suggestions, reviews 100, accepts 20%? (exploratory)
- User defines rules, system auto-applies to matching cards? (automation)
- How much human oversight is necessary vs. desirable?

### 5. **What are the actual workflow patterns?**

From the data, users seem to:
1. Select a deck
2. Describe what they want (natural language)
3. Wait for processing
4. Review suggestions
5. Accept/reject individually

**But is this the RIGHT flow?** Or just what's currently possible?

Alternative flows to consider:
- User shows examples, system infers pattern?
- User reviews sample, approves approach, then bulk applies?
- System suggests rules based on deck analysis?
- Interactive refinement (show 10 results, user tweaks prompt, repeat)?

### 6. **What's the relationship between cost and value?**

If processing 8k cards costs $X:
- What would user pay for this service?
- Is the value in "I get 8k improved cards" or "I save 100 hours of manual work"?
- Would users accept "90% of cards improved" for 10% of the cost?
- Is there a sweet spot between automation and manual work?

### 7. **What are the quality requirements?**

For linguistic tasks:
- Must corrections be native-level accurate?
- Is "good enough" acceptable if cheap?
- Can users easily spot bad suggestions?
- What's the cost of false positives (bad suggestions accepted)?

### 8. **What makes this different from batch editing in Anki?**

Anki already has:
- Find & Replace
- Browser-based editing
- Add-ons for bulk operations

**What does AnkiDeku offer that's unique?**
- Intelligent analysis (not just pattern matching)?
- Natural language instructions (not regex)?
- Context-aware changes (not blind replacement)?
- Something else?

### 9. **What's the mental model?**

How should users think about this tool?
- "An AI assistant that reviews my cards"?
- "A bulk editing tool with smart defaults"?
- "A card quality analyzer"?
- "A workflow automation system"?

### 10. **What are the natural boundaries/constraints?**

- Must work offline? (or cloud API is fine?)
- Must be free? (or paid tiers acceptable?)
- Must process all cards? (or sampling/progressive processing OK?)
- Must preserve original workflow? (or can we reimagine it?)

---

## Patterns Emerging from Analysis

### Pattern 1: LLM tasks are **generative** or **evaluative**
- **Generative:** Create example phrases, write mnemonics
- **Evaluative:** Is this translation precise? Does this card need work?

### Pattern 2: High-volume tasks need different approach than exploration
- 20 cards: Individual processing is fine, even if suboptimal
- 8,000 cards: Need efficiency, batching, smart filtering

### Pattern 3: Users give instructions in natural language
- Not "apply regex to field X"
- But "add examples to vocabulary cards"
- System must interpret intent

### Pattern 4: Review is always human-in-the-loop
- No auto-apply (yet)
- User must review each suggestion
- This is a quality gate but also a bottleneck

### Pattern 5: Suggestions are independent
- Each card is processed separately
- No cross-card dependencies (yet)
- Could this change? (e.g., "ensure examples use consistent style across deck")

---

## Fundamental Tensions

### Tension 1: Cost vs. Quality
- Better LLM = higher cost but better results
- Cheaper LLM = lower cost but more errors
- Where's the acceptable trade-off?

### Tension 2: Automation vs. Control
- Full automation = fast but risky
- Manual review = safe but slow
- How much user involvement is optimal?

### Tension 3: Flexibility vs. Simplicity
- Support all possible workflows = complex
- Focus on core use cases = limited
- What's the right scope?

### Tension 4: Real-time vs. Batch
- Real-time updates = engaging UX
- Batch processing = efficient
- Can we have both?

---

## What We Need to Understand Better

1. **User intentions beyond surface requests**
   - When someone says "add examples", what are they REALLY trying to achieve?
   - Better retention? More context? Easier recognition?

2. **The decision-making process**
   - How do users decide to accept/reject?
   - What signals do they look for?
   - Could we predict acceptance likelihood?

3. **The cost tolerance**
   - What would users actually pay?
   - What's the value of their time saved?
   - Is there a freemium model?

4. **The quality bar**
   - How good is "good enough"?
   - Native speaker level? Passable? Perfect?
   - Does it vary by use case?

5. **The workflow variations**
   - Are there common patterns we haven't seen yet?
   - What would users do if they could describe ANY workflow?
   - What's currently impossible but desirable?

---

## Next Steps in Conception

1. **Map the problem space more completely**
   - What are ALL the ways users want to improve cards?
   - What are the constraints (time, money, quality)?
   - What are the non-negotiables?

2. **Identify the core insight/innovation**
   - What makes this worth building?
   - What can we do that nothing else can?
   - What's the unfair advantage?

3. **Question assumptions**
   - Must processing be per-card? (or could it be per-pattern?)
   - Must it be LLM-based? (or is that just the current approach?)
   - Must users review each suggestion? (or could we build trust for auto-apply?)

4. **Define success metrics**
   - How do we know this is working?
   - User satisfaction? Cost per card? Time saved? Accuracy?

5. **Understand the economics**
   - What does it cost to provide value?
   - What can we charge?
   - Is this sustainable?

---

## Observations to Validate

- **Observation:** Most user requests contain both LLM-worthy and rule-based elements
- **Observation:** Users describe desired outcomes, not processes
- **Observation:** The current 1-card = 1-LLM-call model doesn't scale economically
- **Observation:** Real-time feedback is valuable for UX but may not be necessary for efficiency
- **Observation:** The suggestion file format contains redundant data

**Need to validate:** Are these observations correct? Do they hold for more users/use cases?
