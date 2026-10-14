## Context

- **Compose Multiplatform 1.9.3** has `AnimatedContent`, `animate*AsState`, `Animatable`, springs, `graphicsLayer` and `MutableInteractionSource`, all in common code. On Android, Compose already scales animation durations by the system's animator duration scale. Still, an infinite transition (our loading indicator) keeps running, and our own `Animatable`-driven effects should not depend on that implicitly.
- **Shapes:** `ExpressiveShapes` exposes `cookie9`, `sunny`, `softBurst`, `pentagon` and `circle` as `RoundedPolygon`s, plus `cubicsToPath` (which fits cubics to a size) and `PolygonShape`. `androidx.graphics.shapes.Morph(a, b).asCubics(progress)` gives an in-between shape.
- **Navigation:**
  - `App` renders `when (appState.screen)`;
  - `Screen.Main(tab)` changes on tab selection, and `MainScreen` renders `when (tab)`;
  - `Review` reads `appState.pendingAnalysis`, which `openMain` clears.
- **Empty states today** are plain text: Home's "Todavía no registraste nada este día.", Summary's weight empty text, and Capture's no-favorites text.

## Goals / Non-Goals

**Goals:**
- Illustrations and motion as listed in the issue, with nothing janky or flickering while navigating.
- One switch, `LocalReduceMotion`, that every effect of ours honors.
- The rules (transition direction, badge shape, duration-scale reading) are pure and tested.

**Non-Goals:**
- Lottie or bitmap assets: the illustrations are drawn in code.
- Shared-element transitions.
- Haptics.
- Animating every number in the app: only the headline figures listed.
- Backend changes.

## Decisions

### D1. Reduced motion as a CompositionLocal
- **Platform function:** `expect fun systemAnimationsDisabled(): Boolean` is `@Composable`.
  - **Android:** `Settings.Global.getFloat(resolver, ANIMATOR_DURATION_SCALE, 1f)`, through the pure `animationsDisabled(scale) = scale == 0f`, which is tested. It's re-read in a `LifecycleResumeEffect`, so changing the option and coming back takes effect.
  - **iOS:** `false`.
- **Where it's provided:** `App` provides `LocalReduceMotion` once, around the screens.
- **Who reads it:** each effect in D3–D6. With reduce motion on:
  - figures and bars snap to their targets;
  - the badge snaps to its shape;
  - screen changes use `EnterTransition.None` / `ExitTransition.None`;
  - the press effect is off;
  - the loading indicator draws a still `cookie9`.

### D2. Illustrations drawn in code
- **The composable:** `EmptyIllustration(kind, modifier)` is a 120 dp `Canvas` with three `kind`s. Each uses scheme colors, so it works in both themes:
  - **`PLATE`:** an outer circle in `surfaceContainerHighest`, an inner rim ring in `outlineVariant`, and a fork and knife as rounded rects in `primary` on either side. A small 4-point sparkle in `tertiary`.
  - **`SCALE`:** a rounded-square body in `tertiaryContainer`, a half-dial in `surface` with three tick marks, and a needle in `tertiary`.
  - **`STAR`:** a `softBurst` badge in `secondaryContainer` with a rounded 5-point star (`RoundedPolygon.star(5)`) in `primary`.
- **Placement:** each sits centered above the existing empty text:
  - `PLATE` in `DayContent`, so it shows on Home and on the day detail;
  - `SCALE` in `WeightSection`;
  - `STAR` in Capture's favorites.
- **Accessibility:** they are decorative (no content description). The text below carries the meaning.

### D3. Counting figures
- **The helper:** `animatedCount(target: Int): Int` is backed by an `Animatable(0f)`. It animates to the target with `tween(700, easing = FastOutSlowInEasing)` whenever the target changes, so it counts up on first show and to the new value after an edit. With reduce motion it snaps.
- **Used for:**
  - Home's kcal (`CaloriesCard`) and water (`WaterCard`);
  - Summary's daily average and days-over-goal cards.
- **Not animated:**
  - the percentage badge text, which changes with the shape;
  - the Review total, so typing in a field doesn't make the number jump around.

### D4. Growing bars
- **`SegmentedBar`:** each segment's weight animates from 0 with `animateFloatAsState`. While the total is still small, the empty track fills the rest.
- **`WaterCard`:** the progress fraction animates from 0 with a spring (`dampingRatio = 0.8`).
- **`CalorieChart`:** an `Animatable` grows the bars from 0 to 1 whenever `days` changes, with a spring of medium-low stiffness. Each bar is drawn at its height times that progress, and the goal line doesn't move.
- **Reduce motion:** everything starts at its final value.

### D5. Badge morph at the goal
- **The rule:** `badgeShapeFor(percent)` is `GOAL_REACHED` when the percent is ≥ 100, and `IN_PROGRESS` otherwise (including null). It is pure and tested at 99, 100 and 112.
- **The shape:** `ShapeBadge` gets a `morphProgress: Float` overload. It builds `Morph(cookie9, sunny)` once, and a `MorphShape(morph, progress)` that implements `Shape` via `cubicsToPath(morph.asCubics(progress), size)`.
- **The animation:** `CaloriesCard` animates the progress to 0 or 1 with `spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)` for a visible bounce.
- **The trigger:** the shape follows the *counted* figure (D3), not the final total. The badge springs into the sunburst the moment the count passes 100% of the goal. Home is rebuilt when it's shown again, after saving a meal for instance, so a target tied to the final total would already be at 1 and never morph. The colors keep their current rule (`errorContainer` when over).

### D6. Screen transitions
- **The rule:** `screenDepth(screen)` is pure and tested:
  - `Main` is 0;
  - `Capture`, `Goals`, `Settings` and `DayDetail` are 1;
  - `Review`, `Reminders` and `DesignSample` are 2.
- **The container:** `App` renders the screens in `AnimatedContent(targetState = screen, contentKey = { it::class })`, so a tab change inside Main isn't a screen change, while `DayDetail` to `DayDetail` would be. The content uses the lambda's state, not `appState.screen`.
- **The transition:**
  - forward (greater depth): the new screen slides in from the right, a third of the width, with a fade, under `spring(stiffness = StiffnessMediumLow)`;
  - back: the reverse;
  - same depth: a cross-fade.
- **Review's exit:** `openMain` clears `pendingAnalysis`, which would blank Review during its exit animation. The Review content keeps the last non-null analysis in a `remember`, so it exits intact.
- **Main's tabs:** they cross-fade (`Crossfade` with a 200 ms tween).
- **Reduce motion:** the changes are instant.

### D7. Buttons that react
- **The modifier:** `Modifier.pressBounce(interactionSource)` collects `isPressed` and animates a `graphicsLayer` scale to 0.94 while pressed. On release it goes back with `spring(dampingRatio = 0.4f)`. With reduce motion it does nothing.
- **Applied to:**
  - the FAB;
  - `ConnectedButtonGroup` buttons;
  - Capture's photo tiles and Analyze button;
  - Review's save pill;
  - Goal's save button;
  - Home's percentage badge.

  Each passes its own `MutableInteractionSource` to the clickable.

## Risks / Trade-offs

- **Animations on every tab revisit** (bars re-grow, numbers re-count when Home recomposes from scratch). → This is intended "alive" behavior, and the durations are short (≤ 700 ms). The data reloads anyway.
- **Content that depends on app state during an exit transition.** → Review is handled in D6. The other screens read only their own parameters.
- **The animation duration scale changing while the app is in the foreground.** → It's re-read on resume. Changing the setting requires leaving the app anyway.

## Migration Plan

None. This is a frontend-only change.
