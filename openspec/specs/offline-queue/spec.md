# offline-queue Specification

## Purpose

Lets the user log meals and water without a connection to the backend: entries wait on the phone with the time they were logged, show up as pending, and are sent automatically, exactly once, when the backend can be reached again.

## Requirements

### Requirement: Entries keep the phone's time
A meal saved from Review SHALL be sent with the moment the user tapped Save as its `occurredAt`, and a glass of water logged for today SHALL be sent with the moment of the tap as its `loggedAt`, whether it is sent at once or later from the queue. Each meal and glass SHALL carry a `clientId` generated once for it and repeated on every attempt to send it.

#### Scenario: Meal sent later
- **WHEN** the user saves a lunch at 13:05 without a connection and it is sent at 18:40
- **THEN** the stored meal's time is 13:05

#### Scenario: Retry after a lost answer
- **WHEN** a meal reached the backend but its answer was lost, and the app sends it again
- **THEN** the backend keeps a single meal

### Requirement: Save without a connection
When the user saves a meal from Review or adds a glass of water and the backend can't be reached, the app SHALL keep the entry in a queue on the phone and continue as if it was saved: Review closes, and the glass counts at once. When the backend answers with an error instead, the app SHALL show the error as before and SHALL NOT queue the entry.

#### Scenario: Save a meal offline
- **WHEN** the user saves a meal from Review and the backend can't be reached
- **THEN** Review closes and Home shows the meal marked as pending

#### Scenario: Backend rejects the save
- **WHEN** the user saves a meal and the backend answers with an error
- **THEN** Review stays open with the error message, and nothing is queued

### Requirement: Pending entries on Home
Home and a day's detail SHALL show that day's queued meals inside their meal-type group, marked as pending, and SHALL include them in the day's calories and macros. Queued glasses SHALL be included in the day's water. Pending meals SHALL NOT offer the favorite and delete actions. A queued entry SHALL never be shown twice: once the backend has stored it, it SHALL appear only as a stored entry. The pending mark SHALL be available in Spanish and English.

#### Scenario: Pending meal counts
- **WHEN** a 500 kcal meal is queued for today
- **THEN** Home shows it marked as pending and today's calories include its 500 kcal

#### Scenario: Pending meal is sent
- **WHEN** the queued meal is sent
- **THEN** Home shows it as a normal meal, once, without it disappearing in between

### Requirement: Undo a pending glass
The water card's undo action SHALL remove the day's newest queued glass on the phone when the day has one, without needing a connection. Otherwise it SHALL delete the day's newest stored glass, as before.

#### Scenario: Undo offline
- **WHEN** the user adds a glass without a connection and taps undo
- **THEN** the glass disappears from the total and is never sent

### Requirement: Automatic sending
The app SHALL send queued entries, oldest first, when it opens or returns to the foreground, and in the background when the phone has a network, including after the app is closed or the phone restarts. While the backend can't be reached it SHALL keep retrying in the background with growing waits. After an entry is sent, the shown day SHALL include it as stored.

#### Scenario: Back home with the app closed
- **WHEN** meals were queued away from home, the app was closed, and the phone reconnects to the home network
- **THEN** the meals are sent without opening the app, and Home shows them as stored the next time it opens

#### Scenario: Opening the app sends the queue
- **WHEN** the user opens the app with queued entries and the backend can be reached
- **THEN** the entries are sent right away

### Requirement: Rejected queued entries
When the backend answers a queued entry with an error instead of storing it (for example a validation error or a rejected access token), the entry SHALL stay on the phone marked with that error, SHALL NOT be counted in the day's totals, and SHALL NOT be sent again automatically. Home SHALL show the error with a retry action, which sends it again, and a discard action, which removes it from the phone. The texts SHALL be available in Spanish and English.

#### Scenario: Token rejected
- **WHEN** a queued meal is sent and the backend answers 401
- **THEN** Home shows the meal with the localized rejected-token message, and it isn't retried in a loop

#### Scenario: Retry after fixing the token
- **WHEN** the user fixes the token in Options and taps retry on that meal
- **THEN** it is sent and appears as a stored meal

#### Scenario: Discard
- **WHEN** the user taps discard on a rejected entry
- **THEN** it disappears from Home and is never sent

### Requirement: Queued entries belong to their server
Each queued entry SHALL belong to the server address that was current when it was saved. It SHALL only be shown and sent while that address is the current server. Changing the server SHALL NOT delete queued entries.

#### Scenario: Switch server with pending entries
- **WHEN** a meal is queued for the home server and the user switches to another server
- **THEN** the meal isn't shown or sent to the other server, and it is sent once the home server is set again

### Requirement: Favorites available offline
The capture screen SHALL keep a copy of the favorites list it last loaded and SHALL show it when the backend can't be reached, so a favorite can be re-logged without a connection. The copy SHALL be replaced whenever the list loads, and changing the server address SHALL discard it, like the rest of the cache.

#### Scenario: Re-log a favorite offline
- **WHEN** the user opens capture without a connection after having seen the favorites before
- **THEN** the favorites are listed, and saving one queues it

#### Scenario: Another server
- **WHEN** the user switches to another server and opens capture without a connection
- **THEN** the previous server's favorites aren't listed

### Requirement: Queued meals count for reminders
When a reminder checks whether a meal was logged today, a meal of that type waiting in the queue for today SHALL count as logged.

#### Scenario: Lunch queued offline
- **WHEN** a lunch is queued for today and the lunch reminder fires
- **THEN** no notification is shown

### Requirement: Capture without a connection
When the user analyzes a capture and the backend can't be reached (no answer, or a 5xx, 408 or 429 status), the app SHALL keep the capture on the phone with what the analysis needs (the photo if any, the description if any, the portion if chosen, and the app's language), the time of the tap and a client id, and SHALL return to Home. When the backend refuses the analysis with another error, the app SHALL show the error as before and SHALL NOT keep the capture. Photos SHALL be stored in the app's private storage, which is not backed up.

#### Scenario: Photo without a connection
- **WHEN** the user takes a photo and taps analyze without a connection
- **THEN** the app returns to Home, which shows the capture as waiting for analysis with the photo's thumbnail and the time

#### Scenario: Description only
- **WHEN** the user writes a description, chooses a large portion and taps analyze without a connection
- **THEN** the capture is kept with that description and portion, and no photo

### Requirement: Pending captures on Home
Home and a day's detail SHALL show that day's kept captures as cards with their thumbnail or description and time, in one of three states: waiting for analysis, ready to review, or failed with its error. Captures SHALL NOT count in the day's totals. Every card SHALL offer a discard action, which removes the capture and its photo from the phone. A failed card SHALL also offer retry, and a ready card SHALL offer review. The texts SHALL be available in Spanish and English.

#### Scenario: Discard a capture
- **WHEN** the user discards a capture waiting for analysis
- **THEN** the card disappears, its photo is deleted, and it is never analyzed

### Requirement: Analysis when the connection is back
Kept captures SHALL be analyzed, oldest first and one at a time, at the same moments the queued meals are sent (`offline-queue`): when the app opens or returns to the foreground, and in the background when the phone has a network. Analysis SHALL stop at the first capture without an answer and continue later. A successful analysis SHALL make the capture ready to review and SHALL NOT save a meal. A refused analysis SHALL mark the capture as failed with the error, and it SHALL NOT be analyzed again until the user retries it. Like queued meals, a capture SHALL only be shown and analyzed while the server it was taken for is the current one.

#### Scenario: Back online
- **WHEN** a capture waits for analysis and the backend can be reached again
- **THEN** it is analyzed and its card becomes ready to review, without any meal being saved

#### Scenario: Analysis refused
- **WHEN** the backend refuses a kept capture's analysis with an error
- **THEN** the card shows the error with retry and discard, and it isn't analyzed again on its own

### Requirement: Review a ready capture
Reviewing a ready capture SHALL open Review with its analysis. Saving SHALL store the meal with the capture's time as its time, sending it or queueing it like any other save, and SHALL then remove the capture and its photo. Leaving Review without saving SHALL keep the capture ready to review.

#### Scenario: Save hours later
- **WHEN** a lunch photographed at 13:05 without a connection is analyzed and reviewed at 19:00
- **THEN** the saved meal's time is 13:05

#### Scenario: Leave without saving
- **WHEN** the user opens a ready capture in Review and leaves without saving
- **THEN** Home still shows it ready to review

### Requirement: Ready-to-review notification
When the background analysis makes one or more captures ready to review, the app SHALL post a notification saying there are meals ready to review, if it may post notifications, in the app's language. Tapping it SHALL open Home.

#### Scenario: Analyzed in the background
- **WHEN** the app is closed, the phone gets back to the home network, and a kept capture is analyzed
- **THEN** a "You have meals ready to review" notification appears, and tapping it shows Home with the card ready to review
