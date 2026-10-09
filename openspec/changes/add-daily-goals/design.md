# Design

## Context

The backend is a single-user Express + Drizzle/SQLite service (see `openspec/specs/meal-tracking` and `nutrition-summary`). Routers are built as `create<X>Router(deps)` and mounted in `backend/src/app.ts`. Request bodies are validated with Zod schemas in `backend/src/routes/schemas.ts`, and `ValidationError` is mapped to HTTP 400 there. The schema evolves through drizzle-kit migrations that are applied on startup. The frontend is a Compose Multiplatform app whose `MainScreen` has a bottom `NavigationBar` with two tabs (History, Summary). It talks to the backend only through `FoodTrackerApiClient`.

## Goals / Non-Goals

**Goals:**
- Give the API a goal shape that issue #3 (progress ring) can consume as is.
- Keep the change additive. No existing endpoint, table or screen changes behavior.

**Non-Goals:**
- Showing progress against the goal in Summary or History. That is issue #3.
- Deriving macros from calories, or checking that the macros add up to the calorie target. The user enters all four values freely.
- Goal history or per-day goals.

## Decisions

**1. A single-row `goals` table, not a key-value settings table or a column on some other table.**
The table has columns `id` (integer primary key, always `1`), `target_calories`, `target_protein_grams`, `target_fat_grams`, `target_carbs_grams` (real) and `updated_at` (text, ISO). The upsert is `INSERT ... ON CONFLICT(id) DO UPDATE`. This keeps the typed, real-valued columns the rest of the schema uses, and lets Drizzle infer the types.
- *Alternative:* a generic `settings(key, value)` table. Values would be stored as text and parsed on every read, and it is a bigger abstraction than this one feature needs.
- *Alternative:* per-day rows. This is history, which is out of scope.

**2. Defaults live in the backend and the response carries `isDefault`.**
`GET /api/goals` always returns `{ targetCalories, targetProteinGrams, targetFatGrams, targetCarbsGrams, isDefault }`. When the table is empty, the defaults come from a backend constant and nothing is written to the database. The defaults are 2000 kcal split 25/30/45 % protein/fat/carbs: 125 g protein, 67 g fat (rounded from 66.7) and 225 g carbs. Keeping them server-side means every client (the app now, a widget later) agrees on the same defaults. The `isDefault` flag lets the UI suggest setting a goal.
- *Alternative:* return `null` or 404 and let each client choose defaults. The issue explicitly rules this out ("no error, no null that breaks the client"), and it would duplicate the defaults in every client.
- *Alternative:* seed the defaults in the migration. Then "never configured" and "configured to exactly the defaults" would be indistinguishable, and `isDefault` would be lost.

**3. `PUT /api/goals` takes all four targets and validates them with Zod.**
The rules: calories must be greater than 0, and each macro must be at least 0. `PUT` replaces the whole goal, so partial bodies are rejected. This avoids merge semantics the issue doesn't need. Zod's `.strict()` is not used, which matches how the existing request schemas treat unknown fields.

**4. Frontend: a third "Goals" tab in `MainScreen`, holding a new `GoalsScreen`.**
The issue asks for the goal to be "accessible from the home, editable at any time". The bottom bar is the app's only navigation surface right now, so a tab is the lowest-friction entry point and needs no new navigation state in `AppState`. The screen follows the existing pattern used by `SummaryScreen` and `HistoryScreen`: `LaunchedEffect` load, `isLoading`/`errorMessage` state, `OutlinedTextField`s holding text. Each time the tab is shown, it loads the goal again from the backend.
- *Alternative:* a settings icon that opens a separate `Screen.Goals`. That needs a new `AppState` route and back handling. The tab is enough until issue #3 redesigns the home and can move it.

## Risks / Trade-offs

- **[A fourth table and a third tab add surface area that issue #3 may reshuffle]** → Both are self-contained. Issue #3 can move the entry point without touching the API.
- **[Fixed defaults don't fit everyone]** → They are only shown until the user saves their own goal, and the UI flags them as defaults.
- **[Free-form macros can contradict the calorie target]** → This is an accepted trade-off for now. Deriving or validating the macro split is a possible follow-up, not part of this issue.

## Migration Plan

`npx drizzle-kit generate` produces an additive `CREATE TABLE goals` migration, which `openDatabase` applies on the next startup. There is no data backfill: an empty table simply means "use defaults". Rollback is to drop the table, and the app falls back to not having goals.
