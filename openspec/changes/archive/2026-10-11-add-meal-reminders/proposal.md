## Why

Most reference apps nudge the user to log meals. Research also showed that aggressive notifications create "tracking anxiety", so this is a simple, **opt-in** reminder rather than an engagement system. Tracked as GitHub issue #10: https://github.com/crmartinez930130/food-tracker-ai/issues/10

## What Changes

This change is app-only; the backend does not change.

- **A Reminders screen**, reachable from a bell icon in Home's header, with three reminders:
  - Breakfast at 08:30, Lunch at 13:00 and Dinner at 20:00, each with its own switch and time.
  - All are **off by default**.
  - Turning the first one on asks for the Android 13+ notification permission. If the permission is denied, the switch stays off and the screen explains how to allow it.
- **Local notifications** scheduled on the device with `AlarmManager`, with no server and no push:
  - **Text:** each reminder fires once a day at its time (within a 10-minute window) with "Did you log your lunch?" / "Tap to snap a photo of it."
  - **Delivery:** they are posted on a "Meal reminders" channel. Each meal reuses its own notification ID, so reminders replace rather than pile up.
- **Tapping the notification** opens the app directly on the capture screen. It never discards a meal the user is in the middle of reviewing.
- **Cleanup:** turning a reminder off cancels its scheduled alarm, leaving no orphaned notifications.
- **Persistence:** settings are stored on the device. Alarms are re-registered on app start and after a reboot or a clock or timezone change.

## Capabilities

### New Capabilities
- `meal-reminders`: opt-in, per-meal daily reminder notifications, configured in the app, that open the capture screen.

### Modified Capabilities
<!-- None. Home's header gains an entry point, but no existing requirement changes. -->

## Impact

- **Frontend common:**
  - `ui/reminders/` with `ReminderSettings` (model, defaults, JSON encoding), pure scheduling math (`nextTriggerAt`) and the `RemindersScreen` UI.
  - An `expect` `rememberReminderPlatform()` for storage, scheduling and the permission.
  - `Screen.Reminders` in `AppState`, a bell `IconButton` in Home's header, and `App` handling an "open capture" request.
- **Frontend Android:**
  - Storage in `SharedPreferences`.
  - Scheduling with `AlarmManager.setWindow`.
  - A `ReminderReceiver` that posts the notification, schedules the next day and handles `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `MY_PACKAGE_REPLACED`.
  - The notification channel.
  - In `MainActivity`, notification-tap handling (`onCreate`/`onNewIntent`).
  - Manifest: `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` permissions, plus the receiver.
- **Frontend iOS:** a stub, like the other platform features.
- **Icons:** Material Symbols `notifications` (Compose resource) and `restaurant` (Android notification small icon).
- **Dependencies:** none new (`androidx.core` is already declared).
