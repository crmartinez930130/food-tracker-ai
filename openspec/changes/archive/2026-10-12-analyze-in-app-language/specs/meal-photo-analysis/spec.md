## MODIFIED Requirements

### Requirement: Photo submission for analysis
The system SHALL accept a single meal photo (JPEG or PNG) submitted via the API and return a structured nutrition analysis without persisting a meal entry. The submission MAY include the language, Spanish (`es`) or English (`en`), in which the analysis texts are wanted; a missing or unrecognized language SHALL be treated as English and SHALL NOT cause the request to be rejected.

#### Scenario: Valid photo submitted
- **WHEN** the client submits a JPEG or PNG image via `POST /api/meals/analyze`
- **THEN** the system returns a structured analysis containing the detected foods, their estimated quantities, and nutrition totals, and does not create a meal entry in the history

#### Scenario: Unsupported file type submitted
- **WHEN** the client submits a file that is not a JPEG or PNG image
- **THEN** the system rejects the request with a 4xx error and does not call the vision analysis service

#### Scenario: No food detected in the photo
- **WHEN** the submitted photo contains no recognizable food
- **THEN** the system returns an analysis with an empty food list rather than an error, so the client can inform the user

#### Scenario: Language not given or unknown
- **WHEN** the client submits a photo without a language, or with a value other than `es` or `en`
- **THEN** the system analyzes it normally and writes the texts in English

### Requirement: Structured nutrition analysis output
For each analyzed photo, the system SHALL return, for every detected food item: a name, an estimated quantity in grams, calories, protein (g), fat (g), and carbohydrates (g), plus totals aggregated across all items. The food names and any notes SHALL be written in the language requested with the photo (English by default); quantities, nutrition values and confidence indicators SHALL be the same whatever the language.

#### Scenario: Multiple foods detected
- **WHEN** the photo contains more than one distinguishable food item
- **THEN** the response lists each item separately with its own estimated quantity and macros, plus a total that is the sum across all items

#### Scenario: Low-confidence estimate
- **WHEN** the vision analysis cannot confidently estimate a food item's quantity
- **THEN** the response still includes that item with a confidence indicator, rather than silently omitting it or failing the request

#### Scenario: Analysis in Spanish
- **WHEN** the app, shown in Spanish, submits a photo for analysis
- **THEN** the returned food names and notes are in Spanish, for example "Tarta de chocolate" rather than "Chocolate cake"
