## Why

The widgets from #90 have an opaque background in the app's green. To blend them with the wallpaper, each widget should be able to have a more or less transparent background.

Tracked as GitHub issue #92: https://github.com/crmartinez930130/food-tracker-ai/issues/92

## What Changes

- **A widget configuration screen** with a "Background transparency" slider from 0% (opaque, as today) to 100% (no background), a live preview over the real wallpaper, and "Done". It opens:
  - when a widget is added on Android 11 or older, where configuration is mandatory;
  - on Android 12+, from the widget's long-press "Widget settings". There it's optional: a new widget starts opaque.
- **Each widget keeps its own value,** across reboots and app updates, and it goes away with the widget.
- **Both widgets use it.** Only the background's opacity changes: buttons, progress bars and texts keep their colors.
- **"Today's calories" reads at any transparency:**
  - it's always dark with white text, larger type and the key figures in bold;
  - its opaque background is the dark scheme's, in light mode too.

  "Quick actions" keeps the theme's colors.
- Texts in Spanish and English.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `home-widgets`: the widgets' background transparency can be set per widget. `home-widgets` is still an open change (`add-home-widgets`, waiting for its phone check), so this change adds a requirement to it and should be archived after it.

## Impact

- **Android (`androidMain`):**
  - a configuration activity in Compose, with a theme that shows the wallpaper;
  - `android:configure` and `widgetFeatures` in both widget XMLs;
  - a manifest entry;
  - both widgets read the value from their Glance state.
- **Common:**
  - pure helpers for the transparency (clamping, steps, alpha), with tests;
  - strings in both languages;
  - `FoodTrackerTheme` gains an option to leave its background transparent.
- **Backend:** no change.
