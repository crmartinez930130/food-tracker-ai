## MODIFIED Requirements

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
