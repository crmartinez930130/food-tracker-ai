## MODIFIED Requirements

### Requirement: Photo submission for analysis
The system SHALL accept a meal for analysis via the API, given as a single photo (JPEG or PNG), a text description, or both, and SHALL return a structured nutrition analysis without persisting a meal entry. A request with neither a photo nor a non-blank description SHALL be rejected with a 400-style error. The submission MAY include the language, Spanish (`es`) or English (`en`), in which the analysis texts are wanted; a missing or unrecognized language SHALL be treated as English and SHALL NOT cause the request to be rejected.

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

#### Scenario: Neither photo nor description
- **WHEN** the client calls `POST /api/meals/analyze` without a photo and without a description, or with a blank description only
- **THEN** the system rejects the request with a 400-style error and does not call the analysis service

## ADDED Requirements

### Requirement: Description and portion context
The analysis request MAY include a free-text description of the meal of at most 500 characters, and a portion size of small, medium or large.
- **Description:** when given, the analysis SHALL take it into account as information about the food, for example how it was cooked, its ingredients or how much of it was eaten. It SHALL take precedence over what a photo cannot show. A description longer than 500 characters SHALL be rejected with a 400-style error that explains the limit.
- **Portion:** when given, the estimated quantities SHALL follow it. An unrecognized portion value SHALL be ignored rather than rejected.

#### Scenario: Description refines the estimate
- **WHEN** the client submits a photo with the description "sin aceite, pollo al horno"
- **THEN** the analysis uses that information, for example by not adding fat for frying oil

#### Scenario: Portion given
- **WHEN** the client submits a photo with the portion "small"
- **THEN** the estimated quantities lean toward the lower end of a typical serving

#### Scenario: Description too long
- **WHEN** the description has more than 500 characters
- **THEN** the system rejects the request with a 400-style error naming the limit

#### Scenario: Unknown portion
- **WHEN** the portion is not small, medium or large
- **THEN** the system analyzes the meal as if no portion had been given

### Requirement: Analysis from a description only
When no photo is given, the system SHALL estimate the meal from the description alone and return the same structured analysis as for a photo, written in the requested language. A description that names no food SHALL produce an analysis with an empty food list rather than an error.

#### Scenario: Snack described in words
- **WHEN** the client submits only the description "una manzana y un yogur bebible chico"
- **THEN** the system returns an analysis listing an apple and a small drinkable yogurt with estimated quantities and nutrition
