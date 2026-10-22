## 1. Shared and phone

- [x] 1.1 Add `WatchSettings` with `toMap`/`fromMap` to `:shared`, with tests (D2).
- [x] 1.2 Add `WearSettingsSync.push` in `composeApp/androidMain`, using `play-services-wearable`. Call it from `MainActivity` on create and on every settings change, and ignore failures (D2).

## 2. Watch app

- [x] 2.1 Create the `:wearApp` module (D1):
  - manifest with cleartext, backup exclusions and the standalone=false meta-data (D7);
  - include it in `settings.gradle.kts`;
  - add the libraries to the version catalog.
- [x] 2.2 Add `WatchSettingsStore`, the Data Layer listener service, the initial read of data items, and the debug-only receiver (D2, D8).
- [x] 2.3 Add the watch's cache database, `refreshToday`, and the one-time refresh work that updates the tile (D3).
- [x] 2.4 Add the app screen (D5), and the language wrapper with strings in Spanish and English (D6).
- [x] 2.5 Add the "Today" tile (D4).

## 3. CI and docs

- [x] 3.1 Build `:wearApp:assembleDebug` in CI. Add a README section on installing the watch app, with wireless debugging, pairing and settings from the phone. Note the watch's cleartext under "Security".

## 4. Verification

- [x] 4.1 Run `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest :composeApp:assembleDebug :wearApp:assembleDebug`.
- [x] 4.2 On a Wear OS 6 emulator, with the debug receiver and a demo backend:
  - the app screen and the tile match the data;
  - the no-settings message;
  - the stale note offline;
  - Spanish and English.
- [x] 4.3 On the user's phone and Galaxy Watch 6, with the user's OK to install:
  - opening the phone app configures the watch;
  - the app screen and tile show today's data;
  - document whether the backend is reached through the phone, over Wi-Fi, or both.
