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

#### Scenario: Opened from closed without a connection
- **WHEN** the app starts from closed with no connection and today's copy is on the phone
- **THEN** Home shows that copy with the "No connection · data from …" notice, never the empty day
