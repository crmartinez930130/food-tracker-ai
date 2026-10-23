## Context

The tile is built with `protolayout-material3` inside `materialScope(..., allowDynamicTheme = false, defaultColorScheme = WatchColors.tile)`. `circularProgressIndicator` accepts a size and `ProgressIndicatorColors`. A tile can show images only from resources it declares in `onTileResourcesRequest`.

The "+ glass" tap already works:
- the button has a `loadAction` clickable with an id unique per drawing (`glassClickId`);
- `onTileRequest` checks it with `isNewTileClick` and then logs the glass.

## Decisions

### D1: Layout
The tile is laid out by hand, without `primaryLayout`, like Samsung's own tiles. A centered column holds:
- the title "Today" in `TITLE_SMALL`;
- the panel;
- the stale or no-data line, when there is one.

`primaryLayout`'s reserved title and edge-button room left the panel too little height on the Galaxy Watch6, and the bottom row got clipped.

**The panel:** a `Box` with `WatchColors.SURFACE_CONTAINER` background, rounded corners, wide side padding (a pill like "Salud"), and the open-app clickable. It holds a `Row` of two circles, each about 34% of the screen width (`deviceConfiguration.screenWidthDp`), with `SURFACE_CONTAINER_HIGH` background and a thin `circularProgressIndicator`:
- **calories:** a `PRIMARY` indicator (`ERROR` once over the goal) on a `PRIMARY_CONTAINER` track. Inside, the number in `LABEL_LARGE` and "kcal".
- **water:** a `TERTIARY` indicator on a `TERTIARY_CONTAINER` track. Inside, the ml and "ml +" with a small drop. Its clickable is the `loadAction` with `glassClickId`, which takes the tap over the panel's open-app click.

The user first tried four circles: calories, water, protein and a separate "+ glass" circle. They kept only calories and water, and moved "+ glass" into the water circle.

### D2: The drop image
`ic_tile_water` is a vector drawable (tinted per use), registered in `onTileResourcesRequest` as an `AndroidImageResourceByResId` with id `water`. The resources version goes to "2", so cached tiles fetch it.

### D3: Without settings
The main slot keeps the single "Open Food Tracker on your phone" message, without the grid.

### D4: The #107 leftovers
- Remove the unused `storedSettingsDayRepository` import.
- `EdgeButton` label: `today?.glassMl ?: DEFAULT_GOAL.waterGlassMl`.

## Risks / Trade-offs

- **[Small screens]** → Sizes come from the device width, so the grid scales. Checked on the 44 mm Galaxy Watch6 and on the emulator.
- **[A second clickable inside a clickable panel]** → protolayout gives the innermost clickable the tap, which is checked on the device: the button logs a glass, and the rest of the panel opens the app.
