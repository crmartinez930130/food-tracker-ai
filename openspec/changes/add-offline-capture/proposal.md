## Why

Logging a new meal needs an analysis, and the analysis needs the backend and Claude. Away from home the phone can't reach the backend, so a meal that wasn't analyzed before (a favorite) can't be logged at all, and the photo or description is lost. #70 queues meals that are already analyzed; this change queues the capture itself and analyzes it when the connection comes back, keeping the review step.

Tracked as GitHub issue #71: https://github.com/crmartinez930130/food-tracker-ai/issues/71

## What Changes

- **Capture without a connection:** when Analyze gets no answer (or a retryable status: 5xx, 408, 429), Capture keeps what the analysis takes and returns to Home:
  - the photo, if any, as a file in the app's private storage;
  - the description, the portion and the app's language;
  - the tap's time and a client id.

  A refusal (4xx) is shown as today.
- **Pending captures on Home:** each queued capture appears on its day as a card with its photo thumbnail or description and the time. It doesn't count in the totals until it is reviewed and saved. Its state is one of:
  - "Waiting for analysis";
  - "Ready to review";
  - "Couldn't analyze: <error>".

  Every card can be discarded, which deletes the photo too. A failed card can be retried.
- **Analysis when the connection is back:** the same moments that send the #70 queue (opening or returning to the app, and the WorkManager job) analyze waiting captures, one at a time and oldest first, and stop at the first one without an answer. A result is kept as "ready to review"; **nothing is saved on its own**. A refusal marks the capture as failed and it isn't retried automatically.
- **Review a ready capture:** "Review" opens Review with the analysis. Saving uses the capture's time as the meal's time, sends or queues it like any save (#70), and removes the capture and its photo. Leaving Review without saving keeps the card.
- **Notification:** when the background job finishes analyses, it posts "You have meals ready to review" (Spanish and English), if the app may post notifications. Tapping it opens Home.
- **Server scoping:** like the #70 queue, a capture belongs to the server it was taken for.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `offline-queue`: new requirements for captures taken without a connection: queueing, how they show on Home, analysis when the connection is back, review with the original time, discarding and retrying, and the notification.

## Impact

- **App (`frontend/composeApp`):**
  - `commonMain`:
    - Room database version 3 (auto-migration from 2) with a `capture_queue` table;
    - a `CapturePhotoStore` interface for the photo files;
    - `CaptureQueueRepository` (queue, analyze waiting captures, retry, discard, complete, observe a day);
    - Capture queueing on no answer, the capture cards on `DayContent`, Review taking the capture's time, `AppState` carrying the capture being reviewed, and strings in Spanish and English.
  - `androidMain`: the photo files under the app's `files/` directory (already excluded from backups), the worker and `onStart` also analyzing, and the "ready to review" notification with its channel.
  - `iosMain`: the photo store in the documents directory (unverified, as before).
- **Backend:** none.
- **Dependencies:** none.
