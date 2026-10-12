## Why

The reviews of #28, #29 and #31 left small non-blocking suggestions. Each one is a rough edge the user can notice:
- a stale "notifications blocked" message;
- a reminder for a meal that is already logged, which goes against the no-anxiety goal of #10;
- an export picker with a hard-coded 2020;
- a misleading error for impossible dates.

Tracked as GitHub issue #32: https://github.com/crmartinez930130/food-tracker-ai/issues/32

## What Changes

- **Reminders screen:** the "notifications are blocked" message is re-evaluated every time the screen comes back to the foreground. It appears or disappears after the user changes the permission in system settings, without reopening the screen.
- **Reminders:** when a reminder fires, the app checks today's meals on the backend with a short timeout.
  - If a meal of that type is already logged today, the notification is **not** shown.
  - If none is logged, or the backend can't be reached, it is shown as before.
  - Either way, the next day is scheduled.
- **Export picker:**
  - **Backend:** a new `GET /api/meals/first-date` returns the local date of the earliest logged meal, or `null` when there are none.
  - **App:** the picker's years go from that meal's year (or the year of the Summary period, if earlier) to the current year, replacing the fixed 2020. When there are no meals, or the request fails, it falls back to a range relative to today.
- **Backend date errors:** every route that rejects an impossible but well-formed date such as `2026-02-30` now says "… must be a real date in YYYY-MM-DD format", the wording `/api/water` and `/api/weight` already use. The text comes from one shared place.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `meal-reminders`: the blocked-notifications message reflects the current permission when the screen resumes; reminders are skipped when that meal is already logged today.
- `data-export`: a new earliest-meal-date endpoint, and the export picker's year range derives from it instead of a fixed year.

## Impact

- **Backend:**
  - `routes/dates.ts` gains the shared date error messages, used by the `summary`, `meals` and `water` routes and by `parseDateRange`.
  - New `firstMealDate` query in `meal-repository.ts`.
  - New `GET /api/meals/first-date` route, registered above `/:id`.
  - Tests.
- **Frontend:**
  - `firstMealDate()` client method.
  - Pure `exportYearRange(...)` helper, used by `ExportSection`.
  - Pure `shouldShowReminder(meal, mealsToday)` helper.
  - `ReminderReceiver` uses `goAsync` with a coroutine that calls the backend under a timeout.
  - `RemindersScreen` uses `LifecycleResumeEffect`.
- **Dependencies:** `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` 2.9.6 is declared explicitly. It already resolves transitively through Compose Multiplatform.
