## Why

Tracking food is ultimately about body weight. Without weight there is no way to correlate what was eaten with how weight moves. Every app we compared (MyFitnessPal, Cronometer, Lifesum, MacroFactor) logs it next to calories. Tracked as GitHub issue #11: https://github.com/crmartinez930130/food-tracker-ai/issues/11

## What Changes

- **Backend:**
  - New `weight_logs` table. Each entry has a weight in kg and the local calendar day it belongs to.
  - **Uniqueness per day is not enforced**, as the issue states.
  - New endpoints:
    - `POST /api/weight` logs a weight. The day defaults to today, and a past day can be given to log late.
    - `GET /api/weight?startDate=&endDate=` returns the entries in an inclusive range, in chronological order. A range with no entries returns an empty list.
    - `DELETE /api/weight/:id` removes a mistaken entry.
  - Validation: the weight must be greater than 0 kg and at most 500 kg, and the day must be a real date that isn't in the future.
- **Summary screen:** a new **Weight** section below the calorie chart, using the same Week/Month period:
  - a **line chart** of weight over the period, using the last entry of each day, drawn with `Canvas` like #7 and aligned to the same day slots;
  - the latest weight and its change over the period;
  - a **"Log weight"** button that opens a dialog with a numeric field, pre-filled with the last weight;
  - the period's entries, newest first, each with a delete button.

## Capabilities

### New Capabilities
- `weight-tracking`: logging, listing and deleting body-weight entries (API), and logging and viewing the weight trend in the app.

### Modified Capabilities
<!-- None: the calorie chart and its requirements are unchanged; Weight is an added section. -->

## Impact

- **Backend:**
  - New table plus a migration.
  - New `db/weight-repository.ts`, `routes/weight.ts` (mounted at `/api/weight`) and Zod schema.
  - `isCalendarDate` moves to a small shared module so `/weight` and `/summary` both use it.
  - Tests for all of the above.
- **Frontend:**
  - `WeightEntry` / `LogWeightRequest` models and three client methods.
  - New `ui/summary/WeightChart.kt` (Canvas) with pure layout helpers in `ChartLayout.kt`.
  - A `WeightSection` in `SummaryScreen`.
- **Dependencies:** none.
