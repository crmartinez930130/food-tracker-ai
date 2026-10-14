## Why

While verifying #45, a description-only analysis ("a banana and a glass of milk") came back with the two right foods plus a third item named **"Total"**, with zero grams, calories and macros, and low confidence. The total in Review didn't change, but the fake food shows in the list. If the user doesn't remove it, it's saved with the meal and then shows on Home, History and in the CSV. If the model ever returned such a row with real values instead of zeros, the meal's calories would be doubled. Tracked as GitHub issue #56: https://github.com/crmartinez930130/food-tracker-ai/issues/56

## What Changes

- **Prompt:** in both modes (photo and description), the item rules say explicitly that `items` lists only real foods and drinks, never a summary row such as "Total" or "Subtotal", because the totals have their own fields.
- **Defensive filter** after parsing the model's response. It drops the rows that aren't food:
  - **Empty rows:** 0 g with every value at 0, whatever the name. This covers the all-zero "Total", and also the blank or repeated-name placeholders the model started returning once the prompt forbade "Total" (found while verifying this change).
  - **Summary rows with values:** a name that is a summary word (total, totales, subtotal, sum, suma, "meal total", ignoring case, accents, punctuation and anything in parentheses) whose calories and macros match the sum of the other items within a small tolerance.

  A real food with 0 kcal, such as 250 ml of water, keeps its grams, so it's never empty and is kept.
- **Totals:** the response's totals are always recomputed from the items that remain, so they match the list.
- **Log:** a log line `analysis_items_dropped` names any dropped item, so a recurrence can be tracked.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `meal-photo-analysis`: the returned items are only foods and drinks, with no summary rows or empty placeholders, and the totals are always the sum of the returned items.

## Impact

- **Backend:**
  - `src/services/vision-analysis.ts`: the item rules and a call to the new sanitizer;
  - a new pure `src/services/analysis-sanitizer.ts` (`isSummaryName`, `sanitizeAnalysis`);
  - tests for the sanitizer and for `analyzeMeal` with the fake Anthropic client.
- **App:** no change. Review already sums the items it shows.
- **Out of scope:** cleaning meals already saved with a "Total" item.
