## 1. Fixes from the #73 review

- [x] 1.1 Decode the cached day and the goal separately in `DayRepository.observe` and delete only the row that fails; replace the server-change `clear()` with `deleteOtherServers(newUrl)` in the store and `MainActivity` (D10). Verify with `DayRepositoryTest`: a day that doesn't decode keeps the goal row and vice versa; `deleteOtherServers` keeps the current server's rows and removes the rest.

## 2. Models and database

- [x] 2.1 Add `clientId` to `SaveMealRequest`, `LogWaterRequest`, `MealEntry` and `WaterEntry` (D1). Verify with `FoodTrackerApiClientTest`: requests send `clientId`, and responses with and without it decode.
- [x] 2.2 Add the `outbox` and `favorites_cache` entities and DAOs, bump the database to version 2 with an auto-migration from 1, and commit the exported `2.json` (D3). Verify that the build generates the version 2 schema and the auto-migration.

## 3. Outbox

- [x] 3.1 Add `OutboxRepository` with `saveMeal`, `logWater`, `undoPendingWater`, `observePending`, `sendAll`, `retry` and `discard`, the process-wide mutex, merge-then-delete into the day cache, server scoping and the status classification (D2, D4, D7). Verify with `commonTest` tests on fake stores and `MockEngine`:
  - send-or-queue per outcome: 201 → saved; no answer, 500, 408 and 429 → queued and sync requested; 400 and 401 → rejected and not queued;
  - `sendAll` sends oldest first, stops at the first no-answer, marks 4xx rows rejected and keeps going, and merges each sent entry into the cached day before deleting its row;
  - a meal whose `clientId` is already in the cached day isn't appended twice;
  - `retry` clears the error and sends; `discard` deletes;
  - other servers' rows are neither observed nor sent;
  - `undoPendingWater` removes the day's newest pending glass and reports when there is none.

## 4. Screens

- [x] 4.1 Review: save through `OutboxRepository.saveMeal` with `occurredAt` = the Save tap and a new `clientId`; `Queued` closes like `Saved`, `Rejected` shows the error (D1, D2). Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 4.2 `DayContent`: merge pending meals and glasses into the shown day with the pending label and without favorite or delete actions, list rejected rows with "Retry" / "Discard", log water through `OutboxRepository.logWater` with the tap's time for today, and undo a pending glass locally (D5). Add the strings in Spanish and English. Verify with a unit test for the pure merge (pending added and counted, rejected not counted, duplicates by `clientId` dropped) and `StringResourcesTest`.
- [x] 4.3 Capture: show the cached favorites first, replace them on a successful load, and keep showing them without the error when the load fails (D8). Verify with a repository test for the favorites cache (stored per server, replaced on load) and on the emulator (6.1).

## 5. Background sending and reminders

- [x] 5.1 Add `work-runtime`, the `CoroutineWorker`, unique-work scheduling with network constraint and backoff, a `requestSync` hook used on `Queued`, and `sendAll` on `MainActivity.onStart` (D6). Verify with unit tests: `OutboxSyncTest` (the worker's result for offline and done runs, and the request's network constraint, exponential 30 s backoff and worker class) and an `OutboxRepositoryTest` case for `storedSettingsOutbox` (the worker sends to the stored server with the stored token); the worker running inside WorkManager on the emulator (6.1).
- [x] 5.2 Make the reminder check count today's pending meals of the reminder's type, also when the API check fails (D9). Verify with a unit test of the decision function (pending lunch → no notification, with and without API meals).

## 6. Verification

- [x] 6.1 On the emulator, installed over the current build (database version 1), with the backend on the PC over a copy of the real database, in Spanish and English and in light and dark:
  - the app opens with its cached data after the migration;
  - with the backend stopped: a meal saved from Review (an analysis done before stopping, or a favorite from the cached list) and two glasses show as pending and count; undo removes a pending glass;
  - after killing the app's process (`am kill`, not force-stop) and starting the backend, WorkManager sends the queue without opening the app (forced with `cmd jobscheduler run` if the backoff is long); the stored meal has the save's time, not the send's; Home then shows it once, as stored;
  - opening the app with queued entries and the backend up sends them at once;
  - with the backend on another token, a queued meal is rejected with the 401 message and isn't retried; after the right token, retry stores it; discard removes another;
  - a lunch queued for today stops the lunch reminder;
  - switching server hides the queued entries and switching back shows and sends them.

  Restore the app's settings afterwards.
