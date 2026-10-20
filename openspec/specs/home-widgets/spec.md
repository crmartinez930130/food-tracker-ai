# home-widgets Specification

## Purpose
Android home-screen widgets that show how today is going against the goal and offer one-tap actions (a glass of water, logging a meal), kept current from the phone's copy and the offline queue without opening the app, with a background transparency each widget can set.

## Requirements

### Requirement: Today's calories widget
The app SHALL offer a home-screen widget that shows today's intake as Home shows it: calories against the daily goal with the calories left, protein, fat and carbs, and water against its target. Meals and glasses still waiting in the offline queue SHALL count. Tapping the widget SHALL open the app on Home. When there is no copy of today on the phone, the widget SHALL show zero intake against the stored goal (or the default goal).

#### Scenario: Matches Home
- **WHEN** the user has logged meals and water today and looks at the widget
- **THEN** it shows the same calories, macros and water as Home, including entries still waiting to be sent

#### Scenario: Open the app
- **WHEN** the user taps the widget
- **THEN** the app opens on Home

#### Scenario: Narrow widget
- **WHEN** the widget is resized narrower than its macros fit
- **THEN** it shows only the calories and the progress

### Requirement: Quick actions widget
The app SHALL offer a home-screen widget with two actions: logging one glass of water of the size set in Goals, and opening the meal capture screen. A glass logged from the widget SHALL go through the offline queue like one logged on Home: it is sent right away when the backend answers, kept and sent later exactly once when it doesn't, and shown at once in both widgets and in the app.

#### Scenario: Log a glass online
- **WHEN** the user taps the glass action while the backend can be reached
- **THEN** a glass of the configured size is stored for today, and the widgets and Home show the new total

#### Scenario: Log a glass offline
- **WHEN** the user taps the glass action without a connection
- **THEN** the glass counts at once as pending, and is sent once when the backend can be reached again

#### Scenario: Log a meal
- **WHEN** the user taps the meal action
- **THEN** the app opens on the meal capture screen

### Requirement: Widgets stay current
The widgets SHALL be redrawn whenever the phone's copy of today, the goal or the offline queue changes, whatever changed it. They SHALL switch to the new day at midnight, and, while at least one widget is placed, SHALL refresh today from the backend about every 30 minutes when there is a network. When the shown data is older than 60 minutes, the widget SHALL say from what time it is; when there is no data for today yet, it SHALL say so.

#### Scenario: Change in the app
- **WHEN** the user saves a meal or changes the goal in the app
- **THEN** the widgets show the change without the user doing anything else

#### Scenario: Logged elsewhere
- **WHEN** a meal is logged for today from another device while the phone has a network
- **THEN** the widget shows it within about 30 minutes

#### Scenario: New day
- **WHEN** local midnight passes
- **THEN** the widget shows the new day, with zero intake until something is logged

#### Scenario: Old data
- **WHEN** the backend can't be reached and the phone's copy of today is more than 60 minutes old
- **THEN** the widget shows that copy with a note saying from what time it is

#### Scenario: No widget placed
- **WHEN** no widget is on the home screen
- **THEN** no background refresh runs for widgets

### Requirement: Widget look and language
The widgets SHALL use the app's colors: "Quick actions" follows the system's light or dark mode, and "Today's calories" uses the dark scheme's background with white text in both modes (see the widget background transparency). They SHALL show their texts in the language chosen in the app, and SHALL offer a preview in the widget picker.

#### Scenario: Dark mode
- **WHEN** the system is in dark mode
- **THEN** "Quick actions" uses the app's dark colors, and "Today's calories" looks as it does in light mode

#### Scenario: Language
- **WHEN** the user chose English in the app on a phone set to Spanish
- **THEN** the widgets' texts are in English

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
