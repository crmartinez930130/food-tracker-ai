## Context

Both widgets draw their root with `background(GlanceTheme.colors.widgetBackground)`. That's a day/night `ColorProvider` from glance-material3, so the system resolves light or dark itself. A Glance widget can keep per-instance state through its `stateDefinition` (Preferences DataStore by default). It's read in composition with `currentState()`, written with `updateAppWidgetState(context, glanceId)`, and deleted by Glance when the widget is removed. Android starts a widget's configuration activity through the `android:configure` attribute, with `EXTRA_APPWIDGET_ID`. On Android 12+, `widgetFeatures="reconfigurable|configuration_optional"` makes it optional at placement and reachable later from the widget's long-press menu.

## Goals / Non-Goals

**Goals:**
- A per-widget transparency from 0% to 100%, chosen with a live preview.
- New widgets look as they do today.

**Non-Goals:**
- Other per-widget options.
- A global setting.
- Text shadows, which Glance doesn't support.

## Decisions

### D1: The value lives in each widget's Glance state
- **Storage:** an `Int` key `background_transparency` (0..100) in the default `PreferencesGlanceStateDefinition`. A missing key means 0, so existing and new widgets stay opaque.
- **Lifetime:** Glance deletes the state with the widget, and the DataStore file survives reboots and app updates.
- **Reading:** both widgets read it in composition with `currentState()`.
- **Applying:** the configuration activity writes it with `updateAppWidgetState`, then calls `update(context, glanceId)`. That reloads the state of a running session too.

*Alternative considered:* our own SharedPreferences keyed by `appWidgetId`. Rejected because it needs manual cleanup on removal, and Glance state already does this.

### D2: Pure helpers in common code
- `clampTransparency(percent)` keeps a value within 0..100, so a corrupt or old value can't break drawing.
- `snapTransparency(raw: Float)` rounds the slider to steps of 5.
- `backgroundAlpha(percent)` returns `(100 - percent) / 100f`.

All three are tested in `commonTest`.

### D3: A day/night background with alpha
`transparentBackground(provider, alpha, context)` returns `ColorProvider(day = day.copy(alpha), night = night.copy(alpha))`:
- `widgetBackground` from glance-material3 is a `DayNightColorProvider`, whose `day` and `night` colors are public, so it takes them directly;
- any other provider is resolved for the current context, which is a fallback only.

This way a transparent background still switches with the system mode, as the opaque one does, and the exact color glance-material3 computes stays the source of truth.

The activity's preview uses the same two colors, so it matches the widget.

### D4: One configuration activity for both widgets
`WidgetConfigActivity` works out which widget it's configuring from `AppWidgetManager.getAppWidgetInfo(id).provider`.

**Validation.** It is exported, as the launcher needs, so it accepts only ids whose provider is one of this app's two receivers. Anything else finishes at once. It starts with `RESULT_CANCELED`, so backing out of the first configuration on Android 11 or older doesn't place the widget.

**Window.** Its theme has `windowShowWallpaper` and a transparent window background, so the real wallpaper is behind the preview. `FoodTrackerTheme(transparentBackground = true)` keeps Compose's root surface from covering it.

**Content**, in a bottom card:
- a title;
- the preview, a Compose replica of the widget with today's real numbers from `todayWidgetStates`, over the wallpaper;
- the slider with its percentage;
- "Done".

**Theme and language.** It follows the system's light or dark mode, because that's what the widget does, and the app's chosen language (`AppEnvironment`).

"Done" writes the value (D1), updates that widget, returns `RESULT_OK` with the id, and finishes.

### D5: Provider XML
Both `appwidget-provider` files gain:
- `android:configure="com.foodtracker.app.widget.WidgetConfigActivity"`;
- `android:widgetFeatures="reconfigurable|configuration_optional"`.

### D6: "Today's calories" is always dark with white text
Added after trying it on the emulator: with the theme's colors, the dark text vanished over dark wallpapers once the background was transparent. The user chose white text at all times, and an opaque background that stays dark so white reads at 0% too.

`TodayWidgetLook` fixes the look:
- **background:** the dark scheme's widget background (`widgetBackground.night`), the same in light and dark mode;
- **text:** white for the main figures and white at 85% for the rest;
- **bars and the "over" accent:** the dark scheme's primary, tertiary and error colors on a translucent white track.

The type is larger and the key figures are bold:
- title 15 sp, bold;
- calories 36 sp, bold;
- target line 14 sp, with the calories left bold;
- macros and water 12.5–13 sp, medium.

The macros line drops the spaces around "/" ("76/130 g") so the three fit one line.

"Quick actions" keeps the theme. The configuration preview uses the same look.

## Risks / Trade-offs

- **[Unreadable text at high transparency on some wallpapers]** → "Today's calories" uses white, bold figures (D6), which read over most wallpapers. On very light ones, white can still be faint: that's the user's choice, and the preview shows it over their own wallpaper before applying.
- **[`windowShowWallpaper` with edge-to-edge]** → The activity is edge-to-edge with a transparent window. Verified on the emulator.
- **[A running Glance session doesn't reload state on update]** → Checked on the emulator. If it doesn't reload, the fallback is the existing redraw tick, which restarts the content flow.
