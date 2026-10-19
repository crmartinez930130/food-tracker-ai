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

### Requirement: Empty-state illustrations
The app SHALL show a simple illustration, drawn with the theme's colors, above the message of these empty states:
- a day with no meals, on Home and on a day's detail;
- a Summary period without weight entries;
- no favorite meals on the capture screen.

The illustrations SHALL be decorative, so screen readers skip them and read only the message.

#### Scenario: Empty day
- **WHEN** today has no meals
- **THEN** Home shows the empty-plate illustration above the "nothing logged" message, in light and in dark

#### Scenario: No favorites
- **WHEN** the user has no favorite meals and opens the capture screen
- **THEN** the favorites section shows the star illustration above its message

### Requirement: Expressive motion
When animations are enabled, the app SHALL animate:
- its headline figures counting up to their value: today's calories and water on Home, and Summary's daily average and days over the goal;
- the macro, water and calorie chart bars growing into place;
- the calorie percentage badge changing shape with a spring when the day reaches 100% of the goal, and back when it goes below;
- moving to a deeper screen and back with a directional slide and fade, and switching Main's tabs with a cross-fade;
- the main buttons responding to touch with a brief press effect.

Navigation SHALL NOT flicker or show a screen's content blank during a transition.

#### Scenario: Reaching the goal
- **WHEN** the user logs a meal that takes today from 90% to 105% of the goal
- **THEN** on returning to Home the calorie figure counts up to the new total and the badge morphs to the goal-reached shape

#### Scenario: Open and leave a screen
- **WHEN** the user opens the goal screen from Home and goes back
- **THEN** the goal screen slides in from the right and, on back, slides out to the right, without a blank frame

#### Scenario: Discard a review
- **WHEN** the user discards a review and the app returns to Home
- **THEN** the review screen keeps its content while it animates out

### Requirement: Respect the system's reduced motion
When the system asks to remove animations (on Android, an animator duration scale of 0), the app SHALL show the final state of every figure, bar and badge at once, SHALL change screens and tabs without transitions, SHALL NOT apply the press effect, and SHALL show the loading indicator as a still shape.

#### Scenario: Animations removed
- **WHEN** "Remove animations" is on and the user opens Home
- **THEN** the calories, water and bars appear directly at their values, and opening the goal screen shows it without a slide

#### Scenario: Option changed while the app is in the background
- **WHEN** the user turns "Remove animations" off in system settings and returns to the app
- **THEN** the animations are back

### Requirement: App icon
The app SHALL have its own launcher icon: a green apple with a leaf and a sparkle cut out of it, over a light green background. The icon SHALL be an adaptive icon whose drawing fits the safe zone, so it is never cut by the launcher's mask, and SHALL include a monochrome layer for themed icons.

#### Scenario: Launcher icon
- **WHEN** the user looks for the app in the launcher, whatever the launcher's icon shape (circle, squircle)
- **THEN** it shows the apple icon, whole, instead of Android's generic icon

#### Scenario: Themed icons
- **WHEN** the device has themed icons turned on (Android 13+)
- **THEN** the apple icon is tinted with the system's colors like the other themed apps, with the sparkle still visible

#### Scenario: Recents and launch screen
- **WHEN** the user opens the app or looks at it in Recents
- **THEN** the launch screen (Android 12+) and Recents show the apple icon
