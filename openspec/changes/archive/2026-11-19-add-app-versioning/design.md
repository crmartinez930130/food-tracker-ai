# Design

## Context

- Gradle root is `frontend/`, with modules `:shared` (KMP, used by phone and watch), `:composeApp` (phone, KMP with an iOS target) and `:wearApp` (Android only). Both apps hardcode `versionCode = 1` / `versionName = "0.1.0"`.
- The Options screen (`ui/settings/SettingsScreen.kt`) is common code; the only build-type hook today is `expect fun isDebugBuild()`.
- The backend is deployed to the laptop with `git archive origin/master backend`, so only `backend/` reaches it — the backend can't read a file at the repository root at runtime.
- Every `/api` route goes through `requireAccessToken`. Old deployed backends answer unknown `/api` routes with Express's default 404.
- CI runs `npm test` in `backend/` from a full checkout, and the Gradle unit tests + debug assemblies in `frontend/`.

## Goals / Non-Goals

**Goals:**
- One edit (the root `version.properties`) changes every reported version; mistakes fail a build or a test instead of shipping silently.
- Version code arithmetic lives in one place and is unit-tested.

**Non-Goals:**
- Automating the bump or the tag (no release tooling, no CI job that tags).
- Showing the version on the watch's own screens (it only gets the Android version name/code).

## Decisions

1. **Root `version.properties`, read by Gradle; backend keeps its own copy in `package.json`.**
   Gradle reads `../version.properties` relative to `frontend/` with `providers.fileContents` (configuration-cache friendly). The backend reads its `package.json` version at runtime (one level up from both `src/` and `dist/`). A backend test reads `../version.properties` and fails if it differs from `package.json`.
   *Alternative:* the backend reading the root file at runtime — breaks the laptop deploy, which only copies `backend/`. *Alternative:* a Node script that writes `package.json` from the root file — one more step to forget; the test catches drift with no tooling.

2. **Parse and compute once, in the root `frontend/build.gradle.kts`, shared through `extra`.**
   The root script parses `MAJOR.MINOR.PATCH`, validates MINOR/PATCH ≤ 99, computes the code and exposes `appVersionName` / `appVersionCode`; `composeApp` and `wearApp` read them for `defaultConfig`. An invalid value throws a `GradleException` with the expected format.
   *Alternative:* a `buildSrc`/`build-logic` module with a tested Kotlin function — cleaner but a whole new module and longer builds for a three-line formula. The formula is instead tested through the generated constants (decision 3).

3. **Generated `AppVersion` object in `:shared` common code.**
   A Gradle task in `:shared` writes `com.foodtracker.app.AppVersion` (`NAME`, `CODE`, `COMMIT`) into a generated common source dir, registered as a source of `commonMain`. `COMMIT` comes from `git rev-parse --short HEAD` via `providers.exec`, falling back to `"unknown"` when git isn't available. Common code (Options screen, future watch UI, iOS) uses it without `BuildConfig`; the screen shows `COMMIT` only when `isDebugBuild()`.
   A small common function `versionCodeOf(name)` holds the formula for tests: a shared unit test checks it (`0.2.0` → 200, `1.3.5` → 10305) and that `AppVersion.CODE == versionCodeOf(AppVersion.NAME)`, which catches the Gradle computation drifting from the spec.
   *Alternative:* Android `BuildConfig` fields per app plus an `expect/actual` — two copies, and nothing for iOS.

4. **`GET /api/version` behind the token.**
   A tiny router returning `{ version }`, mounted with the others after `requireAccessToken`. Keeping it behind the token avoids advertising the server version on the home network and matches the existing access spec ("every request under `/api/`").
   *Alternative:* an unauthenticated `/health` — useful for monitoring, but not needed now and contradicts the access requirement.

5. **Server version state in the Options screen.**
   `FoodTrackerApiClient.getVersion()` returns the version; the screen maps the outcome to `Loading | Known(v) | Older (HTTP 404) | Unavailable (anything else)`, requested in a `LaunchedEffect` keyed on the saved server address and token, so it refreshes after saving. A short timeout (same 5 s as the connection test) keeps "Cargando…" from hanging. The mapping is a pure function, unit-tested.

## Risks / Trade-offs

- [Forgetting to bump the version in a PR] → README process + the issue/PR workflow includes "bump version" as a checklist item; the reviewer can flag a feature PR without a bump.
- [`package.json` drifting from `version.properties`] → backend test fails in CI.
- [Generated `AppVersion` changes on every commit (COMMIT), recompiling `:shared`] → acceptable for this project's size; the task declares its inputs so it's skipped when nothing changed.
- [Phone and watch share `applicationId` and now share `versionCode`] → fine for sideloading; if they are ever published to a store, the watch would need its own code offset (out of scope).

## Migration Plan

1. Merge with `version.properties` = `0.2.0`, `package.json` = `0.2.0`; tag `v0.2.0`.
2. Redeploy the laptop backend, then install the new APK on the phone and the new build on the watch. Until the backend is redeployed, Options shows "Servidor: anterior a 0.2.0".
3. Rollback: reinstalling an older APK over `200` requires uninstalling first (Android refuses downgrades) — expected and desired.
