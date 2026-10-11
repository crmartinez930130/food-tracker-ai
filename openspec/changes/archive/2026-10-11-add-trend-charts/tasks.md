# Tasks

## 1. Backend

- [x] 1.1 Add `dailyTotalsBetween(db, startInclusive, endExclusive)` to `meal-repository.ts` (SQL `GROUP BY date(occurred_at,'localtime')` with summed totals, ordered by day); verify with repository tests: sums per day, days without meals absent, ordering
- [x] 1.2 Add `rangeSummary(db, startDate, endDate)` to `services/summary.ts` returning one `{ date, total* }` per day inclusive with zero-fill; verify with service tests: data on every day, empty days zero-filled, single-day range, range crossing a month boundary
- [x] 1.3 Add `GET /api/summary/range?startDate=&endDate=` to `routes/summary.ts` with validation (format, end ≥ start, ≤ 366 days); verify with route tests: 200 with one entry per day incl. zeros, single day, and 400 for malformed date, missing param, end before start, and a 367-day range

## 2. Frontend data and chart math

- [x] 2.1 Add `@Serializable DailyTotals(date, totalCalories, totalProteinGrams, totalFatGrams, totalCarbsGrams)` and `rangeSummary(startDate, endDate)` to the client; verify with a `commonTest` deserialization case including a zero day and the request's query parameters
- [x] 2.2 Add `ui/summary/ChartLayout.kt` (`chartScaleMax`, `barFraction`, `isOverGoal`, label thinning helper); verify with `ChartLayoutTest`: scale covers the goal when all values are below it, covers the max when above, never 0 for empty data, fractions clamp to 0..1, over-goal rule matches Home's, month labels every 5th day

## 3. Frontend UI

- [x] 3.1 Add `ui/summary/CalorieChart.kt` (Canvas bars in `primary`/`error`, dashed goal line with label, baseline, x labels, whole-chart `contentDescription`); verify it compiles
- [x] 3.2 Rework `SummaryScreen`: Week/Month segmented toggle, chevrons moving the end date (next disabled at today), period label, chart + averages (kcal and macros) and total kcal computed from the range data, goal from `getGoals()`; verify `:composeApp:assembleDebug` and `:composeApp:testDebugUnitTest` pass

## 4. End-to-end verification

- [x] 4.1 On the emulator against the real backend, seed meals via the API (no analysis calls) on several days of the last month, one above the goal: Summary week shows the right bars, goal line and an over-goal bar in the error color; averages match the seeded values; switch to Month and navigate back/forward (next disabled at today); light and dark screenshots. Delete the seeded meals afterwards
