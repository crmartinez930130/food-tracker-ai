# Proposal

## Why

Meal entries are stored and listed with no notion of breakfast/lunch/dinner/snack, so the app can only show them as a flat chronological list. Every comparable app (Cronometer, MyFitnessPal, Lifesum) organizes a day's entries by meal type, and it's the natural grouping once more than one or two meals are logged per day. Implements [GitHub issue #4](https://github.com/crmartinez930130/food-tracker-ai/issues/4).

## What Changes

- `meals` gains an optional `mealType` field (`breakfast` | `lunch` | `dinner` | `snack`), settable on save and on edit, nullable so existing entries remain valid.
- `POST /api/meals` and `PATCH /api/meals/:id` accept an optional `mealType`.
- `GET /api/meals` and `GET /api/meals/:id` include `mealType` in the returned entry (null when not set).
- Frontend: `ReviewScreen` gets a meal-type selector before saving, defaulting to a suggestion based on the current time of day (before 11:00 → breakfast, 11:00-15:00 → lunch, 15:00-19:00 → snack, after 19:00 → dinner — exact boundaries are a UI default, not a backend contract).
- Frontend: the meal listing (history/home) groups entries by meal type when present, falling back to a flat list for entries with no type.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `meal-tracking`: adds the ability to tag a meal entry with a type (breakfast/lunch/dinner/snack), persisted and returned alongside existing fields. Existing save/retrieve/edit/delete behavior is unchanged — this is purely additive.

## Impact

- **Backend**: `backend/src/db/schema.ts` (new column + migration), `backend/src/domain/meal.ts` (domain types), `backend/src/routes/schemas.ts` (Zod validation), `backend/src/db/meal-repository.ts` (read/write the new field).
- **Frontend**: `frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/data/Models.kt` (new field + enum), `FoodTrackerApiClient.kt`, `ReviewScreen.kt` (selector), `HistoryScreen.kt` (grouped rendering).
- **Data**: existing rows in `backend/data/food-tracker.sqlite` are unaffected — the new column is nullable and defaults to absent.
- **No breaking changes**: the field is optional on every request/response path.
