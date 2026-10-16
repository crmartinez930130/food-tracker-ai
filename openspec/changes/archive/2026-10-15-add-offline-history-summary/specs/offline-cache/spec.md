## MODIFIED Requirements

### Requirement: Offline notice
When the shown day's refresh fails because the backend can't be reached (no network, unreachable address, connection timeout), the app SHALL keep showing the cached copy with a notice that there is no connection and the time the copy was last refreshed, with the date too when it wasn't today. When the backend answers with an error instead (for example a rejected access token), the app SHALL show that error above the cached copy instead of replacing it. When there is no cached copy and the backend can't be reached, Home and the day detail SHALL show the day empty instead: the cached goal (or the default goal when none was ever loaded), no stored meals and no water, plus that day's queued entries and kept captures (`offline-queue`), with a notice that there is no connection and no time. Logging water SHALL work there (it is queued). When there is no cached copy and the backend answers with an error, the app SHALL show the error alone, as before. The notice SHALL disappear once a refresh succeeds. The notice SHALL be available in Spanish and English.

#### Scenario: Backend unreachable
- **WHEN** the user opens Home away from home and the backend can't be reached
- **THEN** Home shows the last known data of today with a note like "No connection · data from 14:32"

#### Scenario: Rejected token
- **WHEN** the refresh is rejected with 401
- **THEN** the cached day stays visible with the localized "the server rejected the access token" message above it

#### Scenario: No cached copy and no connection
- **WHEN** the app opens for the first time today, away from home, with a lunch queued and a photo kept for today
- **THEN** Home shows today empty against the goal, with the "No connection" notice, the queued lunch as pending and the photo waiting for analysis

#### Scenario: No cached copy and a rejected token
- **WHEN** a day that was never loaded is opened and the backend answers 401
- **THEN** the screen shows the localized rejected-token message alone

#### Scenario: Connection comes back
- **WHEN** a later refresh succeeds
- **THEN** the notice disappears and the data is up to date

## ADDED Requirements

### Requirement: History without a connection
History SHALL keep on the phone a copy of each month it loaded (each day's totals and the goal's calories) and SHALL show it at once when the month is shown again, refreshing it from the backend in the background. When the backend can't be reached, History SHALL keep showing the copy with a notice that there is no connection and when the copy was refreshed. A month without a copy SHALL show a notice that it hasn't been loaded yet, instead of the technical error. A backend error SHALL be shown above the copy, or alone without one. The notices SHALL be available in Spanish and English.

#### Scenario: Month seen before
- **WHEN** the user opens History without a connection on a month seen before
- **THEN** the calendar and the day list show that month as last loaded, with "No connection · data from 14:32"

#### Scenario: Month never seen
- **WHEN** the user moves without a connection to a month never loaded
- **THEN** History shows "No connection: this period hasn't been loaded yet"

### Requirement: Summary without a connection
Summary SHALL keep on the phone a copy of each period it loaded (each day's totals and the goal's calories) and, separately, of the period's weight entries and the last known weight, and SHALL show them at once when the period is shown again, refreshing them in the background. Without a connection, the copies SHALL stay with the same notice as History, and a period without a copy SHALL show the not-loaded-yet notice. Logging or deleting weight and exporting the CSV SHALL still need a connection and show their errors as before.

#### Scenario: Week seen before
- **WHEN** the user opens Summary without a connection on the week seen before
- **THEN** the calorie chart and the weight section show that week as last loaded, with the no-connection notice

#### Scenario: Log weight without a connection
- **WHEN** the user tries to log their weight without a connection
- **THEN** the weight error is shown as before, and the cached copy stays visible
