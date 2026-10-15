# Design

## Context

- Review saves with `apiClient.saveMeal(SaveMealRequest(items, notes, mealType))`, without `occurredAt`, so the backend stamps the arrival time. A favorite is re-logged by opening Review pre-filled (`meal.toReviewAnalysis()`); the favorites list in Capture comes from `listFavorites()` every time.
- `DayContent` (Home and the day detail) shows the day from `DayRepository` (#69): the cached day first, then the backend's. Water add/undo call the API, then `refresh(day)`. A glass for today is sent with `loggedAt = null` (the backend's now); for a past day, noon of that day.
- The local database (`CacheDatabase`, Room 3, version 1) has `day_cache` and `goal_cache`; its schema is exported to `composeApp/schemas/`.
- The backend accepts `clientId` on `POST /api/meals` and `POST /api/water` and answers 200 with the stored entry on a repeat (#68). Its responses include `clientId`.
- `ReminderReceiver` checks today's meals through the API in a fresh process, building its own client from the stored preferences.
- `MainActivity` clears the whole cache when the server address changes (#69); the #73 review pointed out that a refresh of the new server could land before that clear runs and be wiped.

## Goals / Non-Goals

**Goals:**
- No meal or glass logged by the user is lost for lack of a connection, and each is stored once, with the time it was logged.
- What the user sees on Home always includes what they logged, pending or not, without flicker or duplicates.
- Sending happens without the user's help, including with the app closed.

**Non-Goals:**
- Analyzing photos or descriptions offline (#71).
- Editing, deleting, or favoriting stored meals offline; changing goals or weight offline.
- Showing pending entries on a day that has never been loaded (no cached copy): that day shows the load error as today.

## Decisions

1. **Client ids and phone time.** `SaveMealRequest` and `LogWaterRequest` get `clientId`; `MealEntry` and `WaterEntry` get `clientId: String? = null` to read it back. Ids come from `kotlin.uuid.Uuid.random()`. Review sets `occurredAt` to the Save tap's instant; a glass for today sets `loggedAt` to the tap's instant (a past day keeps noon).

2. **Send first, queue on no answer.** A new `OutboxRepository.saveMeal(request)` / `logWater(...)` posts at once:
   - stored → `Saved(entry)`;
   - no answer (any non-`ApiException`) or a retryable status (5xx, 408, 429) → write it to the outbox and request a background sync → `Queued`;
   - any other error status → `Rejected(ApiException)`. Review and the water card show it as today, and nothing is queued.

   Online behaviour therefore doesn't change, and validation errors stay where the user can fix them. *Alternative:* always queue and let the sender send. It's simpler, but every online save would flash "pending" and a 400 would surface on Home instead of in Review.

3. **Database version 2** with a Room auto-migration from 1:
   - `outbox(id INTEGER PK AUTOINCREMENT, client_id TEXT UNIQUE, kind TEXT, day TEXT, server_url TEXT, payload_json TEXT, created_at INTEGER, error TEXT NULL, error_status INTEGER NULL)`. `kind` is `MEAL` or `WATER`, `day` is the local day the entry counts for, and `payload_json` is the full request as it will be sent, `clientId` and time included. An entry with `error` set is rejected.
   - `favorites_cache(id INTEGER PK, favorites_json TEXT, server_url TEXT, refreshed_at INTEGER)`, a single row like `goal_cache`.

4. **Sending (`OutboxRepository.sendAll`)**, guarded by one process-wide `Mutex` so the app and the worker never send the same row at once (and a double send would be harmless anyway, thanks to the `clientId`). It reads the current server's non-rejected rows, oldest first. For each row:
   - stored (201 or 200) → merge the returned entry into that day's cached copy, if the day is cached for the same server, then delete the row. The meal is appended to `meals_json` unless its `clientId` is already there; the water entry is appended to `water_json` and `totalMl` grows. Writing the cache before deleting means the entry is always visible, and the `clientId` check in the display (D5) hides the moment both exist;
   - no answer or a retryable status → stop and report `Offline`, keeping the rest;
   - any other status → set `error`/`error_status` and continue with the next row.

   It returns how many rows were sent and whether it stopped offline. `retry(id)` clears the error and runs `sendAll`; `discard(id)` deletes the row.

5. **Display.** `observePending(day)` emits the current server's rows for that day. `DayContent` merges them with the cached day:
   - pending meals (not rejected, and whose `clientId` isn't among the cached meals) are added to their meal-type group as `MealEntry`s marked pending: a "Pending" label and no favorite or delete action. They count in the totals;
   - pending glasses (same `clientId` check against the cached water entries) are added to the water total and the undo logic;
   - rejected rows are listed above the meals with the localized error (`errorText`, so 401 gets its own message) and "Retry" / "Discard" buttons. They don't count in the totals.

   Undo removes the newest pending glass of the day locally if there is one; otherwise it deletes the newest stored glass through the API, as today.

6. **When it sends:**
   - **Background:** an Android `CoroutineWorker` scheduled as unique work `outbox-sync` (`ExistingWorkPolicy.KEEP`), with `NetworkType.CONNECTED` and exponential backoff from 30 s (WorkManager caps it at 5 h). It builds its own client from the stored preferences, like the reminder receiver. It returns `Result.retry()` when `sendAll` stopped offline and `success()` otherwise. WorkManager persists it across process death and reboots. A force-stop from Settings suspends it until the app is opened again; that's Android's rule.
   - **App:** `MainActivity.onStart` runs `sendAll` in `lifecycleScope`, and schedules the worker if rows remain. `App` triggers Home's refresh afterwards through the existing resume refresh.
   - Every `Queued` result also schedules the worker.

7. **Server scoping:** rows are written with the current server URL; `observePending`, `sendAll` and the reminder check only use rows of the current server. A server change keeps them.

8. **Favorites cache:** Capture shows the cached list first, if any for the current server, then loads; a successful load replaces the copy. A failed load with a cached copy shows the copy without the error; without a copy, the error as today. Removing a favorite still needs a connection. A server change deletes the other servers' copy, together with the day cache (D10).

9. **Reminders:** the receiver also reads today's pending meal rows for the stored server. A pending meal of the reminder's type counts as logged, even when the API check fails.

10. **Fixes from the #73 review:**
    - `DayRepository.observe` decodes the day and the goal separately and deletes only the row that fails;
    - the server-change clear becomes `deleteOtherServers(newUrl)` (`DELETE … WHERE server_url != :url` on both cache tables), so a refresh of the new server that lands first survives. Queued rows aren't touched.

11. **Testing:**
    - `commonTest` covers `OutboxRepository` with fake stores and `MockEngine`: send-or-queue outcomes per status, sending order, stopping when offline, merge-then-delete, rejection without retry, retry and discard, server scoping, the `clientId` deduplication helper, and the per-row decode fix.
    - The worker, the migration and the reminder are exercised on the emulator. The migration is checked by installing over the current build, which has a version 1 database.

## Risks / Trade-offs

- **A pending meal is bucketed by the phone's local day,** while the backend uses its own timezone. → Same assumption as the rest of the app (single user, same timezone); a mismatch would only move the meal to another day after it is sent.
- **Rows for another server wait indefinitely** if the user never switches back. → Visible only on that server; acceptable for a single-user app with two known servers. They could be listed in Options later.
- **WorkManager backoff can reach hours** after many failures away from home. → The app sends on every open, so returning home and opening the app sends at once.
- **A meal sent while its day isn't cached** isn't merged; it shows after the next refresh of that day, which happens when the day is opened.
