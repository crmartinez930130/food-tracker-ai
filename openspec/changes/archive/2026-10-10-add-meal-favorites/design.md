# Design

## Context

- **Data model:** each meal is a row in `meals` (totals, `notes`, `meal_type`, `occurred_at`), with its foods in `meal_items`.
- **Routes:** `routes/meals.ts` registers `GET /`, `GET /:id`, `PATCH /:id` and `DELETE /:id`. `PATCH` is validated by `UpdateMealRequestSchema`, where every field is optional and only the provided ones change, following the `notes`/`mealType` pattern in `updateMeal`.
- **Frontend:**
  - Home (`ui/home/HomeScreen.kt`) lists the day's meals in `MealRow`, with a Delete button.
  - `CaptureScreen` offers camera or gallery and calls `analyzeMealPhoto`.
  - `AppState.openReview(MealAnalysis)` opens `ReviewScreen`, which saves through `POST /api/meals`.

See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Re-logging a repeated meal takes two taps (capture button, then the favorite) plus confirm, and costs no API call.
- Reuse the existing review and save flow, so a re-logged meal is indistinguishable from any other new meal.

**Non-Goals:**
- Named favorites or "templates" that are edited independently of a logged meal.
- Search, sorting or reordering of favorites.
- Suggestions of frequent meals that weren't explicitly starred.

## Decisions

**1. A favorite is a flag on the meal row, not a separate templates table.**
`meals.is_favorite` is defined as `integer` with drizzle `mode: "boolean"`, `NOT NULL DEFAULT false`. The migration is additive, and existing rows read as false.
- *Alternative:* a `favorites` table that copies the items into a reusable template. That makes favorites survive deleting the original, and lets them carry their own name. It is more schema and more endpoints than the issue asks for, and this app logs few meals per day, so the copy-on-relog approach covers the need.
- *Consequence:* deleting a favorite meal also removes it from favorites. It is accepted, and appears in the Risks below.

**2. `PATCH /api/meals/:id` takes `isFavorite`.** It is added to `UpdateMealRequestSchema` as an optional boolean, and `updateMeal` keeps the existing value when the field is omitted (`!== undefined`). No dedicated endpoint, as the issue states.

- *Client side (correction found during implementation):* the app sends the toggle as its own body, `FavoriteUpdateRequest(isFavorite)`, not as `UpdateMealRequest`. The HTTP client serializes with `encodeDefaults = true`, so an `UpdateMealRequest` with only `isFavorite` set would also send `"items": null` (rejected by the schema) and `"notes": null` / `"mealType": null` (which would clear them).

**3. `GET /api/meals/favorites` is registered before `GET /:id`.** Otherwise Express would route `/favorites` into `/:id`, where `parseId` fails with a 400. `listFavoriteMeals(db)` selects `is_favorite = 1 ORDER BY occurred_at DESC` and reuses the same item loading and `toMealEntry` mapping as the date listing. A route test pins down the ordering.

**4. Re-logging is a pure client-side mapping into the existing review flow.**
`MealEntry.toReviewAnalysis()` (commonMain, unit-tested) builds a `MealAnalysis` from the favorite's items, totals and notes. `CaptureScreen` passes it to the same `onAnalyzed` callback the photo path uses, so `AppState.openReview` and `ReviewScreen` are unchanged.
- The review's meal-type default stays time-based (#4). The same food can be a snack or a breakfast.
- Saving goes through `POST /api/meals`. That creates a new row with `occurredAt` set to now and `isFavorite` false by default, which gives "new entry, original unchanged, no duplicate favorite" without any special code.
- The no-analysis guarantee is structural: this path never calls `analyzeMealPhoto`. I verify it on the emulator by checking that the backend logs no `meal_photo_analyzed` entry.

**5. UI placement.**
- **Home:** `MealRow` gets a star `IconButton` before "Delete". It uses the filled star when the meal is a favorite and the outlined one otherwise. Its `contentDescription` is "Add to favorites" or "Remove from favorites". It calls `setFavorite` and then reloads the day.
- **Capture screen:** in its idle state, below "Take photo" and "Choose from gallery", there is a "Favorites" header and the list of favorites. Each row shows the food names and kcal. Tapping a row re-logs it, and a filled-star button removes it.
  - When there are no favorites, a one-line hint is shown instead.
  - The list loads when the screen opens. If loading fails, a short error is shown and the photo options keep working.
- **Icons:** Material Symbols `star`, as the Outlined 24px and the Outlined `fill1` 24px variants, added the same way as in #1 and #3 (tint stripped, literal fill, source/license header).

## Risks / Trade-offs

- **[Deleting a favorite's original meal removes the favorite]** → This follows from Decision 1. On Home it is visible, because the star sits next to Delete on the same row. If it turns out to matter in practice, a later issue can move favorites to their own table without changing the re-log flow.
- **[Favorites show only as food lists, with no name]** → They are good enough to recognise a repeated meal. Named favorites are a non-goal.
- **[The capture screen grows]** → The favorites list only appears in the idle state and scrolls with the screen. The camera and confirm states are unchanged.

## Migration Plan

`npx drizzle-kit generate` produces `ALTER TABLE meals ADD is_favorite integer DEFAULT false NOT NULL`, which `openDatabase` applies on startup. No backfill is needed. Rollback means dropping the column, which is safe because nothing else depends on it.
