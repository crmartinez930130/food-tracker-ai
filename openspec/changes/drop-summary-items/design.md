## Context

- **`analyzeMeal`** sends the photo and/or the description with a prompt built by `analysisPrompt`, and parses the reply with `client.messages.parse` and `zodOutputFormat(MealAnalysisSchema)`. The schema only checks shapes: items with a name, grams, confidence and macros, plus totals and optional notes.
- **The item rules** (`ITEM_RULES`) are shared by both prompt modes. They ask for each item's grams and macros, and for low confidence instead of omitting an item. They don't say what an item may *not* be.
- **Tests** use a fake Anthropic client whose `messages.parse` returns a canned `parsed_output`.

## Goals / Non-Goals

**Goals:**
- No summary rows reach the app.
- The totals always equal the sum of the returned items.
- Real 0 kcal foods are never dropped.

**Non-Goals:**
- Fixing other kinds of bad items, such as duplicates or implausible values.
- Changing the response schema or the app.
- Cleaning saved data.

## Decisions

### D1. Tell the model, then check
- **Prompt:** `ITEM_RULES` gains a sentence: "List only foods and drinks in items; never add an item for a total, subtotal or the whole meal - the totals have their own fields."
- **Why the filter too:** the prompt alone reduces the problem but can't guarantee it, so the filter catches what slips through.

### D2. What counts as "not food"
- **Empty rows:** `estimatedGrams` and all four macros at 0 means not food, whatever the name. A real food always has a quantity, and 0 kcal foods (water, black coffee) keep their grams.
  - **Found while verifying:** once the prompt forbade "Total", the model sometimes returned a blank-named or repeated-name row with everything at 0 instead. Name-based matching alone missed those. The user approved widening the rule.
- **Summary rows with values:** `isSummaryName(name)` matches a closed list after normalizing the name, and the values match the sum of the other items.
  - **Normalization:** lowercase, accents stripped (NFD without combining marks), parenthesized text removed, every non-letter turned into a space, and spaces collapsed.
  - **The list:** total, totals, totales, subtotal, subtotals, subtotales, sum, suma, "meal total", "total meal", "total comida", "total de la comida".
  - **Exact matching** after normalization, so a food that merely starts with "total" (a cereal brand) isn't caught.
  - **The values:** kcal within `max(2 %, 5)` of the others' sum, and each macro within `max(2 %, 1 g)`.
- **A row named "Total" with its own distinct, non-zero values survives.** That's very unlikely, and safer than dropping real food.

### D3. Totals recomputed
- **`sanitizeAnalysis(analysis)`** returns `{ analysis, dropped }`. The returned analysis has the remaining items and totals recomputed as their sums. The model's own totals are discarded, even when nothing was dropped, because the app and the spec define the total as the sum of the items.
- **Rounding:** sums are rounded to one decimal, to avoid float noise like 226.99999.

### D4. Logging
- **When:** `analyzeMeal` logs `analysis_items_dropped` only when something was dropped.
- **What:** the dropped names and the model. The description isn't logged, since it is user text.

## Risks / Trade-offs

- **A real food named exactly like a summary word, with all-zero or sum-matching values.** → It would be dropped. That's extremely unlikely, and the user can add it by hand in Review.
- **Discarding the model's totals.** → They were already ignored by the app, which sums the items. This makes the API consistent with the app.

## Migration Plan

None. This is a backend-only change, and the response shape is unchanged.
