## Why

The user wants Food Tracker on a Galaxy Watch 6 (Wear OS). This first step puts today's progress on the wrist: an app screen and a tile. The goal is also to prove the hardest assumption early: that the watch can reach the home-network backend. Behind the phone it goes through the phone's connection, and alone it uses Wi-Fi.

Tracked as GitHub issue #104: https://github.com/crmartinez930130/food-tracker-ai/issues/104

## What Changes

- **A new Wear OS app module `:wearApp`:**
  - Compose for Wear OS, `minSdk` 30;
  - the same `applicationId` as the phone app, as the Data Layer requires;
  - depends on `:shared` for the API client, local cache and day calculations.
- **Settings come from the phone:**
  - The phone app sends the server address, access token and chosen language to the watch over the Wear OS Data Layer, whenever they're saved and when it opens.
  - The watch stores them locally only, excluded from backups.
  - Nothing is typed on the watch.
- **App screen "Today":**
  - calories against the goal and what's left;
  - macros and water;
  - the "No connection · data from HH:mm" note when the backend can't be reached, showing the watch's own copy of the day.

  Without settings yet, it asks to open Food Tracker on the phone.
- **"Today" tile:** a calories progress ring, the kcal left and the water. A tap opens the app. It refreshes when the app opens, when its data changes and about every 30 minutes.
- **Texts** in Spanish and English, following the language chosen in the phone app.
- **A debug-only receiver,** protected by the `DUMP` permission so only `adb` can use it, to load test settings on an emulator. Pairing emulators needs a Google sign-in, so this is the way to test there.

## Capabilities

### New Capabilities
- `wear-app`: the Wear OS app and its "Today" tile, and getting the backend settings from the phone.

### Modified Capabilities
<!-- None: the phone app only gains a background push of settings it already has. -->

## Impact

- **New module `frontend/wearApp`:**
  - activity, tile service, Data Layer listener and refresh;
  - the watch's cache database builder;
  - strings, manifest, and backup rules.
- **`:shared`:** a pure `WatchSettings` model with its Data Layer map encoding and tests.
- **`composeApp`:**
  - sends the settings to the watch from `MainActivity`, on start and on every settings change;
  - adds the dependency `com.google.android.gms:play-services-wearable`.
- **Build and CI:**
  - `settings.gradle.kts` includes `:wearApp`;
  - the frontend CI job also builds `:wearApp:assembleDebug`.
- **Backend:** no change.
