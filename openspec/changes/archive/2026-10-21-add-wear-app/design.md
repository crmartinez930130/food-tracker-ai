## Context

`:shared` (#95) already holds what the watch needs:
- the API client and `storedSettingsDayRepository`;
- the Room cache (`CacheDatabase`, built per platform);
- `todayWidgetStates`, which yields what the phone widget shows.

The Galaxy Watch 6 runs Wear OS 6 (API 36), so APIs from 30 up are safe.

The Data Layer only connects apps with the same package name and signing key, per the platform docs. Debug builds from this machine share the debug key.

## Goals / Non-Goals

**Goals:**
- Today's numbers on the watch, matching the phone.
- Zero setup on the watch.
- Learning, on the real watch, which network paths reach the backend.

**Non-Goals:**
- Complications, logging water, and voice logging (later issues).
- Play Store distribution.
- Standalone use without ever pairing a phone.

## Decisions

### D1: Module and package
`frontend/wearApp` is an Android application module:
- AGP application, Kotlin Android and the Compose compiler;
- `applicationId com.foodtracker.app`, the same as the phone app;
- `namespace com.foodtracker.wear`;
- `minSdk` 30, target 36.

Its dependencies:
- `:shared`;
- `androidx.wear.compose` material3 1.6.2 (1.7.1 requires compileSdk 37 and AGP 9.1, which the project doesn't use yet);
- `activity-compose`;
- `androidx.wear.tiles` 1.6.2 with `protolayout` and `protolayout-material3`;
- `play-services-wearable` 20.0.1;
- `kotlinx-coroutines-play-services`, for `Task.await()`;
- `work-runtime`.

### D2: Settings over the Data Layer
- **Model:** a pure `WatchSettings(serverUrl, accessToken?, language)` in `:shared`, with `toMap()` and `fromMap()` over plain strings, tested.
- **Phone:** `WearSettingsSync.push(context, preferences)` in `composeApp/androidMain` puts an urgent `DataItem` at `/settings`. `MainActivity` calls it on create and in `onPreferencesChange`. Failures, such as no paired watch or no Play services, are ignored.
- **Watch:** a `WearableListenerService` filtered to `/settings` receives it and saves it in `WatchSettingsStore` (SharedPreferences, excluded from backups). It then refreshes and asks the tile to update.

  On launch, the app also reads the current items with `DataClient.dataItems`, in case an event was missed.
- **Security:** the Data Layer only connects to the same app, with the same package and signature, over an encrypted channel. The token never leaves the user's own devices.

### D3: Data on the watch
- **Storage:** the watch has its own copy of the cache, `CacheDatabase` built in `wearApp` with the same name.
- **Reading:** both the app screen and the tile read `todayWidgetStates(...)` from `:shared`, so they show what the phone widget shows, with the same 60-minute staleness rule.
- **Refreshing:** `refreshToday(context)` uses `storedSettingsDayRepository` with the watch's settings and closes the client.
- **When it refreshes:**
  - the app refreshes on open;
  - every tile request enqueues a unique `OneTimeWork` refresh (`KEEP`, with a network constraint);
  - a finished refresh calls `TileService.getUpdater(context).requestUpdate(...)`.

### D4: The tile
`TodayTileService` builds a `protolayout-material3` layout from the latest state:
- a circular progress ring for calories;
- the calories in the center;
- "1132 left" and "Water 750/2500 ml" below;
- the stale note as a small line when the copy is old.

`freshnessIntervalMillis` is 30 minutes. Tapping the tile launches the app's activity. Without settings, the tile shows a short "Open Food Tracker on your phone".

### D5: The app screen
It's one scrollable Compose for Wear OS screen: an `AppScaffold` and `ScreenScaffold` with a `TransformingLazyColumn`, containing:
- the calories with a progress indicator;
- the left/over line;
- protein, fat and carbs;
- water;
- the stale or no-settings note.

It collects `todayWidgetStates` and refreshes on start.

### D6: Language
The phone sends its resolved language ("es" or "en"). The watch wraps its contexts with `createConfigurationContext(locale)`:
- the activity, in `attachBaseContext`;
- the tile service, when building text.

Strings are Android resources in `wearApp`, with `values` (es) and `values-en`. When no setting has arrived yet, the watch's own language applies.

### D7: Cleartext and backups
The watch talks to `http://` on the home network, as the phone does: `usesCleartextTraffic="true"`, documented in the README "Security" section. `allowBackup="false"` and data extraction rules exclude everything, as on the phone (#61).

### D8: Emulator testing
`DebugSettingsReceiver` lives in the `debug` source set only, so it isn't in release builds. It's exported, but protected by `android:permission="android.permission.DUMP"`, which only the shell and the system hold. `adb shell am broadcast` passes a test server and token to it, written into `WatchSettingsStore` exactly like a Data Layer event.

The real Data Layer path is verified on the user's phone and Galaxy Watch 6.

## Risks / Trade-offs

- **[The watch can't reach a LAN address through the phone's Bluetooth proxy]** → Measured on the user's Galaxy Watch6 (Wear OS 6): it reaches the laptop's backend both over Wi-Fi and, with Wi-Fi off, through the phone's Bluetooth proxy. No relay through the phone is needed.
- **[Two copies of the cache, phone and watch]** → Each refreshes from the backend, so they agree once refreshed. The watch's copy is only today's day and goal.
- **[Play services dependency on the phone app]** → `play-services-wearable` is small, and `push` fails silently where there's no watch or no Play services.
