# Design

## Context

- **`FavoritesRepository`:**
  - `cached()` returns the list (or null) and drops `refreshed_at`;
  - `load()` throws like the API call.
- **`CaptureScreen`:**
  - shows the copy first, then calls `load()`;
  - on any failure it sets `favoritesError` only when there's no copy;
  - `FavoritesSection` shows the error, a loading indicator, the empty state or the list;
  - removing a favorite reloads through the same `LaunchedEffect`.
- **History and Summary** use `SnapshotRepository`:
  - `cached()` returns a `Snapshot(value, refreshedAt)`, and `refresh()` returns `Refreshed.Fresh/Offline/Failed`, where `Failed` means the backend answered (`ApiException`);
  - `SnapshotNotice`/`offlineNotice` render "Sin conexión · datos de las HH:mm" and "…del DD/MM a las HH:mm".

## Goals / Non-Goals

**Goals:**
- Favorites tells "no connection" apart from a backend error, with the same wording and time format as the other screens.

**Non-Goals:**
- Favorite toggling and removal errors without a connection, which stay as they are.
- Queueing favorite changes.

## Decisions

1. **Repository shape.** `FavoritesRepository` mirrors `SnapshotRepository`, reusing its `Snapshot` and `Refreshed` types:
   - `cached(): Snapshot<List<MealEntry>>?`;
   - `refresh(): Refreshed<List<MealEntry>>`, which never throws (except cancellation) and keeps the copy only when the server didn't change meanwhile, as today.
2. **What the section shows**, from a pure function `favoritesView(cached, refreshed)` in common code, so it can be unit-tested:

   | Refresh | Copy | Shows |
   |---|---|---|
   | Fresh | — | the list, no notice |
   | Offline | yes | the copy and `Offline(refreshedAt)` |
   | Offline | no | the "not loaded yet" notice for favorites (new string `offline_favorites_not_loaded`) |
   | Failed | yes | the copy without notice (today's behavior) |
   | Failed | no | the error text `error_load_favorites` with the backend's message, in red (today's behavior) |

   The screen keeps showing the copy while the refresh runs.
3. **Rendering.** The offline line uses `offlineNotice(refreshedAt, today)` (shared with History and Summary) in `bodySmall`/`onSurfaceVariant`, under the section title and above the list. The "not loaded" line takes the list's place, in the same style and not in red. The section keeps its own padding, so it doesn't use `SnapshotNoticeText` with its extra horizontal padding.
