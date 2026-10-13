## Context

- **Current theme:** `Color.kt` holds schemes generated with `@material/material-color-utilities` 0.3.0 (`SchemeTonalSpot`, seed `#4CAF50`). `Theme.kt` wraps `MaterialTheme` with no custom typography or shapes. `ColorContrastTest` checks the text and background role pairs the app uses.
- **Compose Multiplatform 1.9.3** resolves `org.jetbrains.compose.material3` 1.9.0, which is androidx material3 1.4.0. In shared (common) code, the Material 3 Expressive APIs (`MaterialShapes`, `MaterialExpressiveTheme`, `MotionScheme`, `LoadingIndicator`, `ButtonGroup`) are **internal**. A probe compile confirms it. They are public only in the 1.10 and 1.11 alpha artifacts.
- **`androidx.graphics.shapes`** (`RoundedPolygon`, `Morph`) is multiplatform and already on the common classpath through material3.
- **The font:** Google Sans Flex is published in google/fonts as a single variable TTF with the axes ROND, opsz, slnt, wdth and wght, under OFL with no Reserved Font Names. Google's trademark notes allow using the name in connection with the original font.

## Goals / Non-Goals

**Goals:**
- One theme change gives the whole app the Greenwave look, in light and dark, with readable contrast.
- The components later issues need exist once, are tested where they have logic, and can be inspected on a sample screen.

**Non-Goals:**
- Redesigning any screen's layout (#43 to #48).
- Dynamic or wallpaper colors.
- Upgrading Compose to alpha artifacts.
- Button groups and floating toolbars (#45 and #46, which can use `SegmentedButton` until the expressive APIs are public).
- Motion polish beyond the loading indicator (#47).
- iOS.

## Decisions

### D1. Regenerate the schemes from `#81DFA2` with the same tool
- Same pipeline and variant as today (`SchemeTonalSpot`, contrast 0.0), so roles stay complete and consistent, and only the seed changes.
- **Measured fit:** the generated surfaces match the colors sampled from Greenwave's own screenshots: light surface `#F6FBF3` (sampled `#F6FBF4`), light container `#EAEFE8` (`#EAEFE9`), dark surface `#0F1511` (identical), dark container `#1C211C` (`#1B211D`). So the samples were themselves tonal-spot from this seed.
- The header comment in `Color.kt` records the seed and the command. The window backgrounds in `themes.xml` copy each scheme's `background`.
- **Alternative:** hand-picked colors from the screenshots. Rejected: incomplete roles and no tonal guarantees.

### D2. Bundle Google Sans Flex as the variable font, unmodified
- **The files:** `composeResources/font/google_sans_flex.ttf`, the original file renamed only for the resource system, plus `OFL.txt`.
- **No subsetting.** Subsetting would create a Modified Version. The OFL allows it, but keeping the original avoids any trademark question about the name. The cost is about 3.9 MB in the APK.
- **The family:** built with Compose resources `Font(Res.font.google_sans_flex, weight, variationSettings = FontVariation.Settings(FontVariation.weight(w)))` for 400, 500, 600, 700 and 800. Variable fonts work on API 26+, which is the app's minSdk.
- **The typography:** the M3 baseline `Typography()` with `fontFamily` replaced everywhere. Display and headline styles move to SemiBold or Bold with slightly tighter letter spacing, titles to SemiBold, and labels stay Medium.
- **Big figures:** an extra `emphasizedNumber` style (displayMedium, ExtraBold, tabular figures via `fontFeatureSettings = "tnum"`) is exposed through `FoodTrackerTheme.typeTokens`, so the M3 `Typography` contract isn't overloaded.

### D3. Rounder shapes through `Shapes`
- `Shapes(extraSmall = 8.dp, small = 12.dp, medium = 20.dp, large = 28.dp, extraLarge = 36.dp)` takes M3's scale toward the expressive feel of the samples.
- Every component that reads `MaterialTheme.shapes` (cards, sheets, dialogs, text fields, buttons) picks it up with no screen changes.

### D4. Our own expressive pieces on top of graphics-shapes
Upgrading Compose Multiplatform to an alpha just to get `MaterialShapes` and `LoadingIndicator` is not worth the risk. We build the small subset we need:
- **`ExpressiveShapes`:**
  - `cookie9`, built with `RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(0.3f))`;
  - `sunny`, an 8-point star with soft rounding;
  - `softBurst`, a 10-point star;
  - `pill` and `circle`, through `RoundedPolygon.circle` or `pill`.

  The parameters are tuned visually against the samples.
- **Polygon → Compose:** `RoundedPolygon.toComposePath(size)` converts the cubics into a Compose `Path`, normalizing the bounds into the target size. `PolygonShape(polygon)` implements `Shape.createOutline`.
- **`ExpressiveLoadingIndicator`:**
  - an infinite transition morphs through `[softBurst, cookie9, pentagon-like, sunny]` with `Morph(a, b).asCubics(progress)` while rotating;
  - it draws in `primary` on an optional `primaryContainer` disc.

  The morph index and progress math is a pure function with tests.

### D5. Components API (common, stateless)
- `BigNumber(value: String, unit: String?, modifier, style = emphasizedNumber)`.
- `ShapeBadge(text: String, shape: Shape = ExpressiveShapes.cookie9, containerColor = primaryContainer, size = 64.dp)`.
- `SegmentedBar(segments: List<BarSegment>, showLegend: Boolean = true)`, where `BarSegment(label, value: Double, color)`.
  - The pure `segmentFractions(values)` normalizes to 1 and returns all zeros when the total is 0. Negative values are clamped to 0.
  - Each segment gets a small gap, and the ends are rounded.
- `StatCard(label: String, value: String, unit: String? = null, containerColor = surfaceContainerHigh)`.

### D6. Design sample screen, debug only
- **Content:** `DesignSampleScreen` shows every component with sample data, in Spanish and English through the existing resources.
- **Access:** a "Muestra de diseño" row in Options, shown only when `isDebugBuild()` is true. The Android actual checks `ApplicationInfo.FLAG_DEBUGGABLE`; the iOS stub returns false.
- **Why:** it lets the emulator verify every component in both themes without touching real screens.

### D7. Spinner replacement scope
- **Replaced** with `ExpressiveLoadingIndicator`, the full-size spinners only:
  - `LoadingAware`'s initial state;
  - Goals loading;
  - Capture "analyzing";
  - Capture favorites loading.
- **Kept:** small inline progress inside buttons (16–18 dp, in Goals save, Review save and Export), where a morphing shape isn't readable.

## Risks / Trade-offs

- **APK size grows by about 3.9 MB with the variable font.** → It's acceptable for a personal app. Subsetting can come later if it matters, with the name considered.
- **Hand-tuned shapes look slightly different from the official `MaterialShapes`.** → We'll switch to the official ones when Compose Multiplatform material3 makes them public. The components hide the shapes behind `ExpressiveShapes`.
- **The new palette's contrast.** → `ColorContrastTest` runs on the regenerated schemes. If a pair fails, the design adjusts which role a screen uses, never a hand-edited generated role.

## Migration Plan

None. This is a visual-only change. Reverting the commit restores the old theme.
