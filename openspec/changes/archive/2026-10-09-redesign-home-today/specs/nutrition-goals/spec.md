## MODIFIED Requirements

### Requirement: Edit the goal from the app
The app SHALL provide a screen, reachable from Home at any time (through an edit-goal action in Home's header and by tapping the calorie progress indicator), that shows the current goal loaded from the backend and lets the user edit and save all four targets. The screen SHALL offer a back action, and going back SHALL return to Home on the day that was being viewed, with progress measured against the latest saved goal.

#### Scenario: Open the goal screen
- **WHEN** the user opens the goal screen
- **THEN** the app shows the goal currently stored in the backend, or the default when none is configured, and indicates when it is showing the default

#### Scenario: Save from the app
- **WHEN** the user edits the targets and saves them
- **THEN** the app sends them to the backend, and reopening the screen, including after restarting the app, shows the saved values

#### Scenario: Backend rejects the values
- **WHEN** the user saves values that the backend rejects, or the request fails
- **THEN** the app shows an error message and keeps the user's edits on screen so they can correct them

#### Scenario: Return to Home
- **WHEN** the user goes back from the goal screen (back arrow or system back) after viewing a past day on Home and saving a new goal
- **THEN** Home shows the same day it was showing before, with progress measured against the newly saved goal
