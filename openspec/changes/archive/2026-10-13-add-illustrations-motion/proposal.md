## Why

This is the last redesign issue inspired by the Material 3 Expressive samples (https://m3e.c5inco.dev/). The screens now have the Greenwave look but are static. The reference apps use simple illustrations in empty states and springy motion that make them feel alive. Motion also has to respect the system's "remove animations" accessibility option. Tracked as GitHub issue #47: https://github.com/crmartinez930130/food-tracker-ai/issues/47

## What Changes

- **Empty-state illustrations:** simple vector drawings made for the app, drawn with the theme's colors so they follow light and dark:
  - an empty plate with a fork and knife, for a day with no meals (Home and the day detail);
  - a bathroom scale, for a Summary period without weight;
  - a star in a scalloped badge, when there are no favorites on Capture.
- **Motion:**
  - big figures count up to their value: today's kcal and water, Summary's daily average and days over the goal;
  - bars grow into place: the macro segmented bars, the water bar and the calorie chart bars;
  - the calorie badge morphs from the cookie shape to a sunburst when the day reaches 100% of the goal, with a spring;
  - screens slide and fade: deeper screens come in from the right, going back slides out to the right, and Main's tabs cross-fade;
  - buttons react to touch: the main actions and the connected buttons shrink slightly while pressed and spring back.
- **Reduced motion:** when the system's "Remove animations" is on (animator duration scale 0), the figures, bars and badge show their final state at once, screens change without transitions, and the loading indicator shows a still shape.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-appearance`: empty states show illustrations, and the app has expressive motion that is turned off when the system asks to remove animations.

## Impact

- **Frontend common:**
  - new `ui/components/Illustrations.kt` (three Canvas illustrations) and `ui/components/Motion.kt`:
    - `LocalReduceMotion`;
    - `animatedCount`;
    - `Modifier.pressBounce`;
    - the badge morph shape;
    - the screen transition rules.
  - `App` wraps the screens in an `AnimatedContent`, and `MainScreen` cross-fades its tabs;
  - `DayContent`, `WaterCard`, `SegmentedBar`, `CalorieChart`, `SummaryScreen`, `WeightSection`, `CaptureScreen`, `ShapeBadge`, `ConnectedButtonGroup` and `ExpressiveLoadingIndicator` adopt them.
- **Frontend platform:** `systemAnimationsDisabled()` as expect/actual. Android reads `Settings.Global.ANIMATOR_DURATION_SCALE` and re-reads it on resume. iOS returns false, a secondary target, as with `isDebugBuild`.
- **Tests:**
  - the screen depth and transition direction;
  - the badge shape for a percentage;
  - the duration-scale rule.
- **Verification:** the emulator check turns the "Remove animations" option on and back off.
- **Backend:** no change.
