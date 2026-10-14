## MODIFIED Requirements

### Requirement: Structured nutrition analysis output
For each analyzed photo, the system SHALL return, for every detected food item: a name, an estimated quantity in grams, calories, protein (g), fat (g), and carbohydrates (g), plus totals aggregated across all items. The items SHALL be only foods and drinks: a summary row (such as "Total" or "Subtotal") or an empty placeholder (no quantity and no nutrition values) SHALL NOT be returned as an item, and the totals SHALL always equal the sum of the returned items. The food names and any notes SHALL be written in the language requested with the photo (English by default); quantities, nutrition values and confidence indicators SHALL be the same whatever the language.

#### Scenario: Multiple foods detected
- **WHEN** the photo contains more than one distinguishable food item
- **THEN** the response lists each item separately with its own estimated quantity and macros, plus a total that is the sum across all items

#### Scenario: Low-confidence estimate
- **WHEN** the vision analysis cannot confidently estimate a food item's quantity
- **THEN** the response still includes that item with a confidence indicator, rather than silently omitting it or failing the request

#### Scenario: Analysis in Spanish
- **WHEN** the app, shown in Spanish, submits a photo for analysis
- **THEN** the returned food names and notes are in Spanish, for example "Tarta de chocolate" rather than "Chocolate cake"

#### Scenario: Model adds a summary row
- **WHEN** the model's answer lists a banana, a glass of milk and an item named "Total" with all values at zero
- **THEN** the response contains only the banana and the milk, and the totals are their sum

#### Scenario: Summary row repeating the sum
- **WHEN** the model's answer includes an item named "Total" whose calories and macros equal the sum of the other items
- **THEN** that item is not returned and the totals are not doubled

#### Scenario: Empty placeholder row
- **WHEN** the model's answer includes an item with a blank or repeated name and 0 g with every value at zero
- **THEN** that item is not returned

#### Scenario: Real food with zero calories
- **WHEN** the analysis includes "Water", 250 g with 0 kcal
- **THEN** it is returned as an item
