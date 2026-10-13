## MODIFIED Requirements

### Requirement: Material icons in navigation
The main navigation and the primary capture action SHALL use Material Symbols icons, not emoji or text glyphs, and each navigation destination SHALL keep its text label.

#### Scenario: Bottom navigation
- **WHEN** the user views the main screen
- **THEN** the bottom navigation shows exactly three destinations, Home, History and Summary, in that order, each with a Material icon and its label, and the selected destination is visually distinguished

#### Scenario: Capture action
- **WHEN** the user views the main screen
- **THEN** the button that starts a new capture shows a Material "add" icon and exposes an accessible description of its action
