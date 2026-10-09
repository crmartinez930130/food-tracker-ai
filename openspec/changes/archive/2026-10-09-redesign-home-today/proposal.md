## Why

The app opens on a generic "History" list. Nothing tells the user how today is going against their goal, even though the goal now exists (#2). Every reference app (MyFitnessPal, Cronometer, Lifesum, Cal AI) opens on **today's diary**: a calorie progress ring with macro bars under it and the day's meals. This change makes our home follow that pattern. Tracked as GitHub issue #3: https://github.com/crmartinez930130/food-tracker-ai/issues/3

## What Changes

- **New Home screen**, replacing the History tab:
  - Calorie progress ring for the selected day against the daily goal: consumed, goal, and remaining or over.
  - Protein, fat and carbohydrate progress bars against their targets.
  - The day's meals, grouped by meal type (#4), each one deletable.
  - A date header with previous/next-day buttons and a "Today" / "Yesterday" / date label. Moving past today is not allowed.
  - A hint when the goal is still the built-in default.
- **Goals moves off the bottom bar.** It opens from an "edit goal" action in the Home header or by tapping the progress ring. It gets a top bar with a back arrow, and system back returns to Home on the same day.
- **Bottom navigation goes down to two destinations:** Home and Summary.
- **Summary becomes weekly-only**, focused on trends. Its Daily mode is removed, because the day now lives on Home.
- After saving a meal, the app returns to Home on today, with the progress already updated.

## Capabilities

### New Capabilities
- `daily-dashboard`: the Home screen. It shows the selected day's calorie and macro progress against the goal and the day's meals, and lets the user navigate between days.

### Modified Capabilities
- `nutrition-goals`: the goal screen is reached from Home (header action or progress ring) instead of from the main navigation, and supports going back.
- `app-appearance`: the bottom navigation has two destinations, Home and Summary, instead of History, Summary and Goals.

## Impact

- **Frontend only:**
  - `HistoryScreen` is replaced by a new `HomeScreen`.
  - `MainTab` changes to `HOME`/`SUMMARY`, and `AppState` gets a `Goals` route and keeps the Home date.
  - `GoalsScreen` gets a back-enabled top bar.
  - `SummaryScreen` becomes weekly-only.
  - New Material Symbols drawables: `home`, `chevron_left`, `chevron_right`, `arrow_back`, `edit`.
  - The progress and date-label logic moves to pure functions, with unit tests.
- **Backend/API:** none. Home uses the existing `GET /api/meals?date=` and `GET /api/goals`.
- **Dependencies:** `org.jetbrains.compose.ui:ui-backhandler`, for the multiplatform `BackHandler`, is declared explicitly. It was already shipped transitively at runtime, so no new library enters the app.
