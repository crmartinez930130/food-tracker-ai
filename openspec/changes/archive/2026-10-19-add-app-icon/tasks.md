## 1. Icon

- [x] 1.1 Add `ic_launcher_foreground.xml` (one vector: the apple body and the sparkle hole in one `evenOdd` path, plus the stem and the leaf; D1), the `ic_launcher_background` color, and `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` with background, foreground and monochrome (D1, D3). Point `android:icon` and `android:roundIcon` at them. Verify that `./gradlew :composeApp:assembleDebug` passes.

## 2. Verification

- [x] 2.1 On the emulator:
  - the launcher icon is whole with the default mask;
  - with themed icons on, it's tinted and the sparkle shows;
  - Recents and the launch screen show it, in light and dark.
- [x] 2.2 On the phone (Samsung squircle), with the user's OK to install: the same checks.
