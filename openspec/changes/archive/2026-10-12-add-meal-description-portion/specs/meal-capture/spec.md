## MODIFIED Requirements

### Requirement: Confirm the photo before analysis
The app SHALL show the selected or captured photo before analyzing it, and SHALL let the user replace it (retake or choose again) or send it for analysis, together with the optional description and portion. Camera and gallery photos SHALL follow the same analysis path afterwards.

#### Scenario: Analyze a confirmed camera photo
- **WHEN** the user confirms a captured photo for analysis
- **THEN** the app sends it to the analysis service and shows the review screen with the returned analysis

#### Scenario: Retake
- **WHEN** the user chooses to retake a captured photo
- **THEN** the app discards it and opens the camera again

## ADDED Requirements

### Requirement: Describe the meal and its portion
Before analyzing, the capture screen SHALL offer an optional description of the meal and an optional portion size: small, medium or large, with none selected by default.
- **Description:** the user SHALL be able to type it, or to dictate it with the device's speech recognition in the app's language. Dictated text SHALL be added to the description so it can be reviewed and edited before analyzing. When the device offers no speech recognition, the dictation action SHALL NOT be shown. The description SHALL be limited to 500 characters.
- **Sending:** the description and portion SHALL be sent with the analysis request.

#### Scenario: Dictate a description
- **WHEN** the user taps the microphone and says "milanesa al horno con puré"
- **THEN** the description field contains that text, which the user can edit before analyzing

#### Scenario: Choose and clear a portion
- **WHEN** the user selects "Small" and then taps it again
- **THEN** no portion is selected and none is sent

### Requirement: Log a meal by description only
The capture screen SHALL allow analyzing a meal without a photo when a description has been entered. Its analysis action SHALL be available whenever there is a photo, a non-blank description, or both. A meal analyzed from its description SHALL go through the same review and save steps as one analyzed from a photo.

#### Scenario: Snack without a photo
- **WHEN** the user types or dictates "una manzana" without taking a photo and taps analyze
- **THEN** the app shows the review screen with the estimated analysis, and saving logs the meal like any other

#### Scenario: Nothing to analyze
- **WHEN** there is no photo and the description is empty or blank
- **THEN** the analysis action is not available
