# offline-cache Specification

## Purpose

Keeps a copy on the phone of the days the user has seen, so Home and the day detail open at once and still show the last known data when the backend can't be reached.

## Requirements

### Requirement: Cached day shown first
The app SHALL keep on the phone a copy of each day it has loaded (its meals and water) and of the daily goal. When Home or a day's detail opens a day that has a cached copy, the app SHALL show that copy immediately, without a loading indication, and SHALL refresh it from the backend in the background. When the refresh succeeds, the screen SHALL show the refreshed data and the cached copy SHALL be replaced with it. When a day has no cached copy, the app SHALL load it from the backend as before.

#### Scenario: Open Home with a cached day
- **WHEN** the user opens the app and today was loaded before
- **THEN** Home shows today's last known calories, macros, water and meals at once, with no loading indicator, and updates them when the backend answers

#### Scenario: Refreshed data differs
- **WHEN** the backend returns a meal that wasn't in the cached copy
- **THEN** the meal appears without the screen being dimmed or cleared first, and the next opening shows it from the cache

#### Scenario: Day never loaded
- **WHEN** the user opens a past day that was never loaded
- **THEN** the detail shows the loading indication until the backend answers, as before

### Requirement: When the cached day is refreshed
The app SHALL refresh the shown day from the backend when the screen opens or changes day, after the user changes something on it (favorite, delete, water), and when the app returns to the foreground.

#### Scenario: Back to the app
- **WHEN** the user returns to the app after a meal was added from another device
- **THEN** Home refreshes the day and shows the new meal

#### Scenario: After an action
- **WHEN** the user deletes a meal on Home
- **THEN** the day is refreshed and the cached copy no longer has that meal

### Requirement: Offline notice
When the shown day's refresh fails because the backend can't be reached (no network, unreachable address, connection timeout), the app SHALL keep showing the cached copy with a notice that there is no connection and the time the copy was last refreshed, with the date too when it wasn't today. When the backend answers with an error instead (for example a rejected access token), the app SHALL show that error above the cached copy instead of replacing it. When there is no cached copy, the app SHALL show the error alone, as before. The notice SHALL disappear once a refresh succeeds. The notice SHALL be available in Spanish and English.

#### Scenario: Backend unreachable
- **WHEN** the user opens Home away from home and the backend can't be reached
- **THEN** Home shows the last known data of today with a note like "No connection · data from 14:32"

#### Scenario: Rejected token
- **WHEN** the refresh is rejected with 401
- **THEN** the cached day stays visible with the localized "the server rejected the access token" message above it

#### Scenario: No cached copy and no connection
- **WHEN** a day that was never loaded is opened without a connection
- **THEN** the screen shows the error message alone

#### Scenario: Connection comes back
- **WHEN** a later refresh succeeds
- **THEN** the notice disappears and the data is up to date

### Requirement: Unreachable backend detected quickly
The app SHALL give up connecting to the backend after at most 10 seconds, so an unreachable backend is reported without a long wait. The time a connected request may take, such as a meal analysis, SHALL NOT be limited by this.

#### Scenario: Backend machine off
- **WHEN** the backend's machine is off and the user opens Home
- **THEN** the offline notice appears within about 10 seconds

### Requirement: Cache follows the server
Changing the backend server address in Options SHALL discard the whole cache, so data from one backend is never shown as another's. Changing only the access token SHALL keep the cache.

#### Scenario: Switch server
- **WHEN** the user saves a different server address and goes back to Home
- **THEN** Home loads today from the new server, without showing the previous server's data first

### Requirement: Cache stays on the device
The cached data SHALL NOT be included in Android's cloud backups or in device-to-device transfers, on any supported Android version.

#### Scenario: Cloud backup
- **WHEN** Android backs up the phone to the cloud
- **THEN** the app's cached meals, water and goal are not part of the backup
