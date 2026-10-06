# Tasks

## 1. Backend: schema and migration

- [x] 1.1 Add nullable `mealType` column to the `meals` table in `backend/src/db/schema.ts` (text, same pattern as `mealItems.confidence`) and export its TS union type; verify `npx drizzle-kit generate` produces a new migration file with no errors
- [x] 1.2 Apply the migration against the local dev database and verify existing rows still read correctly with `mealType` as `null` (`sqlite3 backend/data/food-tracker.sqlite ".schema meals"` shows the new column; existing `GET /api/meals` rows still return successfully)

## 2. Backend: domain, validation, repository

- [x] 2.1 Add `mealType?: MealType | null` to `SaveMealInput` and `UpdateMealInput` in `backend/src/domain/meal.ts`, and to `MealEntry`; verify `npm run typecheck` passes
- [x] 2.2 Add `mealType` (nullable, optional, validated against the 4 allowed values) to `SaveMealRequestSchema` and `UpdateMealRequestSchema` in `backend/src/routes/schemas.ts`; verify with a unit test that an invalid value is rejected and a valid one passes
- [x] 2.3 Thread `mealType` through `createMeal`, `updateMeal`, and the row-to-`MealEntry` mapping in `backend/src/db/meal-repository.ts`; verify with repository tests covering: save with a type, save without a type, edit a type, read back an entry saved before this change (no type) without error

## 3. Backend: REST API

- [x] 3.1 Verify `POST /api/meals` accepts and persists `mealType`, and `PATCH /api/meals/:id` accepts and updates it, with integration tests covering both the present and omitted cases
- [x] 3.2 Verify `GET /api/meals` and `GET /api/meals/:id` include `mealType` (or `null`) in every returned entry, with a test asserting the field is present in the response shape

## 4. Frontend: data layer

- [x] 4.1 Add a `MealType` enum and `mealType: MealType? = null` field to `MealItem`'s sibling `MealEntry`/`SaveMealRequest`/`UpdateMealRequest` models in `frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/data/Models.kt`; verify `:composeApp:compileDebugKotlinAndroid` compiles
- [x] 4.2 Verify `FoodTrackerApiClient` serializes/deserializes `mealType` correctly with a `commonTest` case (mock response including and omitting the field)

## 5. Frontend: review screen selector

- [x] 5.1 Add a meal-type selector to `ReviewScreen` (`frontend/composeApp/src/commonMain/.../ui/ReviewScreen.kt`), defaulting to a time-of-day suggestion (before 11:00 breakfast, 11:00-15:00 lunch, 15:00-19:00 snack, after 19:00 dinner), editable before confirming
- [x] 5.2 Verify the selected type is included in the `SaveMealRequest` sent to `POST /api/meals` when confirming a save

## 6. Frontend: grouped listing

- [x] 6.1 Update `HistoryScreen` to group a day's entries by `mealType` into labeled sections (Breakfast/Lunch/Dinner/Snack), with an "Other" section for entries with no type, instead of a flat chronological list
- [x] 6.2 Verify `./gradlew :composeApp:assembleDebug` builds and `:composeApp:testDebugUnitTest` passes

## 7. End-to-end verification

- [x] 7.1 On the emulator against the real backend: save a meal, confirm the suggested type matches the current time, change it, save, and verify it appears under the correct group in the history screen; save a second meal with a different type and verify both groups render correctly
