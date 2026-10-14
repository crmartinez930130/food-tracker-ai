## 1. Motion foundation

- [x] 1.1 Add `Motion.kt`, with:
  - `LocalReduceMotion` and the pure `animationsDisabled(scale)`;
  - `animatedCount`;
  - `Modifier.pressBounce`;
  - the pure `badgeShapeFor` and `screenDepth` / `transitionDirection`.

  Add `systemAnimationsDisabled()` as expect/actual (Android reads the scale and re-reads it on resume; iOS returns false). Verify with tests: the scale rule, the badge shape at 99/100/112/null, and the depth and direction for forward, back and the same level.

## 2. Illustrations

- [x] 2.1 Add `Illustrations.kt` with `EmptyIllustration(PLATE | SCALE | STAR)` (D2) and use it in the day content, `WeightSection` and Capture's favorites. Add each to the design sample screen for review.

## 3. Motion adoption

- [x] 3.1 Count-up (D3) on Home's kcal and water and on Summary's two stat cards. Bar growth (D4) in `SegmentedBar`, `WaterCard` and `CalorieChart`.
- [x] 3.2 Badge morph (D5): `MorphShape`, `ShapeBadge` with morph progress, and the spring in `CaloriesCard`.
- [x] 3.3 Screen transitions (D6) in `App`, with Review keeping its last analysis while exiting. Tab cross-fade in `MainScreen`. Press bounce (D7) on the listed buttons. A still loading indicator under reduce motion. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 4. Verification

- [x] 4.1 On the emulator, in light and dark:
  - the three illustrations in their empty states;
  - the kcal and water counting up on Home, and the bars growing;
  - the badge morphing when a meal takes the day over 100% (and back after deleting it);
  - screen slides forward and back (Goal, Options → Reminders, Capture → Review → discard, History → day detail), and tab cross-fades, with no blank frames;
  - buttons bouncing on press;
  - then turn "Remove animations" on (`animator_duration_scale` 0): everything appears at once, there are no transitions, and the indicator is still;
  - turn it back to 1 and see the animations return after resuming.

  Restore the setting and clean the test data afterwards.
