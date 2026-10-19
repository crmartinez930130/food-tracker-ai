## 1. State

- [x] 1.1 Add `TodayWidgetState` and `todayWidgetState(...)` in common code (D1, D2), reusing `mergePending` and the offline placeholder's goal. Add tests for:
  - pending meals and glasses counting;
  - no copy of today;
  - the 60-minute stale note;
  - kcal left never below zero;
  - the glass size.

## 2. Widgets

- [x] 2.1 Add the Glance dependencies.
- [x] 2.2 Add the "Today's calories" widget:
  - responsive narrow and wide layouts (D9);
  - the app's colors (D8);
  - the tap opens Home (D7);
  - the receiver, `appwidget-provider` XML, label and description in both languages, and manifest entry.
- [x] 2.3 Add the "Quick actions" widget:
  - the glass action through `storedSettingsOutbox` with the new optional `requestSync` (D6);
  - "Log a meal" with `EXTRA_OPEN_CAPTURE` (D7);
  - receiver, XML and manifest entry.
- [x] 2.4 Add the strings in `commonMain` `values` and `values-en`, and load them after `applyAppLocale` (D8).

## 3. Staying current

- [x] 3.1 Redraw both widgets on Room invalidations of `day_cache`, `goal_cache` and `outbox`, debounced, from the process's database (D3).
- [x] 3.2 Add the periodic `widget-refresh` worker:
  - every 30 minutes, with a network;
  - enqueued in `onEnabled`, cancelled when no widget of either kind remains (D4).
- [x] 3.3 Add the midnight alarm and its receiver: update, then a one-time refresh (D5).

## 4. Previews and verification

- [x] 4.1 Take the picker preview images from the real widgets with sample data, light mode (D8).
- [x] 4.2 Run `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 4.3 On the emulator:
  - add both widgets from the picker;
  - check they match Home, in light and dark, Spanish and English;
  - "+ glass" works online and in airplane mode, and is sent once later;
  - "Log a meal" opens Capture;
  - a change in the app reaches the widgets;
  - narrow and wide sizes;
  - the stale note (old cache);
  - midnight, by setting the clock;
  - removing the last widget cancels the worker.

  Restore the emulator afterwards.
- [ ] 4.4 On the phone, with the user's OK to install: the same main checks.
