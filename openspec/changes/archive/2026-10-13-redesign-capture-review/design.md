## Context

- **`CaptureScreen`:**
  - one scrolling, centered `Column`;
  - `Button` and `OutlinedButton` for camera and gallery;
  - an inline preview, the `DescriptionField` (with the mic), `PortionChips` (`FilterChip`s), an "Analyze" `Button` (or `ExpressiveLoadingIndicator(56.dp)` while analyzing), and the favorites as rows with dividers;
  - a bottom `TextButton` cancel.

  The camera is a full-screen mode of the same composable (`CameraCapture`).
- **`ReviewScreen`:**
  - a title;
  - a `TextButton` row for the meal type, with brackets around the selected one;
  - a `LazyColumn` of `EditableItemRow`s (text fields), the notes field and a one-line total;
  - error text, then cancel (opens the discard prompt) and save buttons.
- **Back handling** lives in pure functions (`captureBackAction`, `reviewBackAction`) with tests, and stays as is.
- **Components available:**
  - `ConnectedButtonGroup` (from #44), `BigNumber`, `SegmentedBar` and `ExpressiveLoadingIndicator`;
  - icons: `ic_delete`, `ic_mic`, `ic_star_filled`, `ic_check`.

## Goals / Non-Goals

**Goals:**
- Same flow and behavior, in the expressive style, with the total always visible on the save action.
- The new pure logic is tested.

**Non-Goals:**
- Backend or analysis changes.
- Restyling the in-app camera (`CameraCapture`). It's a full-screen viewfinder already, and Pinhole's look is out of scope here.
- Animations (#47).
- Editing foods' confidence. It stays read-only.

## Decisions

### D1. Capture layout
- **Structure:** a `Scaffold` with a `TopAppBar` ("Registrar una comida", a close icon → `onCancel`). The close icon is hidden while analyzing, matching back being ignored then. The body is a scrolling `Column` with 16 dp padding.
- **No photo yet:** two equal tiles in a row, each a `Card` 132 dp tall with a 32 dp icon over its label:
  - "Sacar foto" (`photo_camera`) on `primaryContainer`;
  - "Elegir de la galería" (`photo_library`) on `secondaryContainer`.
- **Photo chosen:**
  - the preview fills the width, up to 360 dp tall, clipped to `shapes.extraLarge`;
  - under it, a tonal button with the matching icon: "Volver a sacar" or "Elegir otra".
- **The rest:** the description field (unchanged), then the portion group (D2), then a 56 dp full-width pill "Analizar" button.
- **Analyzing:** the button area shows a centered `ExpressiveLoadingIndicator(size = 72.dp, containerColor = primaryContainer)` with "Analizando…" under it.
- **Favorites** (only when there's no photo and no description, as today): a "Favoritas" title and a `LazyRow` of 200 dp wide cards on `surfaceContainerLow`. Each card has:
  - the foods (2 lines max);
  - the kcal as a small `BigNumber`;
  - a filled-star `IconButton` that unmarks it.

  Tapping the card re-logs it, and the click label stays "Registrar de nuevo".

### D2. Portion as connected buttons with "none"
- **Component change:** `ConnectedButtonGroup` accepts `selectedIndex = -1` (nothing selected: every button is drawn unselected) and `showCheck: Boolean = true`.
- **The portion group** uses the three portions with `showCheck = true`. Tapping goes through `toggledPortion(selected, tapped)`: tapping the selected portion clears it, and tapping another selects that one.
- **Tests:** `toggledPortion` (select, switch, clear); `connectedCorners` is unchanged.

### D3. Review layout
- **Structure:** a `Scaffold` with a `TopAppBar` ("Revisar análisis", and a close icon with the "Cancelar" description that opens the discard prompt). The prompt and the back handling are unchanged.
- **The body** is one `LazyColumn` (16 dp horizontal padding, bottom padding for the pill) with:
  1. **Meal type:** `ConnectedButtonGroup` with the four types, `showCheck = false` so the longest label ("Desayuno" / "Breakfast") fits a quarter of the width. The default type by time of day is unchanged.
  2. **Food cards:** one card per food on `surfaceContainerLow`, with:
     - the name field and a remove `IconButton` (`ic_delete`, "Quitar");
     - a row with grams and kcal;
     - a row with protein, fat and carbs;
     - the confidence as a small `AssistChip`-like label.

     The text fields keep their current labels.
  3. If there are no foods, the "no food" message.
  4. **Notes field.**
  5. **Total card** (`primaryContainer`): the "Total" title, `BigNumber(kcal, "kcal")` and a `SegmentedBar` of protein, fat and carbs grams with legend.
  6. Error text, when there is one.
- **The fixed pill:**
  - aligned at the bottom over the list, with a 16 dp margin above the navigation bar inset;
  - a full-width 56 dp `Button` with `CircleShape` and `elevation`, reading "Guardar · 736 kcal";
  - while saving it is disabled and shows a small progress indicator before the label.

  The old cancel button goes away: the close icon is the cancel action.

### D4. Totals and number fields as pure functions
- **`mealTotals(items: List<MealItem>)`** returns kcal, protein, fat and carbs. Review calls it with `items.map { it.toMealItem() }`, and both the total card and the pill read it.
- **`editableNumber(text)`** parses Review's number fields. Anything that isn't a number counts as 0, as before. It now also accepts a decimal comma ("12,5"), which the Spanish decimal keyboard types: before, such a value silently counted as 0. The fields use the decimal keyboard.
- **Tests:**
  - the totals' sums and an empty list;
  - `editableNumber` with a comma, a dot, blank and text.

### D5. Resources
- **Icons** (Material Symbols Outlined, Apache-2.0): `photo_camera`, `photo_library`, `close`.
- **New strings:**
  - `review_save_total` ("Guardar · %1$d kcal" / "Save · %1$d kcal");
  - `review_total_title` ("Total");
  - `capture_analyzing` ("Analizando…" / "Analyzing…");
  - `capture_close` ("Cerrar" / "Close").
- **Replaced:** `review_total` and `review_confirm_save` are removed.

## Risks / Trade-offs

- **The fixed pill covers the end of the list.** → The `LazyColumn` gets bottom content padding equal to the pill's height plus its margin, so the last card and the notes can scroll above it.
- **Four meal types in a quarter-width each.** → No check icon, single-line labels, and 8 dp horizontal padding. It's checked on the emulator in English and Spanish.

## Migration Plan

None. This is a frontend-only visual change.
