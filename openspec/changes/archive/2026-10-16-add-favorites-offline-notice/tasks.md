## 1. App

- [x] 1.1 `FavoritesRepository`: `cached()` returns a `Snapshot`, and `load()` becomes `refresh(): Refreshed` (D1). Verify with `FavoritesRepositoryTest`:
  - the copy and its time;
  - Offline keeps the copy;
  - an error status gives Failed and keeps the copy;
  - another server's copy isn't used.
- [x] 1.2 `favoritesView(cached, refreshed)` and the section rendering, plus `offline_favorites_not_loaded` in Spanish and English (D2, D3). Verify with a `favoritesView` test for every row of the table, `StringResourcesTest`, and the build.

## 2. Verification

- [x] 2.1 On the emulator:
  - with the backend up, favorites without a notice;
  - backend down after loading, "Sin conexión · datos de las HH:mm" above the copy, and re-logging one queues it;
  - backend down with the copy deleted (another server address), "todavía no se cargaron" and no red error;
  - in English, the same texts in English.

  Use a copy of the real database and restore the app's settings afterwards.
