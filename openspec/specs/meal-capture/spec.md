# meal-capture Specification

## Purpose

Covers how the app obtains the photo of a meal before it is analyzed: shooting it with the device camera inside the app or choosing an existing one from the gallery, and letting the user confirm it first.

## Requirements

### Requirement: Take a photo with the in-app camera
The app SHALL let the user take a meal photo with the device camera without leaving the app, by showing a live camera preview with a shutter control and a way to cancel.

#### Scenario: Capture a photo
- **WHEN** the user chooses to take a photo and presses the shutter
- **THEN** the app captures the photo and shows it on the capture screen for confirmation

#### Scenario: Cancel the camera
- **WHEN** the user cancels from the camera preview
- **THEN** the app returns to the capture screen without a photo and without calling the analysis service

### Requirement: Camera permission handling
The app SHALL request the camera permission when the user first opens the camera without it being granted, and SHALL handle a denial without crashing, explaining why it is needed and offering the gallery as an alternative.

#### Scenario: Permission not yet granted
- **WHEN** the user opens the camera and the permission has not been granted
- **THEN** the app asks for the camera permission, and shows the camera preview once it is granted

#### Scenario: Permission denied
- **WHEN** the user denies the camera permission
- **THEN** the app shows a message explaining that the camera is needed to take photos and offers to choose from the gallery instead, without crashing

### Requirement: Choose a photo from the gallery
The app SHALL keep letting the user pick an existing photo from the device gallery as an alternative to the camera.

#### Scenario: Pick from gallery
- **WHEN** the user chooses a photo from the gallery
- **THEN** the app shows it on the capture screen for confirmation, the same way as a camera photo

### Requirement: Confirm the photo before analysis
The app SHALL show the selected or captured photo before analyzing it, and SHALL let the user replace it (retake or choose again) or send it for analysis. Camera and gallery photos SHALL follow the same analysis path afterwards.

#### Scenario: Analyze a confirmed camera photo
- **WHEN** the user confirms a captured photo for analysis
- **THEN** the app sends it to the analysis service and shows the review screen with the returned analysis

#### Scenario: Retake
- **WHEN** the user chooses to retake a captured photo
- **THEN** the app discards it and opens the camera again

### Requirement: Re-log a favorite meal without analysis
The capture screen SHALL list the user's favorite meals as an alternative to taking or choosing a photo. Choosing one SHALL open the review screen pre-filled with that meal's foods, quantities, macros and notes, without calling the photo analysis service, and confirming SHALL save a new meal entry timestamped now, leaving the original favorite unchanged.

#### Scenario: Re-log a favorite
- **WHEN** the user picks a favorite on the capture screen and confirms the pre-filled review
- **THEN** a new meal entry is saved for the current date and time with the favorite's foods and macros, the original entry is unchanged, and the photo analysis service is not called

#### Scenario: Edit before re-logging
- **WHEN** the user changes quantities or macros in the pre-filled review before confirming
- **THEN** the new entry is saved with the edited values, and the original favorite keeps its own values

#### Scenario: Re-logged copy is not a favorite
- **WHEN** a favorite has been re-logged
- **THEN** the new entry is not itself a favorite, so the favorites list does not gain a duplicate

#### Scenario: No favorites yet
- **WHEN** the user has no favorite meals
- **THEN** the capture screen shows the photo options and a short hint that starring a meal on Home makes it available here

#### Scenario: Remove a favorite from the capture screen
- **WHEN** the user unmarks a favorite from the capture screen's list
- **THEN** it disappears from the list and the original meal is no longer a favorite

### Requirement: Leave the logging flow safely
While logging a meal, the system back action SHALL navigate within the app instead of closing it. Leaving the review screen, by system back or by its cancel action, SHALL ask for confirmation first, so an unsaved analysis and its edits are not lost by accident.

#### Scenario: Back from the capture screen without a photo
- **WHEN** the user presses system back on the capture screen with no photo selected
- **THEN** the app returns to Home without closing

#### Scenario: Back from the capture screen with a photo awaiting confirmation
- **WHEN** the user presses system back while a captured or chosen photo is shown for confirmation
- **THEN** the photo is discarded and the capture screen shows the photo options again, and a further back returns to Home

#### Scenario: Back while the photo is being analyzed
- **WHEN** the user presses system back while the chosen photo is being analyzed
- **THEN** the app ignores it, so it doesn't discard the photo while the analysis still completes and opens the review

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
