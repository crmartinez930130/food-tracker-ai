## MODIFIED Requirements

### Requirement: Log a meal by description only
The capture screen SHALL allow analyzing a meal without a photo when a description has been entered. Its analysis action SHALL be available whenever there is a meal photo, an ingredient photo or a non-blank description. A meal analyzed from its description SHALL go through the same review and save steps as one analyzed from a photo.

#### Scenario: Snack without a photo
- **WHEN** the user types or dictates "una manzana" without taking a photo and taps analyze
- **THEN** the app shows the review screen with the estimated analysis, and saving logs the meal like any other

#### Scenario: Nothing to analyze
- **WHEN** there is no meal photo, no ingredient photo, and the description is empty or blank
- **THEN** the analysis action is not available

## ADDED Requirements

### Requirement: Add ingredient photos
The capture screen SHALL offer an optional "Ingredients" section where the user can add photos of the ingredients used, from the in-app camera or from the gallery (choosing several at once), up to 5. Added photos SHALL be shown as thumbnails, each with an action to remove it, and the add actions SHALL disappear once there are 5. The photos SHALL be sent with the analysis. Photos SHALL be reduced on the phone before sending to the size the backend analyzes (a long edge of 1568 px). The texts SHALL be available in Spanish and English.

#### Scenario: Plate and label
- **WHEN** the user takes a photo of the plate, adds a photo of the yogurt's nutrition label and taps analyze
- **THEN** both photos are sent and Review shows the analysis

#### Scenario: Several from the gallery
- **WHEN** the user chooses three photos at once from the gallery for the ingredients
- **THEN** the three appear as thumbnails, and two more can still be added

#### Scenario: Remove one
- **WHEN** the user removes an ingredient thumbnail
- **THEN** it isn't sent with the analysis
