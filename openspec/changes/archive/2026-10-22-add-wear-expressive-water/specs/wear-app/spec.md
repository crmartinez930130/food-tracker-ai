## ADDED Requirements

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
