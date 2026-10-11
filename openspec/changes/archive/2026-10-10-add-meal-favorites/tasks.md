# Tasks

## 1. Backend: data and repository

- [x] 1.1 Add `isFavorite` (`integer("is_favorite", { mode: "boolean" }).notNull().default(false)`) to `meals` in `backend/src/db/schema.ts` and `isFavorite: boolean` to `MealEntry` (+ optional on `UpdateMealInput`) in `domain/meal.ts`; generate the migration with `npx drizzle-kit generate` and verify it only adds the column
- [x] 1.2 In `meal-repository.ts`: map `isFavorite` in `toMealEntry`, keep-or-update it in `updateMeal`, add `listFavoriteMeals(db)` (favorites only, any date, `occurred_at` desc); verify with repository tests: new meal is not favorite, mark/unmark persists, favorites listing spans dates, excludes non-favorites, is ordered newest first, and is empty when none

## 2. Backend: API

- [x] 2.1 Add `isFavorite: z.boolean().optional()` to `UpdateMealRequestSchema`; verify in `schemas.test.ts` that true/false/omitted pass and a non-boolean is rejected
- [x] 2.2 Add `GET /api/meals/favorites` (registered before `/:id`) in `routes/meals.ts`; verify with route tests: PATCH `isFavorite` true then false, GET favorites returns only marked meals from different dates newest first, empty list when none, `/favorites` isn't captured by `/:id`, invalid `isFavorite` → 400 with the meal unchanged, and every meal response includes `isFavorite`

## 3. Frontend: data layer

- [x] 3.1 Add `isFavorite: Boolean = false` to `MealEntry`; add `listFavorites()` and `setFavorite(id, Boolean)` to `FoodTrackerApiClient`, with `setFavorite` sending its own `FavoriteUpdateRequest(isFavorite)` body (not `UpdateMealRequest`, whose `encodeDefaults` nulls would clear notes/meal type or be rejected); verify with `commonTest` cases: favorites deserialization (incl. a meal without the field defaulting to false), `setFavorite` sends `PATCH /api/meals/{id}` with exactly `{"isFavorite": …}`
- [x] 3.2 Add `MealEntry.toReviewAnalysis()` (items, totals, notes) in commonMain; verify with a unit test that items/totals/notes carry over and no id/date/type leaks into the analysis

## 4. Frontend: UI

- [x] 4.1 Add Material Symbols `star` Outlined 24px and its `fill1` variant as `ic_star.xml` / `ic_star_filled.xml` (same adjustments and header as #1); verify the accessors compile
- [x] 4.2 Home `MealRow`: star toggle (filled/outlined, accessible description) calling `setFavorite` then reloading the day; verify it compiles
- [x] 4.3 `CaptureScreen` idle state: "Favorites" section loading `listFavorites()`, rows with foods + kcal (tap → `onAnalyzed(entry.toReviewAnalysis())`) and a remove-star button, empty-state hint, non-blocking load error; verify `:composeApp:assembleDebug` and `:composeApp:testDebugUnitTest` pass

## 5. End-to-end verification

- [x] 5.1 On the emulator against the real backend: log a meal (one real analysis), star it on Home; open the capture screen → it appears under Favorites; tap it → review pre-filled with the same items; edit one quantity and save → Home shows two meals (original unchanged, new one with the edit, only the original starred); backend log shows exactly one `meal_photo_analyzed` (from the first meal, none from the re-log); `GET /api/meals/favorites` returns only the original
- [x] 5.2 On the emulator: remove the favorite from the capture screen → list shows the empty hint and Home shows the original unstarred; star/unstar again from Home works. Clean up all test meals afterwards
