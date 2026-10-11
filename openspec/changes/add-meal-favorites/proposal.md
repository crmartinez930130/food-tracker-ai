## Why

Many meals repeat: the same breakfast every day, the usual lunch. Today each one needs a new photo and a new Claude analysis, which is slower for the user and **costs a real API call every time**. Every app we compared lets you re-log a saved meal in a tap. Tracked as GitHub issue #6: https://github.com/crmartinez930130/food-tracker-ai/issues/6

## What Changes

- **Backend:**
  - Meals get an `isFavorite` flag. It defaults to false, so existing meals are unaffected.
  - `PATCH /api/meals/:id` accepts `isFavorite` to mark or unmark a meal.
  - A new `GET /api/meals/favorites` lists the favorite meals regardless of date, most recent first.
  - `isFavorite` is returned on every meal entry.
- **Home:** each meal row gets a star button to mark or unmark it as a favorite.
- **Capture screen ("Log a meal"):** below the photo buttons, a **Favorites** section lists the favorite meals.
  - Tapping one opens the review screen pre-filled with that meal's items and notes, editable as usual, **without calling the analysis service**.
  - Each row also has a star button to remove it from favorites.
- **Re-logging** saves a **new** meal timestamped now, through the existing save path. The new meal isn't itself a favorite, so the favorites list doesn't fill up with copies. The original is left untouched.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `meal-tracking`: adds marking a meal as a favorite and listing favorites. Meal entries now carry an `isFavorite` flag.
- `daily-dashboard`: Home lets the user mark and unmark a meal as a favorite.
- `meal-capture`: the capture screen offers re-logging a favorite as an alternative to a photo, with no analysis call.

## Impact

- **Backend:**
  - New `is_favorite` column plus a migration in `backend/src/db/schema.ts`.
  - Changes to `meal-repository.ts` (mapping, update, new `listFavoriteMeals`), `routes/schemas.ts` and `routes/meals.ts`. The new `/favorites` route is registered before `/:id`.
  - New tests for all of the above.
- **Frontend:**
  - `isFavorite` on `MealEntry`, plus a dedicated `FavoriteUpdateRequest` body for the toggle.
  - `listFavorites()` and `setFavorite(id, Boolean)` in `FoodTrackerApiClient`.
  - Changes to `HomeScreen` and `CaptureScreen`.
  - A pure mapping from a saved meal to the review's starting data, with a unit test.
  - New Material Symbols drawables: `star` (outlined) and `star` filled.
- **API:** additive. Existing clients ignore the new field.
