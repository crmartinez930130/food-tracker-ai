## ADDED Requirements

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
