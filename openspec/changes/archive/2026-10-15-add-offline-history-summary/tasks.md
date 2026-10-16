## 1. Home without a cached day

- [x] 1.1 Add `DEFAULT_GOAL`, make `CachedDay.refreshedAt` nullable and add `DayRepository.offlinePlaceholder(date)` (cached goal of the current server, or the default) (D1). Verify with `DayRepositoryTest`: the placeholder has no meals, no water, the cached goal when there is one and the default otherwise, and nothing is written to the cache.
- [x] 1.2 `DayContent`: show the placeholder when the day has no copy, the cache was read and the refresh ended `Offline`, with the "No connection" notice without a time; a `Failed` refresh without a copy keeps the error alone (D1). Move the notice composable to `ui/OfflineNotice.kt` and add the strings in Spanish and English. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug` and `StringResourcesTest`.

## 2. Snapshot cache

- [x] 2.1 Add the `snapshot_cache` entity and DAO, bump the database to version 4 with an auto-migration from 3 and commit `4.json`; add `SnapshotStore`, `SnapshotRepository` (`cached`, `refresh`, `deleteOtherServers`) and wire the cleanup into `MainActivity` (D2, D3, D5). Verify with `SnapshotRepositoryTest` on a fake store and `MockEngine`: a refresh stores and `cached` returns it with its time; no answer returns `Offline` and keeps the copy; an error status returns `Failed` and keeps the copy; another server's rows aren't returned; a row that doesn't decode is deleted; a refresh whose server changed meanwhile isn't stored; `deleteOtherServers` keeps only the current server's rows.

## 3. Screens

- [x] 3.1 History: cached month first, refresh, notices and errors as in D4. Verify with the build and on the emulator (4.1).
- [x] 3.2 Summary: the calorie period and the weight period each cached and refreshed as in D4; weight logging and deleting refresh the weight key on success. Verify with the build and on the emulator (4.1).

## 4. Verification

- [x] 4.1 On the emulator, installed over the current build (database version 3), with the backend on the PC over a copy of the real database, in Spanish and English:
  - the app opens after the migration;
  - with the backend stopped and today never loaded, Home shows today empty with the goal and "No connection", a kept capture and a queued glass show, and adding water works;
  - with the backend up, History, a past day's detail and Summary (week and month, with weight) load; with it stopped, they show the copies with "No connection · data from …", a never-seen month or week shows the not-loaded-yet notice, and logging weight shows its error;
  - with the backend on another token, History and Summary show the rejected-token message above the copy;
  - with the backend back, everything refreshes on its own.

  Restore the app's settings afterwards.
