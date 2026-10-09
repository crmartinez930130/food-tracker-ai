## Why

The app only shows raw totals: nothing tells the user whether 1,800 kcal is on track or way off. Every comparable app (MyFitnessPal, Cronometer, Lifesum, Cal AI) anchors its "today" view on a daily goal, and the upcoming home redesign (issue #3) needs one to draw a progress ring. Tracked as GitHub issue #2: https://github.com/crmartinez930130/food-tracker-ai/issues/2

## What Changes

- Persist a single daily nutrition goal: target calories, protein, fat and carbohydrates.
- New endpoint `GET /api/goals` returns the current goal. When none has been configured yet, it returns sensible defaults flagged as defaults, instead of an error or `null`.
- New endpoint `PUT /api/goals` creates or replaces the goal (upsert, no version history), validating the body.
- New "Goals" screen in the app, reachable from the main navigation, to view and edit the goal at any time. The saved value is loaded from the backend each time the screen opens.

## Capabilities

### New Capabilities
- `nutrition-goals`: storing, retrieving and updating the user's daily calorie and macro targets, including the default returned before any goal is configured.

### Modified Capabilities
<!-- None. Showing progress against the goal belongs to issue #3 (home redesign); daily/weekly summaries are unchanged. -->

## Impact

- **Backend:** new `goals` table plus a drizzle-kit migration in `backend/src/db/`, a goals repository, a `/api/goals` router mounted in `backend/src/app.ts`, and Zod request validation in `backend/src/routes/schemas.ts`.
- **Frontend:** `Goals` and `UpdateGoalsRequest` models in `Models.kt`, `getGoals()` and `updateGoals()` in `FoodTrackerApiClient`, a new `GoalsScreen`, and a third tab in `MainScreen`.
- **API:** additive only. Existing endpoints and data are untouched.
- **Unblocks:** issue #3 (home redesign with progress ring).
