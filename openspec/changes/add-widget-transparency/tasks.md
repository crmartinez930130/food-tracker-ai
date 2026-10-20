## 1. Logic

- [x] 1.1 Add `clampTransparency`, `snapTransparency` and `backgroundAlpha` in common code (D2), with tests.

## 2. Widgets

- [x] 2.1 Read `background_transparency` from each widget's Glance state and draw the background with `transparentBackground(...)`, a day/night provider with alpha (D1, D3).
- [x] 2.2 Add `android:configure` and `widgetFeatures` to both provider XMLs (D5).

## 3. Configuration screen

- [x] 3.1 Add `WidgetConfigActivity` (D4):
  - only this app's widget ids;
  - `RESULT_CANCELED` first;
  - a wallpaper theme, and `FoodTrackerTheme(transparentBackground = true)`;
  - a preview with today's numbers;
  - the slider in steps of 5;
  - "Done", which saves, updates and returns `RESULT_OK`.

  Add the manifest entry, with the exported `APPWIDGET_CONFIGURE` filter, and the strings in both languages.

- [x] 3.2 Give "Today's calories" its fixed look (D6):
  - the dark background with white text;
  - larger, bold key figures;
  - the compact macros line.

  Do the same in the configuration preview, and update the picker preview images.

## 4. Verification

- [x] 4.1 Run `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 4.2 On the emulator:
  - a new widget is opaque;
  - "Widget settings" opens the screen over the wallpaper;
  - the preview follows the slider;
  - "Done" applies it to that widget only;
  - back cancels;
  - the value survives an app update and a reboot;
  - light and dark, Spanish and English, both widgets.

  Restore the emulator afterwards.
- [x] 4.3 On the phone, with the user's OK to install: the same main checks.
