## 1. Dependencies

- [x] 1.1 Add KSP 2.3.12, the Room 3 Gradle plugin, `room3-runtime` 3.0.3, `room3-compiler` (KSP for Android and the iOS targets) and `sqlite-bundled` 2.7.1 to `libs.versions.toml` and `composeApp/build.gradle.kts`, with `schemaDirectory("$projectDir/schemas")` (D1). Verify with a throwaway one-entity database that `./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest` builds with Kotlin 2.4.20 and AGP 8.13.2. If it doesn't, switch to Room 2.8.x and update design D1 before continuing.

## 2. Database and repository

- [x] 2.1 Add the `day_cache` and `goal_cache` entities, their DAO, the database class (version 1, `@ConstructedBy`) and the exported schema under `composeApp/schemas/` (D2). Verify that the build generates and commits the version 1 schema JSON.
- [x] 2.2 Add `DayCacheStore`, `CachedDay`, `RefreshResult` and `DayRepository` (`observe`, `refresh`, `clear`), with the server-URL filter and the dropped result when the URL changes mid-refresh (D3, D4, D7). Verify with `commonTest` tests on a fake store and `MockEngine`:
  - a refresh stores the day and the goal, and `observe` emits them with `refreshedAt`;
  - a failed connection returns `Offline` and keeps the cached copy;
  - a 401 or 500 returns `Failed` with an `ApiException` and keeps the cached copy;
  - rows from another server URL aren't emitted;
  - a refresh whose server URL changed while it ran doesn't write;
  - a row that doesn't decode is treated as missing;
  - `clear` empties both tables.

## 3. HTTP client

- [x] 3.1 Install `HttpTimeout` with only `connectTimeoutMillis = 10_000` in `createHttpClient` (D6). Verify with a `FoodTrackerApiClientTest` case that the connect timeout is set and the request timeout isn't, and that existing client tests still pass.

## 4. Screens

- [x] 4.1 Make `DayContent` read from `DayRepository`: show the cached day without dimming, keep the current dim and first-load behaviour only when the day isn't cached, refresh on `(date, reloadKey)` and on resume (skipping the first), and route the water refetch through `refresh` (D5). Pass the repository through `HomeScreen`, `DayDetailScreen`, `MainScreen` and `App`. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 4.2 Add the offline notice (with time, plus date when not today) and the server-error-above-content state, with strings in Spanish and English (D5). Verify with a unit test for the notice's text formatting (today vs another day) and with `StringResourcesTest` (both languages have every key).

## 5. Wiring

- [x] 5.1 Build the database once in `androidMain` from the application context and pass the repository from `MainActivity` to `App`; clear the cache in `onPreferencesChange` when the effective server URL changes, not when only the token changes (D7, D8). Add the iOS builder (unverified, D8). Verify with `./gradlew :composeApp:assembleDebug` and on the emulator (6.1).

## 6. Verification

- [x] 6.1 On the emulator, with the backend on the PC, in Spanish and English and in light and dark:
  - Home and a past day's detail open instantly from the cache on the second visit, and pick up a meal added with `curl`;
  - with the backend stopped, Home shows the cached day and the "No connection · data from <time>" notice within about 10 s, a never-visited day shows the error alone, and the notice goes away after starting the backend and returning to the app;
  - with a wrong token, the 401 message appears above the cached day;
  - changing the server address empties the cache, and restoring it reloads;
  - `adb shell bmgr` confirms the app isn't backed up, and the database file is under the excluded `databases/` directory.

  Restore the app's server address and token afterwards.
