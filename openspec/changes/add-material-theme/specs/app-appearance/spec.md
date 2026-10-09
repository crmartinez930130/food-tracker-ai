## Purpose

Defines how the app looks across the system's light and dark modes, so every screen stays legible and consistent with the app's own color identity, whichever mode the device is in.

## ADDED Requirements

### Requirement: Brand color theme in light and dark
The app SHALL render every screen with its own brand color scheme, never the framework's default palette. There SHALL be a light variant and a dark variant, and all text and icons SHALL remain legible against their backgrounds in both.

#### Scenario: Light mode
- **WHEN** the device is in light mode and the user opens any screen
- **THEN** the screen uses the app's light color scheme and all text and icons are legible

#### Scenario: Dark mode
- **WHEN** the device is in dark mode and the user opens any screen
- **THEN** the screen uses the app's dark color scheme and all text and icons are legible

### Requirement: Follow the system mode live
The app SHALL follow the device's light/dark setting and SHALL update its appearance immediately when that setting changes while the app is open, without restarting and without losing what is on screen.

#### Scenario: Switch while browsing
- **WHEN** the user changes the system between light and dark mode while the app is open on any tab
- **THEN** the app switches to the matching color scheme immediately and stays on the same tab

#### Scenario: Switch while reviewing an analysis
- **WHEN** the user changes the system mode while reviewing a photo analysis that has not been saved yet
- **THEN** the app switches color scheme and the review screen keeps the analysis and any edits the user made

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
- **THEN** the History, Summary and Goals destinations each show a Material icon with its label, and the selected destination is visually distinguished

#### Scenario: Capture action
- **WHEN** the user views the main screen
- **THEN** the button that starts a new capture shows a Material "add" icon and exposes an accessible description of its action
