# wear-app Specification

## Purpose
The Wear OS companion app: today's progress on the watch (an app screen and a "Today" tile), with the backend settings sent by the phone app over the Wear OS Data Layer so nothing is typed on the watch.

## Requirements

### Requirement: Settings from the phone
The watch app SHALL get the backend address, access token and app language from the phone app over the Wear OS Data Layer, without the user typing anything on the watch. The phone app SHALL send them when it opens and whenever they are saved in Options. The watch SHALL keep them only on the watch, excluded from backups. Until it has them, the watch app and its tile SHALL ask the user to open Food Tracker on the phone.

#### Scenario: First time
- **WHEN** the watch app is installed and the user opens Food Tracker on the phone
- **THEN** the watch receives the address and token, and shows today's data without any setup on the watch

#### Scenario: Token changed on the phone
- **WHEN** the user saves a new token in the phone app's Options
- **THEN** the watch uses the new token from then on

#### Scenario: Never configured
- **WHEN** the watch app opens and no settings have arrived yet
- **THEN** it shows a message asking to open Food Tracker on the phone

### Requirement: Today on the watch
The watch app SHALL show today's calories against the goal with what is left (or over), protein, fat and carbs, and water against its target, as the phone shows them. It SHALL refresh from the backend when opened, and SHALL keep showing its last copy with "No connection · data from HH:mm" when the backend can't be reached and the copy is more than 60 minutes old.

#### Scenario: Matches the phone
- **WHEN** the user opens the watch app with the backend reachable
- **THEN** it shows the same calories, macros and water as Home on the phone

#### Scenario: Without a connection
- **WHEN** the backend can't be reached and the watch's copy of today is more than an hour old
- **THEN** the watch shows that copy with a note saying from what time it is

### Requirement: Today tile
The watch SHALL offer a "Today" tile with today's calories as a progress ring, the calories left, and the water. Tapping it SHALL open the watch app. The tile SHALL be refreshed when the app opens, whenever a refresh brings new data, and about every 30 minutes.

#### Scenario: Add the tile
- **WHEN** the user adds the "Today" tile on the watch
- **THEN** it shows today's calories, the calories left and the water

#### Scenario: Open the app
- **WHEN** the user taps the tile
- **THEN** the watch app opens

### Requirement: Watch language
The watch app and tile SHALL show their texts in the language chosen in the phone app, Spanish or English, and in the watch's own language until the phone has sent one.

#### Scenario: English on the phone
- **WHEN** the phone app is set to English
- **THEN** the watch app and tile show English texts

### Requirement: Water from the watch
The watch app SHALL offer a "+ glass" button at the bottom edge of the "Today" screen, and the "Today" tile SHALL offer one too, both logging one glass of the size set in Goals without opening anything else. A glass SHALL go through the offline queue as on the phone: sent at once when the backend answers, kept on the watch and sent exactly once when it can be reached again otherwise, and counted at once on the watch screen and tile.

#### Scenario: From the app
- **WHEN** the user taps "+ 250 ml" on the watch app with the backend reachable
- **THEN** the glass is stored, the watch shows the new total, and the phone shows it after its next refresh

#### Scenario: From the tile
- **WHEN** the user taps the tile's "+ 250 ml" button
- **THEN** the glass is logged and the tile shows the new water total, without opening the app

#### Scenario: Offline
- **WHEN** the user logs a glass on the watch while the backend can't be reached
- **THEN** it counts at once on the watch and is sent once when the backend answers again

### Requirement: Food Tracker colors on the watch
The watch app and the "Today" tile SHALL use Food Tracker's dark color scheme (its green), not the watch's dynamic colors.

#### Scenario: Watch face with another color
- **WHEN** the watch face's theme is blue
- **THEN** the Food Tracker app and tile still use Food Tracker's green

### Requirement: Expressive "Today" screen
The watch app's "Today" screen SHALL show calories, macros and water as cards that scale and curve near the screen's edges while scrolling, following Wear OS's Material 3 Expressive lists, with a calories progress ring around the screen.

#### Scenario: Scrolling
- **WHEN** the user scrolls the "Today" screen
- **THEN** the cards shrink and round off as they approach the top and bottom edges, and the calories ring stays in place
