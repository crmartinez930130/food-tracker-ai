# Tasks

## 1. Single version source and backend

- [x] 1.1 Add `version.properties` at the repo root with `version=0.2.0` and set `backend/package.json` (and `package-lock.json`) version to `0.2.0`; verify with `grep`.
- [x] 1.2 Backend: read the version from `package.json` at startup and add `GET /api/version` returning `{ version }`, mounted after the access-token check; route tests cover 200 with the token and 401 without (`npm test`).
- [x] 1.3 Backend test that `package.json` version equals the root `version.properties`, failing with both values in the message (`npm test`; temporarily change one value to see it fail).

## 2. Gradle version and generated constants

- [x] 2.1 Root `frontend/build.gradle.kts`: read `../version.properties`, validate `MAJOR.MINOR.PATCH` with MINOR/PATCH ≤ 99, compute `versionCode`, expose them; an invalid value fails the build with the expected format (check by setting `0.3` locally, then restoring).
- [x] 2.2 `composeApp` and `wearApp` use the shared `versionName`/`versionCode`; verify with `aapt2 dump badging` (or `apkanalyzer`) on both debug APKs: `0.2.0` / `200`.
- [x] 2.3 `:shared`: task that generates `AppVersion` (`NAME`, `CODE`, `COMMIT` from `git rev-parse --short HEAD`, `"unknown"` without git) into a common generated source dir, plus `versionCodeOf(name)`; shared unit tests check `versionCodeOf` (`0.2.0` → 200, `1.3.5` → 10305) and `AppVersion.CODE == versionCodeOf(AppVersion.NAME)` (`./gradlew :shared:testDebugUnitTest`).

## 3. Options screen

- [x] 3.1 Shared API client `getVersion()` with a client test against a mocked response (200 → version, 404 → distinguishable "no endpoint" result).
- [x] 3.2 Pure mapping from the request outcome to `Loading | Known | Older | Unavailable`, unit-tested (known version, 404, 401, network error, timeout).
- [x] 3.3 Version section at the end of Options: "Versión X (code)" (+ commit in debug builds) and the server line, refreshed on open and after saving a new address; strings in es/en (`composeApp:testDebugUnitTest` for the strings/mapping).
- [x] 3.4 Verify on the emulator against the PC backend: shows "Versión 0.2.0 (200) · <commit>" and "Servidor 0.2.0"; with the backend stopped shows "no disponible"; against a backend without the endpoint shows "anterior a 0.2.0"; screenshot at phone width.

## 4. Docs and checks

- [x] 4.1 README: "Versiones" section with the bump rules, where the version lives, and how to tag (`git tag -a vX.Y.Z <merge-sha> -m … && git push origin vX.Y.Z`).
- [x] 4.2 Full CI command set passes locally (`npm run typecheck && npm test` in `backend/`; the CI Gradle command in `frontend/`).
