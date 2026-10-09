# Tasks

## 1. Backend: schema and migration

- [x] 1.1 Add a `goals` table to `backend/src/db/schema.ts` (`id` integer PK, `target_calories`, `target_protein_grams`, `target_fat_grams`, `target_carbs_grams` as real, `updated_at` text) and export its row types; verify `npx drizzle-kit generate` produces a new migration containing only `CREATE TABLE goals`
- [x] 1.2 Verify the migration applies cleanly: `npm test` still passes (every test opens a fresh `:memory:` DB through `openDatabase`, which runs all migrations)

## 2. Backend: domain and repository

- [x] 2.1 Add a `DailyGoal` type (four targets + `isDefault`) and the `DEFAULT_DAILY_GOAL` constant (2000 kcal / 125 g P / 67 g F / 225 g C) in `backend/src/domain/goals.ts`; verify `npm run typecheck` passes
- [x] 2.2 Add `backend/src/db/goals-repository.ts` with `getDailyGoal(db)` (returns the stored row with `isDefault: false`, or the defaults with `isDefault: true` without writing them) and `saveDailyGoal(db, input)` (upsert on the fixed id, sets `updated_at`); verify with `backend/test/db/goals-repository.test.ts` covering: empty table → defaults, first save, replacing an existing goal (still exactly one row)

## 3. Backend: REST API

- [x] 3.1 Add `UpdateGoalsRequestSchema` to `backend/src/routes/schemas.ts` (four required numbers, calories > 0, macros >= 0); verify in `backend/test/routes/schemas.test.ts` that a valid body passes and a missing field, non-number, zero/negative calories and negative macros are rejected
- [x] 3.2 Add `backend/src/routes/goals.ts` (`createGoalsRouter`) with `GET /` and `PUT /` (invalid body → `ValidationError`), and mount it at `/api/goals` in `backend/src/app.ts`; verify with `backend/test/routes/goals.test.ts` covering: GET with no goal → 200 defaults with `isDefault: true`, PUT → 200 saved goal, GET after PUT reflects it with `isDefault: false`, a second PUT replaces it, invalid PUT → 400 and the stored goal is unchanged

## 4. Frontend: data layer

- [x] 4.1 Add `@Serializable` `DailyGoal` (four targets + `isDefault`) and `UpdateGoalsRequest` (four targets) to `frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/data/Models.kt`, and `getGoals()` / `updateGoals(request)` to `FoodTrackerApiClient`; verify `:composeApp:compileDebugKotlinAndroid` compiles
- [x] 4.2 Verify with `commonTest` cases in `FoodTrackerApiClientTest`: deserializing a default goal response, deserializing a configured one, `updateGoals` sending a `PUT` with the JSON body, and a 400 response surfacing as `ApiException` with the backend's message

## 5. Frontend: goals screen

- [x] 5.1 Add `GoalsScreen` (`frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/ui/GoalsScreen.kt`): load the goal on open, show four numeric fields, a "using default goal" hint when `isDefault`, and a Save button with saving/error states that keeps the user's edits when saving fails; verify it compiles
- [x] 5.2 Add a `GOALS` entry to `MainTab` and a third `NavigationBarItem` in `MainScreen` that shows `GoalsScreen`; verify it compiles and `:composeApp:testDebugUnitTest` passes

## 6. End-to-end verification

- [x] 6.1 On the emulator against the real backend: open Goals with an empty table → defaults and hint shown; save new values → success; switch tabs and come back → saved values shown; kill and relaunch the app → saved values still shown; enter `0` calories → error shown and edits kept. Clean up afterwards by restoring the goal row to its pre-test state

## 7. Repo housekeeping (same PR, not part of the capability)

- [x] 7.1 Update `CONTRIBUTING.md`: the desktop app's PR monitor/auto-fix isn't available in this setup, so the dev session itself waits for the reviewer in a background process after opening the PR, and must stay open while the PR is in review; verify the section reads correctly
