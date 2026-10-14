## MODIFIED Requirements

### Requirement: Photo submission for analysis
The system SHALL accept a meal for analysis via the API, given as a single photo (JPEG or PNG, recognized by the file's content rather than its declared type), a text description, or both, and SHALL return a structured nutrition analysis without persisting a meal entry. A request with neither a photo nor a non-blank description SHALL be rejected with a 400-style error. The submission MAY include the language, Spanish (`es`) or English (`en`), in which the analysis texts are wanted; a missing or unrecognized language SHALL be treated as English and SHALL NOT cause the request to be rejected.

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

#### Scenario: File disguised as a JPEG
- **WHEN** the client submits an SVG or HEIF file declared as `image/jpeg`
- **THEN** the system rejects it with a 400-style error without decoding it and without calling the analysis service
