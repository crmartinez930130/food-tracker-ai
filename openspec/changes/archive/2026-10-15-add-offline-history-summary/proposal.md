## Why

Offline mode (#69, #70, #71) only helps on days that were loaded before. The first time the app opens on a day without a connection (the usual case away from home), Home shows only "Couldn't load this day: Failed to connect…", which also hides the meals queued and the captures kept for that day. History and Summary always show an error without a connection.

Tracked as GitHub issue #77: https://github.com/crmartinez930130/food-tracker-ai/issues/77

## What Changes

- **Home and a day's detail without a cached copy:** when the backend can't be reached, the screen shows the day empty instead of the error:
  - the cached goal, or the default goal (the same as the backend's) if none was ever loaded;
  - no stored meals and no water;
  - the queued meals and glasses and the kept captures of that day;
  - the "No connection" notice, without a time, since there is no copy.

  Adding water works (it is queued). When the connection is back, the day loads as usual. A backend error (for example a rejected token) without a copy still shows the error alone.
- **History:** each month shown is cached (its daily totals and the goal's calories). Without a connection the calendar and the day list show the cached month with "No connection · data from HH:mm". A month never loaded shows "No connection: this period hasn't been loaded yet" instead of the technical error.
- **Summary:** the same for the period shown (daily totals and goal calories), and separately for the weight section (the period's entries and the last known weight). Logging or deleting weight and exporting the CSV still need a connection and show their errors as today.
- **Backend errors** other than no connection are shown above the cached copy, as on Home.
- **Server change:** these copies follow the server like the rest of the cache: a server change deletes the other servers' copies.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `offline-cache`: the offline notice for a day without a cached copy, and new requirements for History and Summary without a connection.

## Impact

- **App (`frontend/composeApp`):**
  - `commonMain`: Room database version 4 (auto-migration from 3) with a `snapshot_cache` table; a `SnapshotRepository` (cached value plus refresh); `DayRepository` giving an offline placeholder day; `DayContent`, `HistoryScreen` and `SummaryScreen` using them; a shared offline-notice composable; strings in Spanish and English.
  - `androidMain`: the server-change cleanup also covers `snapshot_cache`.
- **Backend:** none.
