## Context

- **`RemindersScreen`:** `notificationsBlocked` is a `remember`ed value computed once, at screen entry.
- **`ReminderReceiver`:** posts the notification synchronously in `onReceive` and calls `ReminderScheduler.applyAll`.
- **The backend:** reachable from the app process with `FoodTrackerApiClient(createHttpClient())`, using the default `BackendConfig.baseUrl`.
- **`ExportSection`'s picker:** uses `yearRange = 2020..todayYear`. Material 3's range picker requires the initially displayed month to fall inside `yearRange`.
- **Date error messages:** "must be in YYYY-MM-DD format" in `dates.ts` (`parseDateRange`), `summary.ts` and `meals.ts`; "must be a real date in YYYY-MM-DD format" in `water.ts` and `weight.ts`.

## Goals / Non-Goals

**Goals:**
- Each fix is small, local, and tested where it has logic.

**Non-Goals:**
- Changing reminder times or texts.
- Treating an untagged meal ("Other") as covering a reminder.
- Disabling picker days before the first meal: only the year range changes, and exporting an empty stretch is harmless.
- iOS.

## Decisions

### D1. Re-check notification permission on resume
- `LifecycleResumeEffect(Unit) { notificationsBlocked = anyEnabled && !platform.canNotify(); onPauseOrDispose {} }` runs on every resume, including the return from system settings.
- **Keeping a denial visible:** after a denied request with no enabled reminders, the message stays visible until notifications are allowed. A resume where `canNotify()` is true clears it.
- **Library:** this is the multiplatform `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`, declared explicitly. It is the same version, 2.9.6, that already resolves transitively, just as `ui-backhandler` was declared in #6.
- **Alternative:** an Android-only `ON_RESUME` observer in the platform actual. Rejected: the screen logic is common, and this effect is the standard multiplatform API.

### D2. Skip a reminder when that meal is already logged
- **`onReceive`:**
  - calls `goAsync()`;
  - launches a coroutine on `Dispatchers.IO`;
  - fetches `listMeals(today)` under `withTimeoutOrNull(4 s)`, on a client whose Android engine also has 4 s connect and socket timeouts, because a blocking connect to a sleeping laptop may not react to coroutine cancellation. Any exception counts as "unknown";
  - calls `shouldShowReminder(meal, mealsOrNull)`;
  - posts the notification if that returns true;
  - always calls `applyAll`, then `finish()` in a `finally` block.
- **Time budget:** the 4 s timeout stays well inside the ~10 s a `goAsync` receiver may run.
- **`shouldShowReminder(meal, mealsToday: List<MealEntry>?)`:**
  - `null` (backend unreachable or slow) → true. A spurious reminder beats a missed one.
  - Otherwise → true unless some meal has `mealType == meal`.
- **Untagged meals** don't count, because they can't be attributed to a meal.
- **System broadcasts** (boot, time, timezone, update) skip the network entirely and only re-apply alarms, as today.
- **Alternative:** check in `ReminderScheduler` ahead of time. Rejected: the meal can be logged at any moment before the reminder fires.

### D3. Earliest meal date from the backend
- **The endpoint:** `GET /api/meals/first-date` returns `{ "date": "YYYY-MM-DD" | null }`, the local calendar day of `min(occurred_at)`, computed with SQLite `date(min(occurred_at), 'localtime')`.
- **Registration:** the route sits above `/:id`, like `/favorites` and `/export`.
- **Alternative:** a relative range only, such as the last 10 years. Rejected: the issue prefers deriving it from real data, and the endpoint is a single aggregate query.

### D4. Picker year range
- **The helper:** `exportYearRange(firstMealDate: String?, periodStart: String, today: String): IntRange` returns `min(firstYear, periodStartYear)..todayYear`.
- **No first date** (no meals, or the request failed): it returns `min(todayYear - 10, periodStartYear)..todayYear`. Ten years is ample for a personal history, and the period start is always included, so the initial month is always valid.
- **When the date is fetched:** `ExportSection` fetches it when the user taps Export CSV, before showing the dialog. The fetch shows no loading UI, because it is one tiny request. A failure silently uses the fallback.

### D5. One shared wording for invalid dates
- **`dates.ts` exports:**
  - `invalidDateMessage(name)`, which returns "Query parameter '<name>' must be a real date in YYYY-MM-DD format";
  - the range variant used by `parseDateRange`.
- **Callers:** `summary.ts` (daily, weekly), `meals.ts` (list) and `water.ts` use it. `weight.ts` already says "day: must be a real date…", for a body field, and stays as is.
- Tests assert the message for `2026-02-30` on each route.

## Risks / Trade-offs

- **A reminder fires while the backend is on a laptop that's asleep.** → The 4 s timeout shows the reminder anyway, which is the pre-existing behavior. There is never silence because of a network error.
- **A meal is logged as "Other" at lunchtime.** → The lunch reminder still fires. Users who tag meals get the skip, and tagging is the default flow from Review.

## Migration Plan

None. These are additive endpoint and UI changes.
