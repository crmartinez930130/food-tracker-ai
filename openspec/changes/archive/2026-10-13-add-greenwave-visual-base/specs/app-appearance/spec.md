## MODIFIED Requirements

### Requirement: Brand color theme in light and dark
The app SHALL render every screen with its own brand color scheme, never the framework's default palette. The scheme SHALL be the green Greenwave palette, generated from the seed color `#81DFA2`. There SHALL be a light variant and a dark variant, and all text and icons SHALL remain legible against their backgrounds in both, meeting a 4.5:1 contrast ratio for the text color pairs the app uses.

#### Scenario: Light mode
- **WHEN** the device is in light mode and the user opens any screen
- **THEN** the screen uses the app's light color scheme and all text and icons are legible

#### Scenario: Dark mode
- **WHEN** the device is in dark mode and the user opens any screen
- **THEN** the screen uses the app's dark color scheme and all text and icons are legible

## ADDED Requirements

### Requirement: Brand typeface and shapes
All text in the app SHALL use the Google Sans Flex typeface, bundled with the app under its open font license so no network is needed. Headlines and large figures SHALL use heavier weights than body text. Cards, sheets, dialogs, fields and buttons SHALL use the app's rounded corner scale.

#### Scenario: Any screen
- **WHEN** the user opens any screen, online or offline
- **THEN** its text is set in Google Sans Flex, and its cards and buttons have rounded corners

### Requirement: Expressive loading indicator
While a screen or a section loads with nothing to show yet, or while a meal is being analyzed, the app SHALL show an animated loading indicator that changes shape. Small progress indicators inside buttons MAY remain circular.

#### Scenario: Analyzing a meal
- **WHEN** the user taps analyze on the capture screen
- **THEN** a shape-changing loading indicator is shown until the analysis finishes or fails
