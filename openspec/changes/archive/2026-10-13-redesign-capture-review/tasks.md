## 1. Logic and component

- [x] 1.1 Add `toggledPortion` and `mealTotals` (D2, D4). Extend `ConnectedButtonGroup` with `selectedIndex = -1` and `showCheck` (D2). Verify with tests:
  - the portion's select, switch and clear;
  - the totals' sums and an empty list;
  - review numbers with a decimal comma, a dot, blank and text.

## 2. Resources

- [x] 2.1 Add the `photo_camera`, `photo_library` and `close` icons, and the new strings in both languages. Remove `review_total` and `review_confirm_save` (D5). Verify with `StringResourcesTest`.

## 3. Screens

- [x] 3.1 Rebuild Capture's layout (D1): the top bar with close, the photo tiles, the preview with the retake/choose-again tonal button, the portion connected buttons, the pill analyze button, the analyzing indicator with caption, and the horizontal favorite cards.
- [x] 3.2 Rebuild Review's layout (D3): the top bar with close (discard prompt), the meal type connected buttons, the food cards, the notes, the total card, and the fixed "Guardar · N kcal" pill. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 4. Verification

- [x] 4.1 On the emulator, in light and dark and in Spanish and English, with the backend running:
  - analyze with a gallery photo (`real_meal.jpg`);
  - analyze with only a description;
  - re-log a favorite from its card, and unmark one;
  - select and clear a portion;
  - in Review, change the meal type, edit a food's kcal (the total and the pill update), remove a food;
  - close → keep editing → close → discard;
  - save and see the meal on Home;
  - back while analyzing is ignored.

  Clean the test data afterwards.
