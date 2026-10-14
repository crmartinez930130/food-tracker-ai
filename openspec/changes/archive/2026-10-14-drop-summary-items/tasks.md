## 1. Sanitizer

- [x] 1.1 Add `src/services/analysis-sanitizer.ts` with `isSummaryName` and `sanitizeAnalysis` (D2, D3). Verify with tests:
  - the summary names in Spanish and English, with case, accents, a colon and parentheses;
  - real foods such as "Total cereal", "Water" and "Totopos" are not summaries;
  - an all-zero "Total" is dropped;
  - empty rows with a blank or repeated name are dropped (added after verification found them);
  - a "Total" equal to the sum is dropped, within tolerance;
  - a "Total" with unrelated values is kept;
  - 0 kcal water is kept;
  - the totals are recomputed and rounded;
  - an empty list.

## 2. Analysis

- [x] 2.1 Add the sentence to `ITEM_RULES` (D1). Call `sanitizeAnalysis` in `analyzeMeal` and log `analysis_items_dropped` (D4). Verify with `analyzeMeal` tests using the fake client:
  - a reply with a "Total" row comes back without it, with summed totals and the log line;
  - a clean reply is unchanged apart from recomputed totals, with no drop log;
  - both prompt modes include the new rule;
  - `npm run typecheck` and `npm test` pass.

## 3. Verification

- [x] 3.1 Against the real API, through the running backend, analyze "a banana and a glass of milk" (the case that produced the "Total" row) a few times in English and Spanish. Check that no summary or empty item comes back, that the totals equal the items' sum, and that the drop log appears when the model adds one.
