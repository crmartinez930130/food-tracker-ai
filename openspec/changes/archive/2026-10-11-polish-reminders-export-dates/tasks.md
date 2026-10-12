## 1. Backend

- [x] 1.1 Add `invalidDateMessage(name)` and the range wording to `routes/dates.ts`, then use them in `parseDateRange`, `summary.ts` (daily, weekly), `meals.ts` (list) and `water.ts`. Verify with route tests asserting "must be a real date in YYYY-MM-DD format" for `2026-02-30` on `/api/summary/daily`, `/weekly`, `/range`, `GET /api/meals`, `/api/meals/export` and `/api/water`.
- [x] 1.2 Add `firstMealDate(db)` (SQLite `date(min(occurred_at),'localtime')`) and `GET /api/meals/first-date` above `/:id`. Verify with repository and route tests: no meals gives `null`; the earliest local date wins regardless of insertion order; a late-evening earliest meal returns its local day; the route is not captured by `/:id`.

## 2. Frontend: reminders

- [x] 2.1 Add the pure `shouldShowReminder(meal, mealsToday: List<MealEntry>?)`. Verify with `ReminderSettingsTest` cases: `null` gives true, no meals gives true, a different type gives true, the same type gives false, untagged gives true.
- [x] 2.2 `ReminderReceiver`: on a reminder alarm, use `goAsync` and a coroutine on `Dispatchers.IO`, run `listMeals(today)` under a 4 s `withTimeoutOrNull`, with 4 s engine timeouts (exceptions count as null), apply `shouldShowReminder`, then always `applyAll` and `finish()` in `finally`. System broadcasts only call `applyAll`. Verify with `./gradlew :composeApp:assembleDebug` and the emulator checks in 4.1.
- [x] 2.3 `RemindersScreen`: re-evaluate `notificationsBlocked` in `LifecycleResumeEffect`, and declare `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` 2.9.6 in commonMain. Verify with `./gradlew :composeApp:assembleDebug` and the emulator checks in 4.1.

## 3. Frontend: export picker

- [x] 3.1 Add the `firstMealDate()` client method and the pure `exportYearRange(firstMealDate, periodStart, today)`, then use them in `ExportSection`: fetch on tap, fall back on failure, replace the fixed 2020. Verify with `FoodTrackerApiClientTest` (path, both `date` and `null` parsed) and `ExportRangeTest`:
  - first year earlier than the period;
  - period earlier than the first year;
  - fallback with no date;
  - the period start always inside the range.

## 4. End-to-end verification

- [x] 4.1 On the emulator against the real backend:
  - Reminders blocked message: block notifications in system settings with a reminder on, come back, and the message shows. Allow them, come back, and it hides.
  - Reminder skip: with a lunch meal seeded through the API for today, advance the clock past the reminder and check that no notification appears and the next day stays scheduled. With the lunch meal deleted, the notification appears. With the backend stopped, it also appears.
  - The export picker's years start at the earliest meal's year.
  - `curl` on an impossible date shows the new message.

  Clean up: delete the seeded meals, turn reminders off and set the clock back to automatic.
