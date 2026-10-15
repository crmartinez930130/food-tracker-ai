# Design

## Context

- Capture calls `apiClient.analyzeMeal(photo, description, portion, language)` and passes the `MealAnalysis` to `AppState.openReview(analysis)`. An error is shown under the analyze button. A favorite reaches Review the same way, without analysis.
- Review saves through `OutboxRepository.saveMeal` with `occurredAt` = the Save tap (#70).
- The local database is Room 3, version 2 (`day_cache`, `goal_cache`, `outbox`, `favorites_cache`), with exported schemas and auto-migrations.
- Sending the #70 queue happens in `MainActivity.onStart` and in `OutboxSyncWorker`, whose testable parts are `storedSettingsOutbox`, `OutboxSync.resultFor` and `OutboxSync.syncRequest`.
- The backup rules exclude the `file` and `database` domains.
- Notifications exist for reminders (`ReminderNotifications`, its own channel). The permission is requested only from the Reminders screen.
- The analysis endpoint is rate-limited to 20 calls per minute by default and answers 429 past it.

## Goals / Non-Goals

**Goals:**
- A photo or description taken without a connection is never lost, and becomes a reviewed meal with the time it was taken.
- Analyses happen without the user's help, but saving always goes through Review.

**Non-Goals:**
- Analyzing on the phone.
- Saving an analysis without review.
- Asking for the notification permission from the capture flow; without it there's no notification and the card on Home is enough.
- Editing a kept capture's description, photo or portion before it is analyzed.

## Decisions

1. **When a capture is kept.** `CaptureQueueRepository.analyzeOrKeep(photo, description, portion, language)` calls the API:
   - an answer → `Analyzed(analysis)`, and Capture opens Review as today;
   - no answer, or 5xx, 408 or 429 → keep it and request a background run → `Kept`, and Capture returns to Home;
   - any other status → `Rejected(error)`, shown in Capture as today.

   The kept time is the tap's (`currentInstantIso()`).

2. **Storage.**
   - **Room version 3** (auto-migration from 2) adds `capture_queue(id, client_id UNIQUE, day, server_url, captured_at, description, portion, language, photo_file, status, analysis_json, error, error_status, created_at)`. `status` is `WAITING`, `READY` or `FAILED`; `day` is the local day of `captured_at`.
   - **Photos** are files, not blobs: a camera photo can be several MB, beyond Android's cursor window. `CapturePhotoStore` (`write(name, bytes)`, `read(name)`, `delete(name)`) is an interface in `commonMain`. Android stores them in `files/pending-captures/<clientId>.jpg`, under the backup-excluded `file` domain; iOS uses its documents directory (unverified). Tests use an in-memory fake.

3. **Analysing (`analyzeWaiting()`)** reads the current server's `WAITING` rows, oldest first, under a process-wide mutex. For each row:
   - no photo file when there should be one → `FAILED` with a local message;
   - analysis answer → `READY` with `analysis_json`;
   - no answer or a retryable status → stop and report offline;
   - any other status → `FAILED` with the error.

   One at a time keeps well under the rate limit. It returns how many became ready and whether it stopped offline. `retry(id)` puts a failed row back to `WAITING` and runs `analyzeWaiting()`; `discard(id)` and `complete(id)` delete the row and its photo.

4. **One background job for both queues.** The #70 worker becomes the offline sync job: `sendAll()` then `analyzeWaiting()`. The result is `retry` if either stopped offline. `OutboxSync.resultFor` takes both reports, and `storedSettingsOutbox` gets a sibling for the capture queue. `MainActivity.onStart` runs both too. A kept capture requests the same unique work. Both sides stay unit-testable as in #70.

5. **Notification.** Only the worker posts it, when `analyzeWaiting()` made at least one capture ready. In the foreground the user sees the cards. It uses its own channel ("Meals to review") and a fixed id, so a new one replaces the previous one. It is posted only when notifications are enabled for the app, and its tap opens `MainActivity` (Home).

6. **Home.** `DayContent` observes the day's captures and lists them above the rejected entries and the meals as cards:
   - a thumbnail decoded from the file with the existing `decodePreviewBitmap`, or the description when there's no photo;
   - the time;
   - the state, with the actions that state allows.

   Captures aren't part of `mergePending` and never count in the totals.

7. **Review.** `AppState.openReview(analysis, capture = null)` carries a `CaptureRef(id, capturedAt)` when opened from a card. Review then uses `capturedAt` as `occurredAt`, suggests the meal type for that hour instead of the current one (a lunch photo reviewed at night is still lunch), and, after `Saved` or `Queued`, calls `complete(id)`. Cancelling keeps the capture. The capture's `clientId` is reused as the meal's `clientId`, so even a save repeated around a crash stores one meal.

8. **Server scoping** as in #70: rows carry `server_url`, and only the current server's rows are observed and analyzed. A server change keeps them.

## Risks / Trade-offs

- **Many kept photos fill storage.** → Each one is deleted when saved or discarded. Captures that are never reviewed stay, visible on Home, until the user discards them.
- **An analysis can cost money and then be discarded.** → Same as discarding Review online today.
- **A reviewed capture whose save is queued (#70)** leaves Home as a capture and appears as a pending meal, with the original time; the photo is deleted then, since the queued meal no longer needs it.
- **iOS photo store and notification** aren't built here; the notification is Android-only.
- **Seen during verification, not caused by this change:** after the in-app camera has been opened, going back to Home shows the app in the phone's language instead of the one chosen in Options, until the app restarts. It happens with or without a connection and without this change's code involved; tracked separately.
