## Why

Water intake is a standard part of daily tracking in every app we compared (MyFitnessPal, Cronometer, Lifesum, Cal AI). It needs a one-tap log on Home, independent of meals. Tracked as GitHub issue #8: https://github.com/crmartinez930130/food-tracker-ai/issues/8

## What Changes

- **Backend:**
  - New `water_logs` table (`id`, `amount_ml`, `logged_at`). An entry belongs to the local calendar day of `logged_at`, the same rule meals use.
  - New endpoints:
    - `POST /api/water` logs an amount in ml. `loggedAt` defaults to now, and an earlier time can be given to log a past day, but never a future one.
    - `GET /api/water?date=` returns the day's total plus its individual entries. A day without entries returns a total of 0.
    - `DELETE /api/water/:id` removes a mistaken entry.
  - **Water targets live with the daily goal:** the goal gains `targetWaterMl` (default 2000) and `waterGlassMl` (default 250, the amount one tap logs).
    - `PUT /api/goals` accepts both as optional. When omitted, the current values are kept, so a client that only sends the four nutrition targets keeps working.
- **Home:** a **Water** card below the macro bars:
  - consumed vs. target, as text plus a progress bar;
  - a **"+ 250 ml"** button that logs one glass in one tap, into the day Home is showing;
  - an **Undo** button that deletes that day's most recent entry.
- **Goal screen:** a Water section with the daily water target and the glass size.

## Capabilities

### New Capabilities
- `water-tracking`: logging, listing and deleting water entries (API), and the Water card on Home.

### Modified Capabilities
- `nutrition-goals`: the goal now also holds the water target and the glass size, with their defaults, validation and editing on the goal screen.

## Impact

- **Backend:**
  - Migration 0005: the `water_logs` table with an index on `logged_at`, plus two NOT NULL columns with defaults on `goals`, so an existing saved goal stays valid.
  - New `db/water-repository.ts` and `routes/water.ts` (mounted at `/api/water`), with Zod schemas.
  - `goals-repository`, the goals domain and the goals schema are extended.
  - Tests for all of the above.
- **Frontend:**
  - `WaterDay` / `WaterEntry` / `LogWaterRequest` models and three client methods.
  - `DailyGoal` / `UpdateGoalsRequest` gain the two fields.
  - New `ui/home/WaterCard.kt`, with its pure helpers and their tests. Home and Goals screens updated.
- **Dependencies:** none.
