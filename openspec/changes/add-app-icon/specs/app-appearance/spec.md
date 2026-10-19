## ADDED Requirements

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
