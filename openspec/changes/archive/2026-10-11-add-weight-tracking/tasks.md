# Tasks

## 1. Backend

- [x] 1.1 Move `isCalendarDate`, the range checks and `MAX_RANGE_DAYS` from `routes/summary.ts` into `routes/dates.ts` (plus a local "today" helper); verify the existing summary route tests still pass unchanged
- [x] 1.2 Add `weight_logs` (`id`, `weight_kg`, `day`, `created_at`, index on `day`) to the schema and generate the migration; add `db/weight-repository.ts` with `createWeightLog`, `listWeightLogs(startDay, endDay)` (inclusive, ordered by day then created_at), `deleteWeightLog(id)`; verify with repository tests: create, range filtering and ordering (incl. two entries the same day), empty range, delete existing/missing
- [x] 1.3 Add `routes/weight.ts` mounted at `/api/weight` with `POST /` (Zod + calendar-day + not-future checks, readable errors, day defaulting to local today), `GET /?startDate=&endDate=` (shared range validation) and `DELETE /:id`; verify with route tests covering every spec scenario (today default, past day, invalid weight/date/future → 400 with nothing stored, range in order, empty range, invalid range → 400, delete → gone, delete missing → 404)

## 2. Frontend data and chart math

- [x] 2.1 Add `WeightEntry(id, weightKg, day, createdAt)` and `LogWeightRequest(weightKg, day?)` plus `listWeight(start, end)`, `logWeight(request)`, `deleteWeight(id)` to the client; verify with `commonTest` cases for deserialization, the POST body (no null `day` sent when omitted) and the DELETE path
- [x] 2.2 Add `latestPerDay(entries)` and `weightScale(values)` to `ui/summary/ChartLayout.kt`; verify in `ChartLayoutTest`: last entry wins within a day, days stay chronological, scale pads ±0.5 kg, a flat series still spans ≥ 2 kg, single value works

## 3. Frontend UI

- [x] 3.1 Add `ui/summary/WeightChart.kt` (Canvas line + points on the calorie chart's day slots, non-zero y axis, gaps skipped, `contentDescription`); verify it compiles
- [x] 3.2 Add the Weight section to `SummaryScreen` (header with latest + change, "Log weight" dialog pre-filled with the last weight and validating input, chart, newest-first entry list with delete, empty-period message), loading `listWeight` for the same period; verify `:composeApp:assembleDebug` and `:composeApp:testDebugUnitTest` pass

## 4. End-to-end verification

- [x] 4.1 On the emulator against the real backend: empty period shows the invite; log a weight from the dialog (pre-fill empty the first time) → appears in list and as a point; seed a few past days via the API → line across days, latest + change correct, aligned under the calorie chart's days; invalid input (0, 600, "abc") shows an error and saves nothing; delete an entry → it disappears; switch to Month and navigate; light/dark screenshots. Delete all test entries afterwards
