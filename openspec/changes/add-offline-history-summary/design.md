# Design

## Context

- `DayContent` shows the cached day (`DayRepository.observe`) and refreshes it. Without a copy and with a failed refresh, it shows only the error, and the queued entries and captures, which are rendered inside the day's list, aren't shown either.
- `DayRepository` caches days and the goal in `day_cache` and `goal_cache`. `RefreshResult` is `Success`, `Offline(cause)` or `Failed(ApiException)`.
- `HistoryScreen` loads `rangeSummary(month)` and `getGoals().targetCalories` on each month change; `SummaryScreen` loads `rangeSummary(start, end)` and the goal for the calorie chart, and separately `listWeight(start, end)` plus the last weight of the past year. Both keep the result in `remember` state and show only an error on failure.
- The database is Room version 3. The server-change cleanup is `deleteOtherServers` on `DayCacheStore` and `FavoritesCacheStore`, called from `MainActivity`.
- The no-connection notice ("No connection · data from 14:32", with the date when not today) is a private composable in `DayContent`.
- The backend's default goal is 2000 kcal, 125 g protein, 67 g fat, 225 g carbs, 2000 ml water and 250 ml glasses (`backend/src/domain/goals.ts`).

## Goals / Non-Goals

**Goals:**
- Home never shows the technical "Failed to connect" error in normal offline use: it shows the day with what the phone knows.
- History and Summary show what was seen before, and say clearly when a period was never loaded.

**Non-Goals:**
- Adding queued entries or captures to History's or Summary's totals.
- Logging weight or exporting the CSV without a connection.
- Prefetching periods that were never shown.

## Decisions

1. **Placeholder day on Home.** `DayRepository.offlinePlaceholder(date)` returns a `CachedDay` with no meals, a `WaterDay(date, 0, [])`, the cached goal (current server) or `DEFAULT_GOAL` (the backend's values, `isDefault = true`), and `refreshedAt = null`. It is never written to the cache. `DayContent` uses it only when the day has no copy, the cache has been read, and the last refresh ended `Offline`. A `Failed` refresh without a copy keeps showing the error alone. `CachedDay.refreshedAt` becomes nullable, and the notice without a time reads "No connection" / "Sin conexión". Everything else (queued entries, captures, water logging) already works on top of a shown day.

2. **One generic snapshot table for History and Summary.** Room version 4 adds `snapshot_cache(key TEXT PRIMARY KEY, json TEXT, server_url TEXT, refreshed_at INTEGER)`, with an auto-migration from 3. Its keys:
   - `history:<YYYY-MM-01>` → `HistoryMonth(days, targetCalories)`;
   - `summary:<start>:<end>` → `SummaryCopy(days, goalCalories)`;
   - `weight:<start>:<end>` → `WeightCopy(entries, lastKnownWeight)` (`SummaryPeriod` already names the week/month choice).

   Values are the screens' own data classes as JSON (`@Serializable`), with the same decode-or-drop rule as the day cache. *Alternative:* a table per screen. It would need more DAOs and migrations for the same "latest copy of a response" need.

3. **`SnapshotRepository`** (`commonMain`, behind a `SnapshotStore` interface for tests):
   - `suspend fun <T> cached(key, serializer): Snapshot<T>?`, where `Snapshot` holds the value and `refreshedAt`. It returns only the current server's rows; a row that doesn't decode is deleted and returns null;
   - `suspend fun <T> refresh(key, serializer, fetch: suspend () -> T): Refreshed<T>`, which runs `fetch`, stores the value if the server didn't change meanwhile, and returns `Fresh(value)`, `Offline(cause)` or `Failed(ApiException)`, classified like `DayRepository.refresh`;
   - `deleteOtherServers(url)`.

   The screens don't need live observation (nothing else writes these keys while they are shown), so a read followed by a refresh is enough.

4. **Screens.** History (per month) and Summary (per period, and per weight period):
   - on the key's change, show the cached value at once if any; otherwise keep today's loading presentation;
   - then refresh: `Fresh` replaces the value and clears the notice; `Offline` keeps the value with the notice "No connection · data from <time>", or, without a value, shows "No connection: this period hasn't been loaded yet" (in place of the content, not as an error); `Failed` shows the error text above the value, or alone without one;
   - logging or deleting weight stays online-only and, on success, refreshes the weight key.

   The notice composable moves from `DayContent` to a shared `ui/OfflineNotice.kt`. On Summary, the weight section repeats a notice only when it says something the calorie part doesn't (an error, or a notice while the calories are current), so a period never loaded shows the not-loaded-yet line once.

5. **Server change:** `MainActivity` also calls `snapshotStore.deleteOtherServers(newUrl)`.

## Risks / Trade-offs

- **A placeholder day may look like "nothing eaten"** when meals were logged from another device. → The "No connection" notice says the data isn't current; it's replaced as soon as the backend answers.
- **History and Summary don't include queued entries** → stated in the non-goals; they appear once sent.
- **The cache grows with every period viewed**: one small row per month or week. Not pruned, as with days.
