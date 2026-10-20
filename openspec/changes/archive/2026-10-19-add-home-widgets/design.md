## Context

Home shows today from the Room cache (`DayRepository.observe`) merged with the offline queue's rows (`mergePending`), against the cached goal. Water is logged through `OutboxRepository.logWater`: it sends right away, or queues and asks `OutboxSync` (WorkManager) to send later. When a row is sent, it is merged into the cached day. Background code that runs without the UI (`OutboxSyncWorker`, `ReminderReceiver`) does the following:
- reads the stored settings with `PreferencesStore`;
- builds its API client with `storedSettingsOutbox`;
- applies the chosen language with `applyAppLocale`, then reads strings with Compose Resources' `getString`.

A Glance widget is drawn by a `GlanceAppWidget` in the app's process, on request (`update`/`updateAll`). Its content is static between requests: it doesn't keep observing flows.

## Goals / Non-Goals

**Goals:**
- Both widgets show exactly what Home would show for today, offline included.
- They stay current without the user opening the app.
- They cost nothing when none is placed.

**Non-Goals:**
- iOS.
- Lock screen or Wear OS.
- Logging meals from the widget.
- Generated (live) previews in the picker.

## Decisions

### D1: One pure state for the widget, in common code
`todayWidgetState(day: CachedDay?, rows: List<OutboxRow>, today: String, now: Long)` returns a `TodayWidgetState`:
- `calories`, `targetCalories`, `remaining` and `fraction`;
- protein, fat and carbs;
- `waterMl`, `targetWaterMl` and `glassMl`;
- `staleSince: Long?`.

It reuses `mergePending`, so pending meals and glasses count, as on Home. With no cached copy of today it uses the offline placeholder: zero intake against the cached goal, or the default goal. This keeps all the logic testable in `commonTest`. The widgets only draw the state.

### D2: Staleness note
The widget can't know whether the backend is reachable right now. It shows "Data from HH:mm" only when the cached copy of today is older than **60 minutes**: the background refresh runs every 30 minutes, so an older copy means refreshes are failing. With no copy of today at all, it shows "No data for today yet". Fresh data shows no note.

*Alternative considered:* storing the last refresh outcome. Rejected because it adds state that age alone already answers.

### D3: Redraw on database changes
`cacheDatabase(context)`, the process's single instance, starts one process-scoped collector on Room's invalidation flow for `day_cache`, `goal_cache` and `outbox`. After a short debounce (~300 ms), it calls `updateAll` on both widgets.

This covers every writer in the process: Home, the day detail, goals, the outbox sender and the widget's own glass. No call sites need to remember to refresh the widgets.

*Alternative considered:* explicit `requestUpdate()` calls at each write. Rejected because they are easy to forget and spread over common code.

### D4: Background refresh only while placed
A unique periodic WorkManager job, `widget-refresh`, refreshes today's day and goal through `DayRepository.refresh`:
- every **30 minutes**;
- requires `NetworkType.CONNECTED`;
- builds its client from the stored settings, like the outbox worker.

Both receivers enqueue the job in `onEnabled` (KEEP policy). It is cancelled when the last widget of either kind is removed: `onDisabled` checks that neither widget has instances left. A refresh that fails leaves the cache as it was, and D2 then shows the age.

### D5: Midnight
Each widget update schedules an inexact `AlarmManager.set` for the next local midnight plus one minute. No exact-alarm permission is needed. The alarm reaches a small receiver that:
1. calls `updateAll`, so the widgets switch to the new day's placeholder at once;
2. enqueues a one-time refresh.

Being inexact, the alarm may arrive a few minutes late in doze. That's acceptable for a glance widget.

### D6: The glass action
"+ glass" is an `actionRunCallback`. The callback:
1. loads the stored settings and builds `storedSettingsOutbox(..., requestSync = { OutboxSync.request(context) })`. The new optional parameter defaults to the current no-op;
2. logs `LogWaterRequest(glassMl, loggedAt = now)` for today. The glass size comes from the cached goal, or the default goal.

The outcome reaches the widgets through D3:
- **sent at once:** the queue doesn't touch the cached day on this path (Home refreshes after its own glass), so the callback refreshes today from the backend. That writes the cache, and D3 redraws;
- **queued while offline:** the new queue row redraws the widgets, the glass shows as pending, and it is sent later, exactly once (client id);
- **refused at once by the backend (4xx):** nothing is stored or queued, as on Home, so the day stays as it was. The widget doesn't show errors itself.

### D7: Opening the app
- **"Log a meal":** opens `MainActivity` with `EXTRA_OPEN_CAPTURE`, the same intent the reminders use.
- **Tapping "Today's calories":** opens `MainActivity` plainly, which lands on Home.

### D8: Language and look
- **Strings:** `provideGlance` first calls `applyAppLocale(context)`, then loads its strings with Compose Resources' `getString`, like the reminders. They live in `commonMain` `values`/`values-en`, and the widget follows the app's language on every Android version.
- **Picker texts:** the receiver's label and description are platform resources (`androidMain/res/values` and `values-en`), shown in the device's language.
- **Colors:** `GlanceTheme(colors = ColorProviders(light = LightColors, dark = DarkColors))` reuses the app's generated scheme. Glance picks light or dark from the system.
- **Picker previews:** a static `previewImage` per widget, a screenshot of the real widget with sample data.

### D9: Sizes
- **"Today's calories":** 4×2 cells by default (`targetCellWidth/Height`), resizable. `SizeMode.Responsive` with two layouts:
  - **narrow:** calories and progress only;
  - **wide:** adds the macros and the water line.
- **"Quick actions":** 2×2 by default, resizable. Its buttons stack vertically when narrow and sit side by side when wide.

## Risks / Trade-offs

- **[Room's invalidation flow on Room 3 differs from Room 2.7]** → Use whatever invalidation API Room 3 exposes for observing tables. If none fits, fall back to observing `DayRepository.observe(today)` and `OutboxRepository.observeDay(today)` from the same process-scoped collector.
- **[The collector keeps a flow open while the process lives]** → It's a single cheap observer, and the process only lives while the app or a worker runs.
- **[Debounced `updateAll` with no widgets placed]** → `updateAll` with no instances does nothing. It's skipped early by checking `GlanceAppWidgetManager.getGlanceIds`.
- **[Launchers limit widget update frequency]** → Updates come from data changes, the 30-minute refresh and midnight, all well under the limits.
