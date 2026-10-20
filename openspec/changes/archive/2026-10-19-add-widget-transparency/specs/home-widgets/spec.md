## ADDED Requirements

### Requirement: Widget background transparency
Each placed widget SHALL have its own background transparency, from 0% (opaque) to 100% (no background), set from a configuration screen. A new widget SHALL start opaque. The configuration screen SHALL show a preview of the widget over the device's wallpaper that follows the chosen value before it is applied. Only the background's opacity SHALL change: buttons, progress bars and texts keep their colors. "Quick actions" keeps the theme's background, which follows the system's light or dark mode. "Today's calories" SHALL always use the dark scheme's background with white text, in light and dark mode alike, with larger type and its key figures (title, calories, calories left) in bold, so that it reads at any transparency. The value SHALL be kept across reboots and app updates, and SHALL go away with the widget.

#### Scenario: Change the transparency
- **WHEN** the user opens the widget's settings, moves the transparency to 60% and taps "Done"
- **THEN** that widget's background becomes 60% transparent, and the other widgets keep theirs

#### Scenario: New widget
- **WHEN** the user adds a widget
- **THEN** its background is opaque until the user changes it

#### Scenario: Preview
- **WHEN** the user moves the slider in the configuration screen
- **THEN** the preview over the wallpaper shows the new transparency at once, and the widget itself doesn't change until "Done"

#### Scenario: Cancel
- **WHEN** the user leaves the configuration screen without tapping "Done"
- **THEN** the widget keeps its previous transparency

#### Scenario: After a restart
- **WHEN** the phone restarts or the app is updated
- **THEN** each widget keeps the transparency it had

#### Scenario: Dark mode
- **WHEN** the system switches between light and dark mode
- **THEN** a transparent "Quick actions" background switches with it and keeps its transparency, and "Today's calories" stays dark with white text

#### Scenario: Readable over the wallpaper
- **WHEN** "Today's calories" is fully transparent
- **THEN** its white, bold figures show directly over the wallpaper
