## Context

`:shared` already has the offline queue (`OutboxRepository`, `storedSettingsOutbox`), and the watch has its own `CacheDatabase` with an `outbox` table. `todayWidgetStates` counts queued glasses, so a queued glass shows at once on anything that reads it. The phone's widget logs a glass this way (#90). The phone sends the queue in the background with WorkManager (`OutboxSync`, in `composeApp`).

Wear Compose Material 3 1.6.2 offers:
- `EdgeButton`, hosted by `ScreenScaffold`;
- `SurfaceTransformation` and `Modifier.transformedHeight`, for `TransformingLazyColumn` items;
- `CircularProgressIndicator`.

For tiles, `protolayout-material3` 1.4.2 offers edge buttons for the `primaryLayout` bottom slot and `loadAction()` clickables. A tap on one re-invokes `onTileRequest`, with `currentState.lastClickableId`.

## Goals / Non-Goals

**Goals:**
- Log water from the wrist, online or offline, exactly once.
- Look like Food Tracker and like Wear OS 6.

**Non-Goals:**
- Undoing a glass on the watch.
- Complications.
- Voice logging.
- Phone or backend changes.

## Decisions

### D1: Brand colors
`WatchColors`, in `wearApp`, holds the app's dark palette (the hex values of `DarkColors` in `composeApp/.../ui/theme/Color.kt`, which the watch can't depend on), with a comment to regenerate both together. It feeds two schemes:
- **The app:** a Wear Compose `ColorScheme` (primary, containers, surfaces, error) passed to `MaterialTheme`.
- **The tile:** a `protolayout-material3` `ColorScheme`, passed to `materialScope(..., allowDynamicTheme = false, defaultColorScheme = …)`.

A watch UI is always dark, so only the dark scheme is used.

### D2: The Expressive screen
`TransformingLazyColumn` items:
- a `ListHeader` with "Today";
- a calories card (number, target, left/over);
- a macros card;
- a water card;
- the stale note.

Each card carries `transformation = SurfaceTransformation(spec)` and `Modifier.transformedHeight(this, spec)` from `rememberTransformationSpec()`, so cards scale and curve near the top and bottom edges while scrolling.

The calories ring fills the screen behind the list, drawn in the `AppScaffold` so it stays put while the list scrolls. Wear's `CircularProgressIndicator` showed only its track, on the Galaxy Watch6 as on the emulator, as `LinearProgressIndicator` did in #104. So the ring is a `Canvas` arc (track plus progress, round cap) with the same colors.

**Fitting the round screen** (tried on the watch):
- the list keeps 18 dp of side margin so the cards stay inside the ring;
- cards use 8 dp of side content padding;
- macro lines are 12 sp and centered, so "Carbohidratos 105/220 g" fits one line on a Galaxy Watch6.

### D3: Logging a glass on the watch
`logWatchGlass(context)`:
1. reads the glass size from today's state, the cached goal or the default;
2. builds `storedSettingsOutbox(..., requestSync = { WatchOutboxSync.request(context) })` with the watch's settings and database;
3. logs `LogWaterRequest(glassMl, loggedAt = now)` for today;
4. if it was sent at once, refreshes today, as the phone widget does (#90), because that path doesn't touch the cache.

The state flow shows a queued glass right away, and the tile is redrawn.

`WatchOutboxSync` is a unique WorkManager job for the watch: network-constrained, exponential backoff and `KEEP`. It runs `outbox.sendAll()` and retries while offline, which `watchSyncResult(report)` decides. That function is pure and tested.

A glass rejected at once (4xx) is dropped silently, as on the phone widget. Home on the phone shows the rejected ones.

### D4: App "+ 250 ml"
`ScreenScaffold(edgeButton = { EdgeButton(onClick = …) { Text("+ 250 ml") } })`:
- the label uses the goal's glass size;
- the button is enabled only with settings;
- a tap gives a confirm haptic and calls `logWatchGlass` in the activity's scope.

### D5: Tile "+ glass"
- **The button:** the bottom slot holds an edge button labeled "+ 250 ml", with `clickable(id = "add_glass", action = loadAction())`.
- **Water:** the water line moves next to the ring.
- **The click:** `onTileRequest` logs the glass first when `requestParams.currentState.lastClickableId == "add_glass"`, then builds the layout from the updated state.
- **No double glass:** only a request caused by the tap carries that id, which is checked on the emulator. To be safe, a second request with the same id within 2 seconds is ignored (`isNewTileClick`, tested).

## Risks / Trade-offs

- **[Accidental taps on a small screen]** → The edge button is large but deliberate. Undo stays on the phone (Home), as agreed in #106.
- **[The palette copied into the watch module]** → Like the window backgrounds in `themes.xml`, it's documented next to the source palette.
