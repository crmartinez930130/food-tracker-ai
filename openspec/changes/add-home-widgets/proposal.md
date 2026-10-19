## Why

To see how the day is going, or to log a glass of water, the user has to open the app. A home-screen widget answers at a glance or with one tap. The app already has what that needs:
- a local copy of the day that feeds Home even offline (#69);
- an offline queue that sends water by itself (#70);
- a way to open the capture screen directly, which the reminders use.

Tracked as GitHub issue #90: https://github.com/crmartinez930130/food-tracker-ai/issues/90

## What Changes

- **"Today's calories" widget** (read-only):
  - today's calories against the goal, with a progress bar and the kcal left;
  - protein, fat and carbs;
  - water against its target.

  Tapping it opens Home. When its data is old, a short note says from what time it is.
- **"Quick actions" widget:**
  - **"+ glass"** logs one glass of the size set in Goals. It shows up at once in both widgets and in the app, and goes through the offline queue, so it works without a network.
  - **"Log a meal"** opens the capture screen.
- **Updates:**
  - whenever the local copy of today or the queue changes, whatever changed it (the app, the background sender, the widget itself);
  - at midnight;
  - every 30 minutes in the background with a network, to pick up what was logged elsewhere. This only runs while a widget is placed.
- **Look:**
  - the app's green scheme, following the system's light or dark mode;
  - texts in the language chosen in the app;
  - a preview image in the widget picker.
- Built with Jetpack Glance, Android only.

## Capabilities

### New Capabilities
- `home-widgets`: home-screen widgets that show today's progress and offer quick actions.

### Modified Capabilities
<!-- None: the widgets reuse offline-cache, offline-queue and water-tracking as they are. -->

## Impact

- **Android (`androidMain`):**
  - two Glance widgets and their receivers;
  - `appwidget-provider` XML and preview images;
  - a refresh worker and a midnight alarm;
  - a watcher on the cache database that redraws the widgets;
  - manifest entries.
- **Common:**
  - a pure `TodayWidgetState` built from the cached day, the queued rows and the goal, with tests;
  - a few strings in both languages.
  - `storedSettingsOutbox` gains an optional `requestSync`, so the widget's glass schedules a send.
- **Dependencies:** `androidx.glance:glance-appwidget` and `glance-material3`.
- **Backend:** no change.
