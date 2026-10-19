## Context

An adaptive icon is drawn on a 108dp canvas. Launchers show only the central 72dp, masked to their own shape (a circle on Pixel, a squircle on Samsung), and may shift the layers for parallax. Only the central circle of 66dp diameter (radius 33dp) is guaranteed to stay visible. On Android 13+, when the user turns on themed icons, the launcher ignores both color layers. It uses the `monochrome` layer's alpha and tints it with the wallpaper's colors.

The chosen drawing, on the 108-unit canvas:
- the apple body, about x 33–75 and y 38–79;
- a stem and a leaf reaching y ≈ 29;
- a four-point sparkle centered at (57, 59) with radius 8.

Every point stays within radius ≈ 29.4 of the center; the farthest is the leaf tip at (71, 30).

## Goals / Non-Goals

**Goals:**
- An app-specific, crisp icon at every density.
- Correct in circle and squircle masks.
- A good themed version.

**Non-Goals:**
- Notification icon, iOS, Play Store artwork.
- An animated splash icon.

## Decisions

### D1: One vector serves as foreground and monochrome
The sparkle is a hole cut out of the apple, not a light shape painted on top: one path holds the apple body and the sparkle, with `fillType="evenOdd"`. The background shows through the hole. The background is the same light green the proposal used for the sparkle, so the color icon looks exactly like the proposal.

Because the drawing has only one color plus holes, the same drawable is also correct as the `monochrome` layer, whose alpha is all that matters. One file instead of two, and they can't drift apart.

*Alternative considered:* a separate monochrome drawable. Rejected because it would duplicate the geometry for no visual difference.

### D2: Fixed brand colors, not theme colors
- Background: `#B0F1C3`, the light scheme's `primaryContainer`.
- Apple: `#2C6A45`, the light scheme's `primary`.

Launcher icons don't follow the app's light/dark choice, and the launcher draws them over any wallpaper. The light background with a dark apple keeps contrast on both light and dark home screens. On Android 13+, the themed version is the one that adapts to the system.

### D3: `mipmap-anydpi`, `icon` and `roundIcon`
- `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` are the same adaptive icon. `minSdk` is 26, so no `-v26` qualifier and no PNG fallbacks.
- `roundIcon` is declared because some launchers still read it.
- The background is a color resource (`@color/ic_launcher_background`), not a drawable, so it scales trivially.

### D4: The launch screen keeps its current setup
On Android 12+, the system launch screen shows the launcher icon by default, over the window background that `themes.xml` already sets for light and dark. The icon arrives there without changes. The adaptive background keeps the icon's own circle readable over the dark launch background.

## Risks / Trade-offs

- **[A launcher with an unusual mask cuts the leaf]** → The whole drawing stays within radius ≈ 29.4, below the guaranteed 33.
- **[The sparkle hole is too small at small sizes]** → At 48dp the sparkle measures about 7dp across, as it did in the proposal's 48 px preview, where it read clearly. Checked on the phone.
