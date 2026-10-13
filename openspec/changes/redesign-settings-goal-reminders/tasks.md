## 1. Shared pieces

- [x] 1.1 Add `SettingsGroup` and `SettingsRow` (D1) and the pure `choiceIndex` (D2). Move the meal type icon style to `MealTypeLabel.kt` with a `MealTypeIcon` composable (D5), and use it in `DayContent`. Verify with `choiceIndex` tests.

## 2. Resources

- [x] 2.1 Add the `notifications` and `palette` icons. Add `settings_language_phone`, `settings_general` and `goals_macros_section` in both languages, and remove `settings_language_system` (D6). Verify with `StringResourcesTest`.

## 3. Screens

- [x] 3.1 Rebuild Options (D2): language and theme connected buttons, the General group's rows, and the Server card.
- [x] 3.2 Rebuild Reminders (D3): a card per meal with the icon, name, large time and switch, plus the blocked-notifications card.
- [x] 3.3 Rebuild Goal (D4): the default hint card, the two groups, and the pill save button. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 4. Verification

- [x] 4.1 On the emulator, in light and dark and in Spanish and English:
  - choose each language (including Teléfono with the emulator in English) and each theme; they apply at once and persist after restarting;
  - the server is saved, tested (OK and failure) and restored;
  - the reminders screen opens from General; a reminder turns on with the permission prompt, its time changes and persists, and it turns off;
  - the goal: an invalid value shows its error, saving works, and back returns to Home with the new goal;
  - Home's meal groups still show their icons;
  - the design sample row opens.

  Restore the preferences and goal afterwards.
