## MODIFIED Requirements

### Requirement: Loading indication while Home changes day
When Home moves to a new day that has no cached copy (see `offline-cache`), it SHALL visibly mark the content on screen as outdated until the new day's data has loaded, instead of showing the previous day's data unmarked under the new date. When the new day has a cached copy, Home SHALL show that copy at once instead.

#### Scenario: Change day
- **WHEN** Home switches to a new day that isn't cached and its data is still loading
- **THEN** the previously shown content is dimmed and a progress indicator is shown, and both return to normal once the new day's data is displayed

#### Scenario: Change to a cached day
- **WHEN** Home switches to a day that has a cached copy
- **THEN** it shows that day's cached data at once, without dimming, and refreshes it in the background

#### Scenario: First load
- **WHEN** Home opens with no data loaded yet and no cached copy of the day
- **THEN** it shows a loading indicator instead of empty or zero progress
