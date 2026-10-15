## Why

Saving a meal from Review or adding a glass of water needs the backend at that moment. Without a connection the save fails and the entry is lost, and when the backend does store a meal it stamps it with the time it arrived, not the time the user logged it. The phone's backend runs on a laptop at home, so away from home nothing can be logged. The backend already accepts a client id that makes retries safe (#68), and the app now has a local database (#69).

Tracked as GitHub issue #70: https://github.com/crmartinez930130/food-tracker-ai/issues/70

## What Changes

- **Phone time:** saving from Review sends `occurredAt` with the moment the user tapped Save; a glass logged for today sends `loggedAt` with the tap's time. Both send a `clientId` (UUID) generated once per entry.
- **Send now, queue if offline:** the app still tries to send immediately. If the backend answers with an error (validation, rejected token), the error is shown as today and nothing is queued. If it can't be reached, the entry goes to a queue in the local database and the screen behaves as saved.
- **Pending on Home:** queued meals and glasses appear on Home (and the day's detail) marked "Pending", inside their meal-type group, and count in the day's calories, macros and water. They have no favorite or delete action. Undo on the water card removes the newest pending glass locally when there is one; otherwise it deletes the newest stored glass as today (which needs a connection).
- **Automatic sending:** a WorkManager job sends the queue when the phone has a network, retrying with growing waits while the backend can't be reached. It survives closing the app and restarting the phone. The app also sends the queue whenever it opens or returns to the foreground. Entries are sent oldest first.
- **No duplicates:** a retry carries the same `clientId`, so the backend returns the stored entry instead of creating another (#68). A sent entry moves into the day's cached copy at once, so it never disappears or shows twice.
- **Rejected entries:** if the backend answers a queued entry with an error (for example 400 or 401), it stays on Home marked with the error, isn't retried automatically, and offers "Retry" and "Discard".
- **One server per entry:** each queued entry belongs to the server address it was saved for. It's only shown and sent while that is the current server, and changing the server keeps it.
- **Favorites offline:** Capture keeps a copy of the favorites list, so a favorite can be re-logged without a connection.
- **Reminders:** a meal waiting in the queue counts as logged when a reminder checks today's meals.
- **Fixes from the #73 review:**
  - a cached day or goal that no longer decodes deletes only that row, not both;
  - changing the server deletes only the other servers' cache rows, so a refresh of the new server that lands first is kept.

## Capabilities

### New Capabilities
- `offline-queue`: logging meals and water without a connection, how queued entries are shown, sent, retried and discarded, and what else needs to work offline for that (favorites, reminders).

### Modified Capabilities
- `water-tracking`: "Water on Home" no longer requires the backend for logging a glass or undoing a pending one.

## Impact

- **App (`frontend/composeApp`):**
  - `commonMain`:
    - `clientId` on `SaveMealRequest`, `LogWaterRequest`, `MealEntry` and `WaterEntry`;
    - Room database version 2 (auto-migration) with `outbox` and `favorites_cache` tables;
    - an outbox repository (save-or-queue, send, retry, discard, observe pending);
    - Review, `DayContent` and Capture using it, and new strings in Spanish and English.
  - `androidMain`: WorkManager (`work-runtime`), the sync worker, scheduling it from the app, sending on app open, and the reminder check reading the queue.
- **Backend:** none. It needs the #68 build, already deployed on the PC and the laptop.
- **Dependencies:** `androidx.work:work-runtime` 2.12.0.
