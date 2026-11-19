# Proposal

## Why

The phone app, the watch app and the backend have been stuck at `0.1.0` (`versionCode = 1`) since the start, with no tags and no version shown anywhere. There is no way to tell which build a device runs or whether the backend it talks to is up to date — e.g. a phone APK with "Preentreno" against an older laptop backend that rejects `pre_workout` (issue #118).

## What Changes

- One version for the whole project, kept in a single file at the repository root (`version.properties`, `version=0.2.0`). The phone app, the watch app and the backend all report this version.
- Semantic versioning (`MAJOR.MINOR.PATCH`): MINOR for new functionality, PATCH for fixes, MAJOR for app/backend incompatibilities. The version is bumped in the same pull request as the change; the first version is `0.2.0`.
- The Android `versionCode` of the phone and watch apps is derived from the version (`MAJOR*10000 + MINOR*100 + PATCH`, so `0.2.0` → `200`) instead of being edited by hand.
- New backend endpoint `GET /api/version` (behind the access token) returning the backend's version.
- Options screen: a "Versión" section showing the app version and code (plus the short commit in debug builds) and the server's version, with "no disponible" when it can't be reached and "anterior a 0.2.0" when the server predates the endpoint.
- Release tags `vX.Y.Z` on `master` after merging a version bump; process documented in the README.
- Out of scope: signed release APKs attached to GitHub Releases from CI, warning about app/server version mismatches, showing the version on the watch's screens.

## Capabilities

### New Capabilities

- `app-versioning`: the single project version, its format and bump rules, the derived Android version code, the backend's version endpoint and release tags.

### Modified Capabilities

- `app-settings`: the Options screen gains a version section with the app's and the server's versions.

## Impact

- New `version.properties` at the repo root.
- Gradle (`frontend/build.gradle.kts`, `composeApp`, `wearApp`, `shared`): read the version, derive `versionName`/`versionCode`, generate the version constants used by the app.
- Backend: `package.json` version kept equal to `version.properties` (checked by a test, since the laptop deploy copies only `backend/`), new `/api/version` route.
- Shared API client: `getVersion()`.
- App: Options screen, strings (es/en).
- README: versioning and tagging process. Deploy: backend redeploy shows the new version; older backends answer 404 on `/api/version`.
