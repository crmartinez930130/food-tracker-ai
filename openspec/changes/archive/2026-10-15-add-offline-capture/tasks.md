## 1. Storage

- [x] 1.1 Add the `capture_queue` entity and DAO, bump the database to version 3 with an auto-migration from 2, and commit `3.json` (D2). Add the `CapturePhotoStore` interface with its Android implementation under `files/pending-captures/` and the iOS one (unverified). Verify that the build generates the version 3 schema and the auto-migration.

## 2. Capture queue

- [x] 2.1 Add `CaptureQueueRepository`: `analyzeOrKeep`, `analyzeWaiting`, `observeDay`, `retry`, `discard`, `complete`, with the mutex, server scoping and status classification (D1, D3, D8). Verify with `commonTest` tests on fake stores and `MockEngine`:
  - analyze-or-keep per outcome: an answer is returned and nothing kept; no answer, 500 and 429 keep the capture with its photo file, description, portion, language and time, and request a run; 400 is rejected and keeps nothing;
  - `analyzeWaiting` analyzes oldest first, stops at the first no-answer, marks refusals failed and goes on, and turns answered rows ready with their analysis;
  - a missing photo file fails the row;
  - `retry` re-analyzes a failed row; `discard` and `complete` delete the row and its photo;
  - other servers' rows are neither observed nor analyzed.

## 3. Background and app start

- [x] 3.1 Run `analyzeWaiting` after `sendAll` in the worker and in `MainActivity.onStart`; the worker retries if either stopped offline (D4). Verify with `OutboxSyncTest` (result for each combination) and a test of the stored-settings capture queue (stored server, token and language).
- [x] 3.2 Post the "ready to review" notification from the worker when captures became ready, on its own channel, only if notifications are enabled; tapping it opens Home (D5). Add the strings in Spanish and English. Verify with a unit test of the decision (only when ready > 0) and on the emulator (5.1).

## 4. Screens

- [x] 4.1 Capture: analyze through `analyzeOrKeep`; `Kept` returns to Home, `Rejected` shows the error as today (D1). Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 4.2 Home and the day detail: capture cards with thumbnail or description, time, state and actions (discard; retry when failed; review when ready), not counted in totals (D6). Add the strings in Spanish and English. Verify with `StringResourcesTest` and on the emulator (5.1).
- [x] 4.3 Review from a ready capture: `AppState` carries the capture, Review uses its time and `clientId`, and completes it after `Saved` or `Queued`; cancel keeps it (D7). Verify with `AppStateTest` (opening and leaving Review with a capture) and on the emulator (5.1).

## 5. Verification

- [x] 5.1 On the emulator, installed over the current build (database version 2), with the backend on the PC over a copy of the real database, in Spanish and English and in light and dark:
  - the app opens after the migration with its data;
  - with the backend stopped: a photo capture, a description-only capture with a large portion, and a photo with description show on Home as waiting, with thumbnail or text and time, outside the totals; discarding one deletes its photo file;
  - with the backend up, opening the app analyzes them (one real analysis per capture), the cards become ready, and no meal is saved;
  - with the app's process killed and notifications allowed, the worker analyzes a capture and posts the notification; tapping it opens Home;
  - reviewing a ready capture and saving stores the meal with the capture's time, removes the card and the photo; leaving Review without saving keeps the card;
  - with the backend on another token, a capture fails with the 401 message; retry with the right token makes it ready.

  Restore the app's settings and the notification permission afterwards.
