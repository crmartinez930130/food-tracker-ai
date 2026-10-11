# Design

## Context

- **Backend:** `services/summary.ts` computes `dailySummary` (one date) and `weeklySummary` (7 days from a start date) by summing meal entries loaded through `listMealsByDate` / `listMealsBetween`. Both group days by `date(occurred_at, 'localtime')`, because the server's timezone is the user's (see #4 / setup design).
- **Frontend:** since #3, `SummaryScreen` is weekly-only. It shows the 7 days ending today with chevron navigation and two text cards: the week total and the per-day average. The daily goal is available through `GET /api/goals` (#2). The theme (#1) gives `primary` / `error` / `outlineVariant` roles, all already covered by the contrast test.

See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One request per period gives everything Summary needs: the chart, the averages and the total.
- A chart that is legible in light and dark mode, with no new dependency.
- Testable chart math.

**Non-Goals:**
- Macro charts, stacked bars, tooltips, pinch/zoom or scrolling charts.
- Calendar-aligned weeks or months (Monday to Sunday, the 1st to the 31st). Periods are rolling windows ending on the selected day.
- Removing the `/weekly` endpoint.

## Decisions

**1. The aggregation is done in SQL, and zero-filling in the service.**
- `dailyTotalsBetween(db, start, endExclusive)` runs `SELECT date(occurred_at,'localtime') AS day, SUM(total_calories)… GROUP BY day ORDER BY day` over `meals`. Only the stored totals are needed, so it avoids loading every item, which `listMealsBetween` does.
- `rangeSummary(db, start, end)` walks every date from `start` to `end` inclusive, using the existing `addDaysToDateString`, and puts a zero entry where SQL returned no row.
- The route validates both dates with the same `DATE_RE` regex, requires `end >= start`, and caps the range at 366 days. A request without a cap could ask for decades of zero-filled entries.
- *Alternative:* reuse `listMealsBetween` and group in JS, as the issue suggests. It works, but it loads every meal's items for nothing, and grouping in SQL keeps the "local day" rule in the same place as the other queries.

**2. The response shape is a flat array of `{ date, totalCalories, totalProteinGrams, totalFatGrams, totalCarbsGrams }`.** It reuses the `MealTotals` field names, so the frontend's `NutritionTotals` vocabulary stays consistent. The new `DailyTotals` model adds `date`.

**3. Chart: Compose `Canvas`, no library.**
- `ChartLayout.kt` (pure, in commonMain) computes the bar heights and the goal line position:
  - `chartScaleMax(values, goal)` is the larger of the highest value and the goal, rounded up to a "nice" step and never 0.
  - `barFraction(value, scaleMax)` returns a value in `0..1`.
  - `isOverGoal(value, goal)` follows the same rule as `progressOf` on Home: over only when strictly above a positive goal.
- `CalorieChart.kt` draws the bars with rounded tops: `primary` for days at or under the goal and `error` for days over it. The goal is a dashed `outline` line, with "goal N" labelled on the left, and the baseline uses `outlineVariant`.
- **X labels:**
  - A week shows a two-letter weekday for every bar (Mo, Tu, …). Single initials would repeat T/T and S/S, as the PR review pointed out.
  - A month shows the day of the month every 5 bars, so the labels don't collide.
- Whole-chart semantics give a `contentDescription` such as "Calories per day, May 3 to May 9; average 1830 kcal; 2 days over goal" for TalkBack.
- *Alternative:* Vico 2.5 (`com.patrykandpatrick.vico:multiplatform`). It is mature and multiplatform, but it is a sizeable dependency with its own model and producer API for two simple charts, and #11's line chart can follow the same Canvas approach. Revisit it if we ever need scrolling or zoomable charts or tooltips.

**4. The Summary screen state is `period` (WEEK = 7 or MONTH = 30 days) plus an `endDate`, which defaults to today.**
- The start date is `endDate - (days - 1)`. The chevrons move `endDate` by ±days, and "next" is disabled once `endDate` is today.
- Switching period keeps the same `endDate`.
- The screen loads `rangeSummary(start, end)` and `getGoals()`. The averages are computed client-side as `sum / days`, counting empty days as zero, which matches the backend's weekly semantics. The total kcal is shown next to them.
- The old two text cards are replaced by the chart plus a compact averages block.

## Risks / Trade-offs

- **[A hand-rolled chart can drift in quality]** → It is scoped to bars plus a goal line. The math is unit-tested, and the visuals are checked on the emulator in both themes.
- **[30 bars on a narrow phone are thin]** → At about 360dp width that is still around 8dp per bar plus gaps, which is legible. Labels are thinned to every 5th day.
- **[Changing Summary from week-from-today to "ends today"]** → This was already the case since #3. Nothing changes for the week.
