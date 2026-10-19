## Why

The app has no icon of its own. The manifest doesn't declare `android:icon`, so Android shows its generic icon in the launcher, in Recents and on the Android 12+ launch screen. Among three proposals, the user chose the "Apple" icon: a green apple with a leaf, with the analysis sparkle cut out of it, on a light green background.

Tracked as GitHub issue #85: https://github.com/crmartinez930130/food-tracker-ai/issues/85

## What Changes

- **Adaptive icon, all vector:** `minSdk` is 26, so no per-density PNGs are needed. It has three layers:
  - a light green background;
  - the apple in the foreground, inside the 66dp safe zone so no launcher mask cuts it;
  - a monochrome layer, so Android 13+ themed icons tint it like the other apps.
- **Manifest:** `android:icon` and `android:roundIcon` point to the new icon.
- **Launch screen and Recents** pick it up from the launcher icon, with no extra setup.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-appearance`: the app has its own launcher icon, including a monochrome version for themed icons.

## Impact

- **Android:**
  - `AndroidManifest.xml`;
  - new `res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`;
  - new `res/drawable/ic_launcher_foreground.xml`;
  - the background color in `res/values/ic_launcher_background.xml`.
- **Out of scope:**
  - the small notification icon (`ic_notification`);
  - the iOS icon;
  - a 512 px Play Store icon.
- **Common code and backend:** no change.
