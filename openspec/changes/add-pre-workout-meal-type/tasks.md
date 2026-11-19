# Tasks

## 1. Backend

- [x] 1.1 Add `"pre_workout"` to `MEAL_TYPES` (`backend/src/db/schema.ts`) and to the `MealType` union in `backend/src/domain/meal.ts`; `npm run typecheck` passes
- [x] 1.2 Tests: `schemas.test.ts` accepts `pre_workout` on save and update and still rejects an unknown type; `meals.test.ts` saves a `pre_workout` meal and reads it back; `csv-export.test.ts` writes `pre_workout` in `meal_type`. `npm test` passes
- [x] 1.3 Update the `meal_type` values listed in any backend API docs/README that enumerate meal types (grep `breakfast` under `backend/` and `docs/`)

## 2. Shared model and labels

- [x] 2.1 Append `@SerialName("pre_workout") PRE_WORKOUT` at the end of `MealType` (`frontend/shared/.../data/Models.kt`) and add a serialization round-trip case to `FoodTrackerApiClientTest`; `./gradlew :shared:allTests` (or the module's JVM/Android unit tests) passes
- [x] 2.2 Add strings `meal_type_pre_workout` ("Preentreno" / "Pre-workout") in `values` and `values-en`, the `ic_fitness_center.xml` drawable (Material Symbols), and the `PRE_WORKOUT` branches in `MealTypeLabel.kt` (`labelRes`, `iconStyle()` on `tertiaryContainer`); the app module compiles
- [x] 2.3 Add `MEAL_TYPE_DISPLAY_ORDER` (pre-workout, breakfast, lunch, dinner, snack) in `MealTypeLabel.kt`, use it for Home's `MEAL_TYPE_SECTION_ORDER` (plus `null`) in `home/DayContent.kt`, with a unit test asserting the order

## 3. Review screen

- [x] 3.1 Make `suggestedMealType(hour)` internal, add the 07:00–08:59 → `PRE_WORKOUT` branch, and unit-test the boundaries in `MealTypeOrderTest`
- [ ] 3.2 Drive the selector from `MEAL_TYPE_DISPLAY_ORDER` (index ↔ type mapping, not `ordinal`), and add optional per-option icons to `ConnectedButtonGroup` (icon above `labelSmall`, 56 dp tall) used by the review selector; existing callers unchanged; the project has no Compose UI test setup, so the icon variant is covered by `MealTypeOrderTest` (order ↔ index mapping) and the 3.3 emulator check
- [x] 3.3 Emulator check (Pixel 9 and a 360 dp wide AVD, Spanish): five options with full labels; a meal captured at 07:30 preselects Preentreno; choosing Desayuno and saving stores breakfast. Verified with screenshots on the emulator (described in the PR)

## 4. Reminders

- [x] 4.1 Add `MealReminder(PRE_WORKOUT, 7, 15)` first in `ReminderSettings.DEFAULT`, `reminder_title_pre_workout` ("¿Ya registraste tu preentreno?" / "Did you log your pre-workout?") and the time-dialog string (es/en), and the `PRE_WORKOUT` branches in `reminderTitle()` and `RemindersScreen`
- [x] 4.2 Tests in `ReminderSettingsTest`: decoding settings stored without pre-workout keeps lunch's time/on state and adds pre-workout off at 07:15; `shouldShowReminder(PRE_WORKOUT, …)` is true with only a breakfast logged and false with a pre-workout logged
- [x] 4.3 Emulator check: with the old build, turn lunch on; install the new build and confirm lunch still fires as lunch and pre-workout appears off at 07:15; turn pre-workout on, move the clock past its time and see the Spanish notification

## 5. Integration

- [x] 5.1 Full flow on the emulator against the local backend: save a pre-workout meal, see it grouped first on Home, above Desayuno, export CSV with `pre_workout` (the app has no type-edit UI besides review; editing the type is covered by the backend PATCH tests)
- [x] 5.2 `./gradlew :wearApp:assembleDebug` still builds (shared enum change), and note in the PR that the backend and watch must be redeployed with the phone APK
