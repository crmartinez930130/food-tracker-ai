# Design

## Context

See proposal.md. This is a cleanup across code from five earlier issues. The relevant current state:

- **Dates:** `routes/dates.ts` (#11) already exports `isCalendarDate` and `parseDateRange`. `routes/summary.ts` (`/daily`, `/weekly`) and `routes/meals.ts` (`GET /?date=`) still check only `DATE_RE`.
- **Errors:** `routes/goals.ts` and `routes/weight.ts` format Zod issues inline as `path: message`. `routes/meals.ts` throws `parsed.error.message`, which is a JSON dump.
- **Loading:** `HomeScreen` and `SummaryScreen` set `isLoading` on reload but keep rendering the old data, and only show a spinner when there is nothing yet. `WeightSection` has no loading state at all.
- **Client:** `HttpClientFactory` uses `encodeDefaults = true`, so `UpdateMealRequest(notes = "x")` also sends `"items":null,"occurredAt":null,"mealType":null`. #11 solved the same thing for `LogWeightRequest` with `@EncodeDefault(NEVER)`.

## Goals / Non-Goals

**Goals:** fix the five items with the smallest consistent change each, and cover each with a test or an emulator check.

**Non-Goals:** redesigning the screens, adding skeleton loaders, or changing status codes or the shape of successful responses.

## Decisions

**1. One pure loading helper, shared by both screens.**
`ui/LoadingState.kt` defines `enum class LoadingPresentation { INITIAL, STALE, READY }` and `loadingPresentation(isLoading, hasContent)`:
- `INITIAL`: loading with nothing to show yet, so a spinner is shown.
- `STALE`: loading over old content, so the content is drawn at `alpha = 0.4` with a `LinearProgressIndicator` above it.
- `READY`: not loading, so the content is shown normally.

Home applies it to the progress card and meal list. Summary applies it separately to the calorie block and to the weight block, which gets its own `isWeightLoading`, because the two reload independently.
- *Correction found during verification:* the weight chart received the **new** period's days together with the **old** entries, so the stale chart drew nothing. `SummaryScreen` now keeps `weightDays` (the days the loaded entries belong to) and passes those, so the dimmed chart still lines up.
- *Alternative:* clear the data on navigation. That makes the screen flicker on every chevron tap, which is the reason it wasn't done in #3 and #7.

**2. Shared Zod error formatter.** `routes/validation.ts` exports `validationErrorFrom(error: ZodError): ValidationError`, which builds `issues.map(i => \`${i.path.join(".") || "body"}: ${i.message}\`).join("; ")`. It is used by meals, goals and weight. The `|| "body"` covers root-level issues, such as a body that isn't an object, which have an empty path.

**3. `isCalendarDate` in every date query.** `/summary/daily`, `/summary/weekly` and `GET /api/meals?date=` swap `DATE_RE.test` for `isCalendarDate`, keeping their existing error messages. `DATE_RE` stays exported for `dates.ts` itself.

**4. `UpdateMealRequest` with `@EncodeDefault(NEVER)` on every nullable field.** An absent field is then omitted, so the backend keeps its current value, which is exactly `updateMeal`'s "only the fields provided" contract.
- *Trade-off:* the client can no longer send an explicit `null` to clear `notes` or `mealType`. Nothing in the app needs that today. Supporting it would take a tri-state type, and it isn't worth adding until a screen edits saved meals.

**5. Manifest.** Add `<uses-feature android:name="android.hardware.camera" android:required="false" />`. `CameraCapture` already handles a device with no back camera (#5, `IllegalArgumentException` → gallery fallback), so this only stops Play from filtering such devices.

## Risks / Trade-offs

- **[Meal error message text changes]** → Only the app shows these messages, and it shows them as-is, so readable text is strictly better. Status codes don't change.
- **[Dimming relies on the emulator check]** → The decision logic is unit-tested. The visual part is checked on the emulator with a slowed backend. `adb emu network delay` didn't affect host (`10.0.2.2`) traffic, so the check ran the backend on `PORT=3001` behind a throwaway 3-second delaying TCP proxy on `:3000`, kept in the scratchpad. No repository code changed for it.
