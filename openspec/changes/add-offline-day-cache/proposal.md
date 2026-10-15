## Why

Home and a past day's detail ask the backend for the day's meals, the goal and the water every time they open. Without a connection they show only an error, and with a slow one they show a spinner first. The phone's backend runs on a laptop at home, so away from home the app shows nothing. This change keeps a local copy of what was last loaded, as the first step towards using the app offline (#70 and #71 build their send queue on the same local database).

Tracked as GitHub issue #69: https://github.com/crmartinez930130/food-tracker-ai/issues/69

## What Changes

- **Local database:** a Room 3 database (Kotlin Multiplatform, with the bundled SQLite driver) on the phone. In this change it holds a cache of each day's meals and water, and of the daily goal.
- **Cache first:** Home and the day detail show the cached day at once and refresh it from the backend in the background. When the refresh arrives the content is replaced, with no spinner or dimming. The loading indication only appears when the day has never been loaded.
- **Refresh moments:** on opening the day, on changing day, after an action on it (favorite, delete, water), and when the app returns to the foreground.
- **Offline notice:** when the backend can't be reached, the cached day stays on screen with a short note, "No connection · data from <time>", using the time it was last refreshed. When the backend answers with an error (for example a rejected token), that error is shown above the cached content instead of replacing it. With no cached copy, the behaviour is as today: the error alone.
- **Faster failure:** the app's HTTP client gets a connection timeout of 10 seconds, so an unreachable backend is noticed quickly. Today it can wait up to about 100 seconds. Read and request time aren't limited, so a long analysis is unaffected.
- **Server change:** changing the server address in Options → Server empties the cache, so another backend's data is never shown.
- **Backups:** the database stays out of Android backups and device-to-device transfers. The current rules already exclude the whole `database` domain; the spec now covers the cache too.
- Actions on the day (favorite, delete, water) still need a connection. Sending without a connection comes in #70.

## Capabilities

### New Capabilities
- `offline-cache`: what the app keeps on the phone to show days without a connection, when it refreshes it, how it signals stale data, and when it discards it.

### Modified Capabilities
- `daily-dashboard`: the loading indication while Home changes day only applies when the day isn't cached; a cached day appears at once.

## Impact

- **App (`frontend/composeApp`):**
  - Gradle: KSP plugin and Room 3 Gradle plugin, `room3-runtime`, `sqlite-bundled`, and the exported Room schema committed under `composeApp/schemas/`.
  - `commonMain`: the database, its DAO and entities, a day repository, `DayContent` reading from it, the offline notice, and the timeout in `createHttpClient`.
  - `androidMain`: building the database with the app context, wiring it into `App`, and clearing it when the server address changes (`MainActivity`).
  - `iosMain`: the database builder for iOS. iOS isn't built on this Linux machine or in CI, so this part can't be verified here.
  - New strings in Spanish and English.
- **Backend:** none.
- **Dependencies:** Room 3.0.3, `androidx.sqlite:sqlite-bundled` 2.7.1, KSP 2.3.12. Compatibility with Kotlin 2.4.20 and AGP 8.13.2 is checked as the first implementation step.
