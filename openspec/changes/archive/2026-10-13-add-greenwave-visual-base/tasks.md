## 1. Palette

- [x] 1.1 Regenerate `Color.kt` from seed `#81DFA2` (`SchemeTonalSpot`, contrast 0.0, material-color-utilities 0.3.0). Update the header comment and the window backgrounds in `res/values{,-night}/themes.xml`. Verify with `ColorContrastTest` on the new schemes; if a pair fails, adjust role usage, never a generated value.

## 2. Typography and shapes

- [x] 2.1 Add `google_sans_flex.ttf` (the unmodified variable font) and `OFL.txt` under `composeResources/font/`. Build `Type.kt` (the variable-font family for 400, 500, 600, 700 and 800, the expressive `Typography`, and an `emphasizedNumber` token) and `Shape.kt`. Wire both into `FoodTrackerTheme`. Verify with `./gradlew :composeApp:assembleDebug` and in the emulator (4.1).

## 3. Components

- [x] 3.1 Add `ui/components/ExpressiveShapes.kt` (the polygons, `toComposePath`, `PolygonShape`) and `ExpressiveLoadingIndicator`, with the pure morph-step math. Verify with `ExpressiveShapesTest`:
  - path bounds fit the target size;
  - polygons are centered;
  - the morph index wraps;
  - progress stays within 0..1.
- [x] 3.2 Add `BigNumber`, `ShapeBadge`, `SegmentedBar` (with the pure `segmentFractions`) and `StatCard`. Verify with `SegmentedBarTest`: fractions sum to 1; an all-zero input gives zeros; negatives are clamped; a single segment gets everything.

## 4. Sample screen and adoption

- [x] 4.1 Add `DesignSampleScreen`, `Screen.DesignSample`, the `isDebugBuild()` expect/actual and the debug-only Options row. Replace the full-size spinners listed in design D7. Add the strings in both languages. Verify on the emulator, light and dark:
  - the sample screen shows every component correctly;
  - all text is Google Sans Flex;
  - every screen (Home, Summary, Capture, Review, Goals, Options, Reminders) works as before with the new palette and shapes;
  - the analyze and initial loads show the morphing indicator.
