## MODIFIED Requirements

### Requirement: Edit the goal from the app
The app SHALL provide a screen, reachable from Home at any time through an edit-goal action in Home's header and by tapping the calorie percentage badge, and from a day's detail in History by tapping its percentage badge, that shows the current goal loaded from the backend. On that screen the user SHALL be able to edit and save the four nutrition targets, the daily water target and the glass size. The screen SHALL offer a back action, and going back SHALL return to the screen it was opened from (Home or that day's detail), with progress measured against the latest saved goal.

#### Scenario: Open the goal screen
- **WHEN** the user opens the goal screen
- **THEN** the app shows the goal currently stored in the backend, or the default when none is configured, and indicates when it is showing the default

#### Scenario: Save from the app
- **WHEN** the user edits the targets, including the water target and glass size, and saves them
- **THEN** the app sends them to the backend, and reopening the screen, including after restarting the app, shows the saved values

#### Scenario: Backend rejects the values
- **WHEN** the user saves values that the backend rejects, or the request fails
- **THEN** the app shows an error message and keeps the user's edits on screen so they can correct them

#### Scenario: Return to Home
- **WHEN** the user goes back from the goal screen (back arrow or system back) after saving a new goal
- **THEN** Home shows today with progress measured against the newly saved goal, including the new water target and glass size

#### Scenario: Return to a day's detail
- **WHEN** the user opens the goal from a past day's detail, saves a new goal and goes back
- **THEN** the same day's detail is shown, with its progress measured against the new goal
