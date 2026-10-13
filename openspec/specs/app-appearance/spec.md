# app-appearance Specification

## Purpose

Defines how the app looks across the system's light and dark modes, so every screen stays legible and consistent with the app's own color identity, whichever mode is in use: the device's, or the one the user chose in Options.

## Requirements

### Requirement: Brand color theme in light and dark
The app SHALL render every screen with its own brand color scheme, never the framework's default palette. The scheme SHALL be the green Greenwave palette, generated from the seed color `#81DFA2`. There SHALL be a light variant and a dark variant, and all text and icons SHALL remain legible against their backgrounds in both, meeting a 4.5:1 contrast ratio for the text color pairs the app uses.

#### Scenario: Light mode
- **WHEN** the device is in light mode and the user opens any screen
- **THEN** the screen uses the app's light color scheme and all text and icons are legible

#### Scenario: Dark mode
- **WHEN** the device is in dark mode and the user opens any screen
- **THEN** the screen uses the app's dark color scheme and all text and icons are legible

### Requirement: Follow the system mode live
The app SHALL use the theme the user chose in Options:
- with Light or Dark, it SHALL use that color scheme whatever the device's light or dark setting is;
- with System, which SHALL be the default, it SHALL follow the device's setting and SHALL update its appearance immediately when that setting changes while the app is open.

Changing the theme in Options, or the device's mode while the theme is System, SHALL take effect without restarting and without losing what is on screen. The status bar and navigation bar icons SHALL stay legible for the theme in use.

#### Scenario: Switch while browsing
- **WHEN** the theme is System and the user changes the system between light and dark mode while the app is open on any tab
- **THEN** the app switches to the matching color scheme immediately and stays on the same tab

#### Scenario: Switch while reviewing an analysis
- **WHEN** the theme is System and the user changes the system mode while reviewing a photo analysis that has not been saved yet
- **THEN** the app switches color scheme and the review screen keeps the analysis and any edits the user made

#### Scenario: Forced dark on a light device
- **WHEN** the user chooses Dark while the device is in light mode
- **THEN** every screen uses the dark color scheme and the system bar icons are light, and changing the device's mode does not change the app

#### Scenario: Forced light on a dark device
- **WHEN** the user chooses Light while the device is in dark mode
- **THEN** every screen uses the light color scheme and the system bar icons are dark

### Requirement: Legible system bars and launch background
The app SHALL keep the status bar and navigation bar icons legible in both modes, and SHALL NOT show a background of the opposite mode while launching.

#### Scenario: System bars in dark mode
- **WHEN** the app is shown in dark mode
- **THEN** the status bar and navigation bar icons are light-colored over the dark app background

#### Scenario: Launch in dark mode
- **WHEN** the user launches the app while the device is in dark mode
- **THEN** the window background shown before the first screen draws is dark, not white

### Requirement: Material icons in navigation
The main navigation and the primary capture action SHALL use Material Symbols icons, not emoji or text glyphs, and each navigation destination SHALL keep its text label.

#### Scenario: Bottom navigation
- **WHEN** the user views the main screen
- **THEN** the bottom navigation shows exactly three destinations, Home, History and Summary, in that order, each with a Material icon and its label, and the selected destination is visually distinguished

#### Scenario: Capture action
- **WHEN** the user views the main screen
- **THEN** the button that starts a new capture shows a Material "add" icon and exposes an accessible description of its action

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
