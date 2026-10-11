## Why

Summary shows a week's totals and per-day averages as plain text, so patterns like "every Friday is over" or "this week trended down" aren't visible at a glance. Every reference app shows calorie intake over time as a chart. Tracked as GitHub issue #7: https://github.com/crmartinez930130/food-tracker-ai/issues/7

## What Changes

- **Backend:** new `GET /api/summary/range?startDate=&endDate=` (both inclusive) returning **one entry per day**: the date plus total calories, protein, fat and carbs.
  - Days without meals come back as zeros, not omitted.
  - Invalid dates, `endDate` before `startDate` and ranges longer than 366 days are rejected with 400.
- **Summary screen:**
  - A **Week / Month** toggle over the last 7 or 30 days, ending today by default, with previous/next stepping by the same window.
  - A **bar chart of calories per day** with a dashed line at the daily calorie goal. Days over the goal are drawn in the error color, as on Home.
  - Under the chart, the period's daily averages (kcal and macros) and total kcal, computed from the same per-day data.
- **The chart is drawn with Compose `Canvas`, with no chart library.** The layout math (scale, bar heights, goal line position) is pure and unit-tested. #11 (weight) can reuse the same approach for a line chart.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `nutrition-summary`: adds a per-day breakdown over a date range (API) and the trend chart on the Summary screen.

## Impact

- **Backend:**
  - New aggregate query `dailyTotalsBetween` in `meal-repository.ts`, which groups by local date in SQL like the existing date queries.
  - New `rangeSummary` in `services/summary.ts`, which fills the days with no data with zeros.
  - New route in `routes/summary.ts`.
  - Tests for all of the above.
- **Frontend:**
  - `DailyTotals` model and `rangeSummary()` client method.
  - New `ui/summary/CalorieChart.kt` (Canvas) and `ChartLayout.kt` (pure math), plus a reworked `SummaryScreen`.
- **API:** additive. The existing `/daily` and `/weekly` endpoints are unchanged. The app stops calling `/weekly`, but the endpoint is left as is.
- **Dependencies:** none.
