## Context

- Meals store a UTC ISO `occurred_at` and belong to `date(occurred_at, 'localtime')`. The server's timezone is the user's, since this is a single-user, single-machine app.
- Weight (#11) stores a `day` string instead, because a weighing has no meaningful time.
- The goal is a single row (`id = 1`). `GET /api/goals` returns built-in defaults with `isDefault: true` until the user saves one.
- Home shows one day (`homeDate`) with the calorie ring and macro bars in the first `LazyColumn` item, wrapped in `LoadingAware`.

## Goals / Non-Goals

**Goals:**
- One tap logs a glass, and a mistaken tap is just as easy to undo.
- Water follows the same day rules as meals, so Home never disagrees with itself.

**Non-Goals:**
- Water in Summary charts or in the CSV export.
- Other drinks, or caffeine.
- Reminders, which are #10.
- An arbitrary-amount dialog. The glass size is configurable instead, as the issue asks.

## Decisions

### D1. Store a timestamp (`logged_at`), not a day
- This follows the issue (`loggedAt`) and the meals model: grouping uses `date(logged_at,'localtime')`, so water and meals always land on the same day.
- An index on `logged_at` keeps the per-day query cheap.
- **Alternative:** a `day` column, like weight. Rejected: the time is useful, entries are listed in order and Undo removes the latest, and keeping one day rule for everything consumed is simpler.

### D2. `POST /api/water` accepts an optional `loggedAt`
- **Body:** `{ amountMl, loggedAt? }`. `amountMl` is an integer from 1 to 5000. `loggedAt` is an ISO-8601 timestamp with an offset or `Z`, stored normalized to UTC so entries sort correctly as strings.
- **Future check:** a `loggedAt` more than 5 minutes in the future is rejected. The margin absorbs clock skew between phone and server.
- **Omitted `loggedAt`:** the server's now.
- **In the app:**
  - When Home shows today, it omits `loggedAt`.
  - When Home shows a past day, it sends that day at **12:00 local time**. Noon is unambiguous across DST changes and stays inside the day under any small offset.
- This lets the user fix "I forgot to log yesterday's water" from the day they are looking at.
- **Alternative:** only allow logging today. Rejected: Home lets you browse past days, and a button there that logs to today would be confusing.

### D3. `GET /api/water?date=` returns the day as one object
- **Shape:** `{ date, totalMl, entries: [{ id, amountMl, loggedAt }] }`, with entries in chronological order.
- `totalMl` is summed from the day's entries, which are loaded anyway, so the total and the list always agree. A day with no entries returns `totalMl: 0` and an empty list.
- `date` is validated with `isCalendarDate`.
- **Alternative:** return a bare list and let the client sum it. Rejected: the issue asks for the total, and sending it keeps clients trivial.

### D4. Water target and glass size on the goal row
- **Columns** on `goals`: `target_water_ml` (default 2000) and `water_glass_ml` (default 250), both NOT NULL with SQL defaults, so the migration needs no backfill.
- `DEFAULT_DAILY_GOAL` gains the same defaults.
- **`PUT /api/goals`:**
  - the four nutrition targets stay required (no behavior change);
  - the two water fields are optional;
  - when they are omitted, `saveDailyGoal` keeps the stored values, or the defaults when no goal row exists yet.
- **Validation:**
  - `targetWaterMl`: integer, 1 to 20000;
  - `waterGlassMl`: integer, 1 to 5000, the same bound as a single log.
- **Alternatives:**
  - A separate settings table. Rejected: the goal row is already the one place for "my daily targets", and the Goal screen is where the user edits them.
  - Client-only preferences. Rejected: the issue's "persists after reopening" and the backend-as-source-of-truth approach of the rest of the app.

### D5. Home: a Water card inside the progress item
- **Placement:** below the macro bars, in the same `LazyColumn` item, so it dims together with the rest while a new day loads.
- **Contents:**
  - a title row "Water" with `750 / 2000 ml`;
  - a `LinearProgressIndicator` using `progressOf` (clamped at full when over);
  - a `FilledTonalButton` "+ 250 ml" (the glass size from the goal);
  - an "Undo" `TextButton`, enabled when the day has entries, that deletes the latest one.
- **Loading:**
  - Water is fetched alongside meals and the goal when the day changes.
  - After a log or an undo, only water is refetched (`waterReloadKey`), not the meals.
  - Rapid taps are allowed, because two glasses are two taps. Water actions run one at a time behind a `Mutex`, and each refetches the day, so the final total reflects every tap.
  - Undo picks the latest entry from a fresh fetch inside that lock, so two quick undos remove two different entries instead of the same one twice.
  - A refetch that completes after the user moved to another day is not shown on that day.
- **Pure helpers in `WaterCard.kt`, unit-tested:**
  - `waterLabel(totalMl, targetMl)`;
  - `loggedAtFor(date, today)`, which returns null for today and noon local time as ISO for a past day.

### D6. Goal screen: a "Water" section
- Two integer fields: "Daily water (ml)" and "Glass size (ml)".
- The client checks the same rules as D4 before saving, like the existing fields.
- The title stays "Daily goal".

## Risks / Trade-offs

- **Undo deletes the latest entry of the shown day, not necessarily the user's mistaken one.** → For the one-tap flow the mistake is almost always the last tap. Arbitrary deletion is still possible through the API if a list is ever needed.
- **Noon for past-day logs invents a time.** → Only the day matters for totals, and the actual hour of a forgotten glass is unknowable anyway.
- **Goal PUT semantics become mixed** (four required fields, two optional). → This is documented in the spec, and it keeps older clients working.

## Migration Plan

Migration 0005 is additive: a new table plus two columns with defaults. To roll back, revert the commit. SQLite would keep the unused columns, which is harmless.
