## ADDED Requirements

### Requirement: Leave the logging flow safely
While logging a meal, the system back action SHALL navigate within the app instead of closing it. Leaving the review screen, by system back or by its cancel action, SHALL ask for confirmation first, so an unsaved analysis and its edits are not lost by accident.

#### Scenario: Back from the capture screen without a photo
- **WHEN** the user presses system back on the capture screen with no photo selected
- **THEN** the app returns to Home without closing

#### Scenario: Back from the capture screen with a photo awaiting confirmation
- **WHEN** the user presses system back while a captured or chosen photo is shown for confirmation
- **THEN** the photo is discarded and the capture screen shows the photo options again, and a further back returns to Home

#### Scenario: Back or cancel on the review screen
- **WHEN** the user presses system back or the cancel action on the review screen
- **THEN** the app asks whether to discard the meal, and nothing is discarded yet

#### Scenario: Confirm discarding
- **WHEN** the user confirms discarding in that prompt
- **THEN** the app returns to Home and no meal entry is created

#### Scenario: Keep editing
- **WHEN** the user chooses to keep editing in that prompt, or presses back while it is shown
- **THEN** the prompt closes and the review screen keeps the analysis and every edit made so far

#### Scenario: Back while saving
- **WHEN** the user presses system back while the meal is being saved
- **THEN** the app ignores it, neither leaving the screen nor showing the prompt, and the save completes normally
