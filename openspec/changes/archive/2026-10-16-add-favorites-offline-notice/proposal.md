## Why

Since #70, Capture shows the saved copy of the favorites when the backend can't be reached, but nothing says so, and the list may be out of date. Without a copy, it shows the raw connection error in red ("No se pudieron cargar los favoritos: Failed to connect to /10.0.2.2:3000"). Home, History and Summary already say "Sin conexión" in those cases (#77), and Favorites should match.

Tracked as GitHub issue #83: https://github.com/crmartinez930130/food-tracker-ai/issues/83

## What Changes

- **With a copy and no connection:** the Favorites section shows "Sin conexión · datos de las HH:mm" (or "del DD/MM a las HH:mm") above the list, built the same way as History and Summary.
- **Without a copy and no connection:** "Sin conexión: los favoritos todavía no se cargaron." replaces the raw error.
- **A backend error (it answered):** shown as today, meaning the error without a copy and the copy without a message.
- **With a connection:** no notice.
- `FavoritesRepository` reports how a load went (fresh, offline, error) instead of throwing, and returns the copy with its time, like `SnapshotRepository`.
- One new string in Spanish and English.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `offline-queue`: "Favorites available offline" says when the list is a copy, or that it never loaded.

## Impact

- **App:**
  - `FavoritesRepository` (`cached()` returns `Snapshot<List<MealEntry>>`, `load()` becomes `refresh(): Refreshed<…>`), with its tests;
  - `CaptureScreen` (favorites state and the notice in `FavoritesSection`);
  - `strings.xml` (es/en).
- No database change: `favorites_cache` already stores `refreshed_at`.
- No backend change.
