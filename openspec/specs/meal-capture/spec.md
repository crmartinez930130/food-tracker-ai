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
