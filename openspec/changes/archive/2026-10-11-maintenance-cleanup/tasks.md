# Tasks

## 1. Backend

- [x] 1.1 Add `routes/validation.ts` with `validationErrorFrom(ZodError)` (`path: message` joined by `; `, root issues labelled `body`) and use it in `meals.ts` (POST, PATCH), `goals.ts` and `weight.ts`; verify with tests: invalid meal POST and PATCH return 400 with `field: message` and no `{`, a non-object body yields `body: …`, and the existing goals/weight error tests still pass
- [x] 1.2 Use `isCalendarDate` in `/api/summary/daily`, `/api/summary/weekly` and `GET /api/meals?date=`; verify with tests that `2026-02-30` returns 400 on all three and valid dates still work

## 2. Frontend

- [x] 2.1 Add `ui/LoadingState.kt` (`LoadingPresentation`, `loadingPresentation(isLoading, hasContent)`); verify with `commonTest` covering all four input combinations
- [x] 2.2 Apply it in `HomeScreen` (progress + meals), and in `SummaryScreen` to the calorie block and, via a separate `isWeightLoading`, to `WeightSection`: spinner when INITIAL, `alpha(0.4f)` + `LinearProgressIndicator` when STALE; verify it compiles
- [x] 2.3 Mark `UpdateMealRequest`'s nullable fields `@EncodeDefault(NEVER)`; verify with a client test that `updateMeal(id, UpdateMealRequest(notes = "x"))` sends exactly `{"notes":"x"}`
- [x] 2.4 Add `<uses-feature android:name="android.hardware.camera" android:required="false" />` to the manifest; verify `:composeApp:assembleDebug` builds and `aapt`/manifest dump shows the feature as not required

## 3. End-to-end verification

- [x] 3.1 On the emulator against a deliberately slowed backend (backend on :3001 behind a throwaway 3 s delaying proxy on :3000 — nothing committed): change day on Home and period on Summary and capture the dimmed + progress state, then the normal state after load; also check first load shows a spinner. Run the full backend and frontend test suites. Remove the temporary delay and any test data afterwards
