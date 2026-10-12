## Context

- **Platform features:** the app is Kotlin Multiplatform with Android as the real target. Platform features are `expect`/`actual` composables returning a small interface. Examples: `rememberImagePickerLauncher` and `rememberFileSharer`; the camera permission flow in `CameraCapture.android.kt`.
- **Navigation:** handled by `AppState.screen` (`Main`/`Capture`/`Review`/`Goals`). `MainActivity` hosts a single `App()`.
- **Emulator:** the test emulator runs API 37 (Google Play image, no root). `targetSdk` is 36.

## Goals / Non-Goals

**Goals:**
- Opt-in and quiet: nothing fires unless the user turns a reminder on, at most one notification per meal per day, and a reminder replaces itself instead of stacking.
- No orphaned alarms: what is scheduled always matches the settings.

**Non-Goals:**
- Server or push notifications.
- Snack reminders.
- Custom reminder texts.
- More than one reminder per meal.
- Skipping a reminder when that meal is already logged. This would need a network call from the receiver, so it is a possible follow-up.
- Exact-to-the-minute delivery, which would need the `SCHEDULE_EXACT_ALARM` special permission.
- iOS.

## Decisions

### D1. AlarmManager one-shot alarms, rescheduled on each fire
- **Mechanism:** each enabled reminder registers one `setWindow(RTC_WAKEUP, nextTrigger, 10 min, pendingIntent)`. When it fires, the receiver posts the notification and registers the next day's alarm.
- **Why `setWindow` with 10 minutes:**
  - It needs no special permission.
  - Android 14+ stretches any shorter window to 10 minutes anyway.
  - It keeps delivery close to the chosen time.
- **Why not plain `set`:** its inexact heuristic can defer an alarm by up to 75 % of the time until it is due. That is hours for a reminder set in the morning.
- **Why not WorkManager:** periodic work has flex windows and is subject to app standby buckets, so a 13:00 reminder could drift by a lot. One-shot work rescheduled each day would also add a dependency for no gain over AlarmManager.
- **Why not `setExactAndAllowWhileIdle`:** it requires `SCHEDULE_EXACT_ALARM`, which is denied by default for new installs on Android 14+, and minute precision isn't needed.
- **Doze:** `setWindow` alarms are deferred while the device is dozing. They fire at the next maintenance window or when the user picks up the phone, which is acceptable for a nudge.

### D2. One PendingIntent per meal, identified by request code
- **Identity:** the request code is the meal's ordinal. The intent carries the meal type, plus an explicit component (`ReminderReceiver`) and action `com.foodtracker.app.REMINDER`.
- **Canceling:** `alarmManager.cancel()` with an equal PendingIntent (same request code and intent filter equality) cancels exactly that meal's alarm.
- **Flags:** `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.

### D3. Settings are the single source of truth; `applyAll` reconciles
- **What is stored:** `ReminderSettings` is a list of `MealReminder(meal, hour, minute, enabled)` for breakfast, lunch and dinner, saved as JSON in `SharedPreferences`.
- **Missing or unreadable prefs:** these mean the defaults, with everything disabled. A corrupt value never crashes the app.
- **`applyAll(settings)`:** for each meal, it schedules the next trigger if the reminder is enabled and cancels it otherwise.
- **When it runs:**
  - after every settings change;
  - on app start, which recovers alarms lost to a force-stop;
  - from the receiver on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `MY_PACKAGE_REPLACED`.
- **Result:** the set of registered alarms always matches the settings, and turning a reminder off removes its alarm.

### D4. Pure, tested scheduling math in common code
- **The function:** `nextTriggerAt(now: Instant, hour, minute, timeZone): Instant` returns today at that local time if it is still ahead, and otherwise tomorrow. "Still ahead" means strictly after `now`, so an alarm firing at 13:00:00 schedules the next day instead of looping.
- **Daylight saving:** times are computed with kotlinx-datetime in the device zone. A time that doesn't exist on a DST spring-forward day resolves to the next valid instant, using kotlinx-datetime's `toInstant` behavior.
- **Other pure code:** the JSON encoding and the notification texts (`reminderTitle(meal)`).

### D5. Notification permission, asked at the moment of opting in
- **Android 13+ with `POST_NOTIFICATIONS` not granted:** turning a switch on launches the permission request.
  - If granted, the reminder is enabled and scheduled.
  - If denied, the switch stays off and the screen shows "Notifications are blocked for Food Tracker. Allow them in system settings to get reminders." with a button that opens the app's notification settings.
- **Below Android 13:** no runtime prompt.
- **At fire time:** the receiver also checks `NotificationManagerCompat.areNotificationsEnabled()` and silently skips posting when they are off, but still schedules the next day.

### D6. Tap opens Capture, without destroying work in progress
- **The notification's content intent:** `MainActivity` with extra `open_capture=true` and flags `SINGLE_TOP | CLEAR_TOP`, plus `autoCancel`.
- **Passing the request to Compose:** `MainActivity` turns the extra, from `onCreate` (only when not restoring) or `onNewIntent`, into an incrementing "open capture" request counter passed to `App`.
- **In `App`:** it calls `appState.openCaptureFromReminder()`. That switches to `Capture` only from `Main`, `Goals` or `Reminders`.
- **Capture or Review already showing:** a pending analysis is never discarded. The notification still dismisses.

### D7. Notification content and channel
- **Channel:** `meal_reminders`, "Meal reminders", default importance.
- **Content:**
  - title "Did you log your breakfast/lunch/dinner?";
  - text "Tap to snap a photo of it.";
  - small icon: Material Symbols `restaurant`, as a monochrome vector;
  - notification ID per meal.

### D8. Entry point and screen
- **Entry point:** a bell icon (`notifications`) in Home's header, next to edit-goal, opens `Screen.Reminders`. The screen uses a `TopAppBar` with a back arrow and `BackHandler`, the same pattern as Goals.
- **Each reminder row:** the meal name, its time as a `TextButton` that opens a Material 3 `TimePicker` in an `AlertDialog`, and a `Switch`.
- **Saving:** changes apply immediately, with no Save button. Changing an enabled reminder's time reschedules it.

## Risks / Trade-offs

- **The OEM or the user kills the app**, for example by force-stopping it, and alarms are dropped. → They are re-applied on the next app start. A force-stopped app also does not receive `BOOT_COMPLETED` until it is opened, which is Android's own rule.
- **Up to a 10-minute delay, or longer under Doze.** → This is acceptable for a nudge, and it is documented in the spec as "within about 10 minutes".
- **The boot broadcast is lost if the process is killed right after starting.** This was seen once during verification on a 2 GB emulator, where the low-memory killer fired during boot. → The alarms are re-applied the next time the app opens, and a second reboot re-registered them within 15 s.
- **Clock changes.** → `TIME_SET` and `TIMEZONE_CHANGED` trigger `applyAll`, which recomputes from the new local time.

## Migration Plan

None. These are new, app-local settings, and the defaults are off.
