## 1. Backend: data model

- [x] 1.1 Add `waterLogs` (`id`, `amountMl` integer, `loggedAt` text, index on `logged_at`) and `goals.targetWaterMl`/`waterGlassMl` (NOT NULL, defaults 2000/250) to `schema.ts`. Generate migration 0005 with drizzle-kit and check the SQL adds the columns with defaults. Verify with `npm run typecheck` and the existing test suite still passing on a migrated in-memory DB.

## 2. Backend: goals

- [x] 2.1 Extend the goals domain (`DEFAULT_DAILY_GOAL` with 2000/250), `getDailyGoal`, `saveDailyGoal` (keep the stored values or defaults when the water fields are omitted), and `UpdateGoalsRequestSchema` (optional integers: target 1–20000, glass 1–5000). Verify with goals-repository and `routes/goals` tests: defaults, saving both fields, omission keeps the stored values, an old-style PUT, out-of-range and fractional values give 400 with the goal unchanged.

## 3. Backend: water

- [x] 3.1 Create `db/water-repository.ts`: `createWaterLog`, `waterForDay(date)` (SQL total plus chronological entries by local day) and `deleteWaterLog`. Verify with repository tests: total and order, an empty day giving 0, a late-evening local day, deletion.
- [x] 3.2 Create `routes/water.ts` (POST/GET/DELETE) with `LogWaterRequestSchema` and the 5-minute future margin, and mount it at `/api/water`. Verify with route tests covering each acceptance criterion: post then get reflects the total, delete recalculates, an empty day returns 0, the 400 cases (amount and loggedAt, including future), a date that is invalid or missing, and 404/400 on delete.

## 4. Frontend: data

- [x] 4.1 Add `WaterDay`/`WaterEntry`/`LogWaterRequest` (`loggedAt` with `@EncodeDefault(NEVER)`), the `DailyGoal`/`UpdateGoalsRequest` water fields, and the `getWater`/`logWater`/`deleteWater` client methods. Verify with `FoodTrackerApiClientTest`: the paths, `loggedAt` omitted when null, deserialization, the goal fields sent and parsed.

## 5. Frontend: UI

- [x] 5.1 Create `ui/home/WaterCard.kt` with the pure `waterLabel` and `loggedAtFor` helpers, plus the card (progress, "+ N ml", Undo). Verify the helpers with `WaterCardTest`: the label, null for today, and noon local time for a past day round-tripping to that date.
- [x] 5.2 Wire the card into `HomeScreen`: fetch water with the day, refetch only water after log or undo, and show an error on failure. Verify with `./gradlew :composeApp:testDebugUnitTest assembleDebug`.
- [x] 5.3 Add the Water section (target and glass size, with client-side validation) to `GoalsScreen`. Verify with `./gradlew :composeApp:assembleDebug`.

## 6. End-to-end verification

- [x] 6.1 On the emulator against the real backend:
  - tap "+ 250 ml" three times and check the total is 750, then Undo once and check it is 500;
  - force-stop and reopen the app, and check the total is still 500;
  - go to yesterday, add a glass, and check it lands there and not on today;
  - set the glass size to 300 ml in Goals, and check the button says "+ 300 ml" and logs 300;
  - with the backend stopped, tap the button and check an error is shown.

  Afterwards delete the test entries and restore the goal state: the `goals` table stays empty.
