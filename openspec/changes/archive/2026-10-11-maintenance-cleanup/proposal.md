## Why

Several small issues surfaced while building #2, #3, #5, #6 and #7 and were kept out of those PRs. Two of them were flagged by the automated reviewer. Fixing them together before adding more features keeps the base consistent. Tracked as GitHub issue #26: https://github.com/crmartinez930130/food-tracker-ai/issues/26

## What Changes

1. **Loading indication when navigating.** On Home (changing day) and Summary (changing period, both the calorie and weight sections), the previously loaded data is dimmed and a thin progress bar is shown until the new data arrives. This replaces showing the old data unmarked under the new date.
2. **Real-date validation everywhere.** `GET /api/summary/daily`, `GET /api/summary/weekly` and `GET /api/meals?date=` reject impossible dates such as `2026-02-30` with 400, using the shared `isCalendarDate` (#7/#11).
3. **Readable validation errors for meals.** `POST /api/meals` and `PATCH /api/meals/:id` return `field: message` errors instead of raw Zod JSON. A shared formatter replaces the copies in `/goals` and `/weight`.
4. **The client's `updateMeal` sends only the fields that are set.** `UpdateMealRequest` stops sending explicit `null`s, which the backend rejected (`items`) or treated as "clear" (`notes`, `mealType`).
5. **Android manifest** declares the camera as optional (`uses-feature … required="false"`), since the app already falls back to the gallery (#5).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `meal-tracking`: adds requirements for readable validation errors and for rejecting impossible dates when listing by date.
- `nutrition-summary`: adds requirements for rejecting impossible dates in the daily and weekly summaries, and for a loading indication while the Summary period changes.
- `daily-dashboard`: adds a requirement for a loading indication while Home changes day.

## Impact

- **Backend:**
  - New `routes/validation.ts` with the shared Zod error formatter.
  - Changes to `routes/meals.ts`, `routes/summary.ts`, `routes/goals.ts` and `routes/weight.ts`.
  - New and updated route tests.
- **Frontend:**
  - New `ui/LoadingState.kt` with a pure presentation helper and its tests.
  - Changes to `HomeScreen`, `SummaryScreen` and `WeightSection`.
  - `UpdateMealRequest` fields get `@EncodeDefault(NEVER)`, with a client test of the exact body.
  - `AndroidManifest.xml`.
- **API:** error message text changes for meal validation errors. Status codes are unchanged.
