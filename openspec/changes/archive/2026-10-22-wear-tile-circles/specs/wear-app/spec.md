## MODIFIED Requirements

### Requirement: Today tile
The watch SHALL offer a "Today" tile with a title and a rounded panel holding two circles: today's calories as a progress ring with the number inside (green within the goal, red once over it), and the water as a ring with its ml. Tapping the water circle SHALL log one glass of the size set in Goals, exactly once per tap, as described in "Water from the watch". Tapping anywhere else on the panel SHALL open the watch app. When the shown data is old, or there is none for today yet, a short line SHALL say so. The tile SHALL be refreshed when the app opens, whenever a refresh brings new data, and about every 30 minutes.

#### Scenario: Add the tile
- **WHEN** the user adds the "Today" tile on the watch
- **THEN** it shows the calories and water circles with today's figures

#### Scenario: Open the app
- **WHEN** the user taps the tile outside the water circle
- **THEN** the watch app opens

#### Scenario: Log a glass from the tile
- **WHEN** the user taps the water circle once
- **THEN** one glass is logged and the circle shows the new total

#### Scenario: Over the goal
- **WHEN** today's calories are above the goal
- **THEN** the calories ring is red
