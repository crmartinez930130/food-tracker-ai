## Why

This is the first of six redesign issues inspired by the Material 3 Expressive samples at https://m3e.c5inco.dev/, specifically the **Greenwave** palette. Every later screen redesign (#43 to #48) needs a shared visual base first: colors, typeface, shapes and a handful of reusable expressive components. Tracked as GitHub issue #42: https://github.com/crmartinez930130/food-tracker-ai/issues/42

## What Changes

- **Palette:** Material 3 tonal-spot schemes, light and dark, generated from the Greenwave seed `#81DFA2`. They replace the current `#4CAF50` schemes. The generated surfaces match the colors sampled from Greenwave's own screenshots: light `#F6FBF3` against the sampled `#F6FBF4`, dark `#0F1511` exactly. The launch window backgrounds follow. The theme default stays System.
- **Typeface:** **Google Sans Flex** (Google Fonts, SIL OFL 1.1), bundled as the variable font with its license.
  - Every text style uses it.
  - Display and headline are heavier for an expressive feel.
  - A new emphasized style serves big figures.
- **Shapes:** rounder corner scale for cards, sheets and buttons.
- **Reusable components** in `ui/components/`, each with a pure, tested layout or math core where there is logic:
  - `BigNumber`: a large figure with a small unit ("1.240 kcal");
  - `ShapeBadge`: a value inside an expressive shape, cookie or sunny style, like Greenwave's "87";
  - `SegmentedBar`: proportional colored segments with a dot legend;
  - `StatCard`: a label and a figure on a tinted container;
  - `ExpressiveLoadingIndicator`: a shape that morphs and rotates. It replaces the full-size spinners: initial loads, Goals loading, photo analysis and favorites loading.
- **Design sample screen:** a "Muestra de diseño" screen that shows all the components in both themes. Options offers it only in debuggable builds.
- No screen layout changes; screens pick up the new look through the theme alone.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-appearance`: the brand theme becomes the Greenwave palette with the Google Sans Flex typeface and expressive shapes, and full-screen loading shows the expressive indicator.

## Impact

- **Frontend common:**
  - `ui/theme/Color.kt` is regenerated, plus new `Type.kt` and `Shape.kt`, and `Theme.kt` wires them in.
  - New `ui/components/` with the five components and their pure helpers, and a `DesignSampleScreen`.
  - `Screen.DesignSample` in `AppState`.
  - `LoadingAware` and three call sites switch to the new indicator.
  - Tests: contrast for the new schemes, the segment math, and the morph and shape helpers.
- **Frontend Android:** window backgrounds in `res/values{,-night}/themes.xml`, and an `isDebugBuild()` actual.
- **Resources:** `composeResources/font/google_sans_flex.ttf` (variable, about 3.9 MB) and `OFL.txt` next to it.
- **Dependencies:** `androidx.graphics:graphics-shapes` declared explicitly. It already comes transitively through material3.
- **Backend:** no change.
