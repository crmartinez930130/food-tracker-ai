## 1. Common: model and scheduling math

- [x] 1.1 Create `ui/reminders/ReminderSettings.kt` with these pure pieces:
  - the `MealReminder`/`ReminderSettings` model with defaults (off, 08:30/13:00/20:00);
  - JSON encode/decode that falls back to the defaults on missing or corrupt input;
  - `nextTriggerAt(now, hour, minute, zone)`;
  - `reminderTitle(meal)` and the time label.

  Verify with `ReminderSettingsTest`:
  - defaults are all off;
  - JSON round-trip, plus garbage and partial input fall back to the defaults;
  - next trigger later today, at exactly now (tomorrow), already passed (tomorrow);
  - DST spring-forward and fall-back days in `Europe/Madrid`;
  - the texts.

## 2. Android: storage, scheduling, receiver

- [x] 2.1 Add the notification icons (Material Symbols `notifications` as a Compose resource, `restaurant` as an Android drawable), the manifest permissions (`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`) and the `ReminderReceiver` declaration with its system-broadcast filters. Verify with `./gradlew :composeApp:assembleDebug`.
- [x] 2.2 Implement the Android side in `androidMain`:
  - `ReminderStore` (`SharedPreferences`);
  - `ReminderScheduler.applyAll` (`AlarmManager.setWindow`, one PendingIntent per meal, cancel when off);
  - the channel setup;
  - `ReminderReceiver` (post the notification with the open-capture content intent, schedule the next day, and `applyAll` on boot, time, timezone and package-replaced broadcasts).

  Verify with `./gradlew :composeApp:assembleDebug` and the emulator checks in 4.1.

## 3. Common UI and wiring

- [x] 3.1 Add the `expect`/`actual` `rememberReminderPlatform()`, which loads and saves settings, applies schedules and checks and requests the notification permission. The Android version uses an activity-result permission launcher; the iOS version is a stub. Verify with `./gradlew :composeApp:assembleDebug`.
- [x] 3.2 Build `RemindersScreen`: three rows with a `Switch` and a time button opening a `TimePicker` dialog, a back arrow and `BackHandler`, and the blocked-permission message with an "Open settings" button. Also add `Screen.Reminders` and `openReminders`, plus the bell icon in Home's header. Verify with `./gradlew :composeApp:testDebugUnitTest assembleDebug`.
- [x] 3.3 Handle the notification tap:
  - `MainActivity` turns the `open_capture` extra into an open-capture request counter passed to `App`;
  - `AppState.openCaptureFromReminder()` only leaves `Main`, `Goals` or `Reminders`;
  - `MainActivity` re-applies the schedules on start.

  Verify the guard with `AppStateTest`: it opens capture from Main, Goals and Reminders, and keeps Capture and Review with their pending analysis.

## 4. End-to-end verification

- [x] 4.1 On the emulator (API 37) against the real backend:
  - On a fresh install, `dumpsys alarm` shows no reminder alarms and the screen shows all three off.
  - Turning lunch on prompts for the permission. If it is denied, the switch stays off and the message shows. If it is allowed, an alarm appears in `dumpsys alarm` for the next time.
  - Set a reminder a couple of minutes ahead, or advance the system clock if the image allows it, and check the notification appears with the right text.
  - Tapping the notification opens Capture.
  - Turning the reminder off removes the alarm from `dumpsys alarm`.
  - After a reboot (`adb reboot`), an enabled reminder's alarm is registered again.
  - The settings persist across a force-stop.

  Afterwards, leave every reminder off.
