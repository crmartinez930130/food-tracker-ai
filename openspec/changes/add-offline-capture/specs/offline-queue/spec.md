## ADDED Requirements

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
