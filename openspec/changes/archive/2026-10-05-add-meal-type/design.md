# Design

## Context

See proposal.md for motivation. Relevant current state: `meals` table (`backend/src/db/schema.ts`) has no categorization field; `backend/src/db/meal-repository.ts` reads/writes rows via Drizzle; `backend/src/routes/schemas.ts` validates request bodies with Zod; the Android app's `ReviewScreen`/`HistoryScreen` (`frontend/composeApp/src/commonMain/.../ui/`) render a flat chronological list with no concept of meal type.

## Goals / Non-Goals

**Goals:**
- Thread an optional `mealType` through save, edit, and retrieve without breaking existing data or clients that omit it.
- Let the Android UI suggest a sensible default and group the day's entries by type.

**Non-Goals:**
- No historical backfill of `mealType` for existing rows — they stay untyped (`null`) unless the user edits them.
- No enforcement that a day has exactly one of each type, or any validation beyond the four allowed values — this is a label, not a scheduling system.
- No change to how totals/summaries are computed (`nutrition-summary` capability is untouched).

## Decisions

### 1. Column type: SQLite `text` with a Zod/TS enum, not a DB-level CHECK constraint
**Choice:** `meal_type` is a nullable `text` column; validity (`breakfast`/`lunch`/`dinner`/`snack`) is enforced at the Zod schema layer (`backend/src/routes/schemas.ts`), matching how `confidence` is already validated on meal items.

**Why:** Consistent with the existing pattern in this codebase (`mealItems.confidence` uses the same approach — see `backend/src/db/schema.ts`) rather than introducing a new validation mechanism for one field.

### 2. Default-suggestion logic lives entirely in the Android client
**Choice:** The "suggest breakfast before 11:00, lunch 11:00-15:00, snack 15:00-19:00, dinner after 19:00" logic is a `ReviewScreen` UI default only — the backend has no opinion on it and never derives a type from a timestamp itself.

**Why:** It's a presentation nicety tied to the device's local clock, not a behavior contract. Keeping it client-side avoids the backend needing timezone awareness for something that doesn't affect persisted data (the user can always override the suggestion before saving).

### 3. Grouping is a frontend concern; the API stays a flat list
**Choice:** `GET /api/meals` keeps returning a flat `MealEntry[]` with `mealType` on each entry (nullable). `HistoryScreen` groups client-side by `mealType`, with a single "Other" bucket for entries where it's `null`.

**Why:** Restructuring the API response into nested groups would be a real breaking change for a cosmetic concern, and the existing endpoint already gives the client everything it needs to group locally.

## Risks / Trade-offs

- **[Risk]** Mixed data (some entries typed, some not) could look inconsistent in the grouped view → **Mitigation:** an explicit "Other" group for untyped entries, never silently dropped or merged into a guessed type.
- **[Trade-off]** No backfill means existing rows stay in "Other" until manually edited — acceptable since the only existing data is the user's own test entries from development, not real production history.

## Migration Plan

Standard `drizzle-kit generate` + the existing startup migration runner (`backend/src/db/client.ts` already calls `migrate()` on every boot) — no manual steps. Adding a nullable column is backward-compatible; no rollback procedure needed beyond reverting the migration file if something goes wrong before it's deployed anywhere else.
