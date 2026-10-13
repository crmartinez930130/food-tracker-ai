## 1. Pure helpers

- [x] 1.1 Add `ui/home/HomeHeader` helpers: `greetingFor(hour)` (D2) and `goalPercent(consumed, target)` (D4). Add `millisUntilNextMinute(now)` to `util/Dates.kt` (D1). Verify with tests:
  - the greeting boundaries 4/5, 11/12, 19/20 and 23/0;
  - the percentage rounding, values over 100 and a zero or negative target giving null;
  - the time to the next minute at :00.000, mid-minute and :59.999.

## 2. Resources

- [x] 2.1 Add the Material Symbols drawables (`bakery_dining`, `lunch_dining`, `dinner_dining`, `cookie`, `restaurant`, `delete`) in the same cleaned form as the existing icons. Add the strings and string arrays in both languages:
  - the greetings and the date template;
  - `weekday_names` and `month_names`;
  - the delete content description.

  Remove `day_today`, `day_yesterday`, `previous_day` and `next_day`. Extend `StringResourcesTest` to string arrays (same names, same item counts). Verify with `StringResourcesTest`.

## 3. Day content and Home

- [x] 3.1 Add `BarSegment.detailColor` and restyle `WaterCard` as a `tertiaryContainer` card (D6). Verify that `SegmentedBarTest` still passes and that the design sample still renders (4.1).
- [x] 3.2 Move the day's loading, state and actions from `HomeScreen` into `ui/home/DayContent.kt` (D7). Build in it:
  - the calories card with `BigNumber` and the clickable percentage `ShapeBadge` (D4);
  - the default-goal hint;
  - the segmented macro bar with the over-target legend (D5);
  - the water card;
  - the meal-group cards with type icons, group kcal, favorite and icon delete (D6).
- [x] 3.3 Rewrite `HomeScreen` as the header (greeting, written-out date, Options and goal actions) plus `DayContent` for today. Today comes from `rememberCurrentMinute()`, a minute loop plus a re-read on resume (D1).
- [x] 3.4 Remove `homeDate` from `AppState`, `MainScreen` and `App`. Make saving return to `openMain(HOME)`. Remove `dayLabel`/`DayLabel` and their tests (D8). Add the new role pairs to `ColorContrastTest`. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 4. Verification

- [x] 4.1 Emulator, light and dark, Spanish and English, with the backend running and test data:
  - the header shows the greeting and the date;
  - the calories, percentage, macros and water match the data;
  - over-goal and over-target are distinguished;
  - the badge and the header action open the goal, and back returns to Home;
  - Options opens;
  - favorite, delete, add a glass and undo work;
  - the default-goal hint shows with no goal saved;
  - meals are grouped with their icons;
  - changing the emulator's date or time zone past midnight with the app open switches Home to the new day;
  - the design sample still renders.

  Clean the test data afterwards.
