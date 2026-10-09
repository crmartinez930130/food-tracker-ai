# meal-photo-analysis Specification

## Purpose

Turns a photo of a meal into a structured, reviewable estimate of the foods present and their nutritional content, without committing anything to the user's history.

## Requirements

### Requirement: Photo submission for analysis
The system SHALL accept a single meal photo (JPEG or PNG) submitted via the API and return a structured nutrition analysis without persisting a meal entry.

#### Scenario: Valid photo submitted
- **WHEN** the client submits a JPEG or PNG image via `POST /api/meals/analyze`
- **THEN** the system returns a structured analysis containing the detected foods, their estimated quantities, and nutrition totals, and does not create a meal entry in the history

#### Scenario: Unsupported file type submitted
- **WHEN** the client submits a file that is not a JPEG or PNG image
- **THEN** the system rejects the request with a 4xx error and does not call the vision analysis service

#### Scenario: No food detected in the photo
- **WHEN** the submitted photo contains no recognizable food
- **THEN** the system returns an analysis with an empty food list rather than an error, so the client can inform the user

### Requirement: Structured nutrition analysis output
For each analyzed photo, the system SHALL return, for every detected food item: a name, an estimated quantity in grams, calories, protein (g), fat (g), and carbohydrates (g), plus totals aggregated across all items.

#### Scenario: Multiple foods detected
- **WHEN** the photo contains more than one distinguishable food item
- **THEN** the response lists each item separately with its own estimated quantity and macros, plus a total that is the sum across all items

#### Scenario: Low-confidence estimate
- **WHEN** the vision analysis cannot confidently estimate a food item's quantity
- **THEN** the response still includes that item with a confidence indicator, rather than silently omitting it or failing the request

### Requirement: Analysis failure handling
The system SHALL return a clear error to the client when the vision analysis cannot be completed, without creating a partial or corrupt meal entry.

#### Scenario: Vision API unavailable or errors
- **WHEN** the call to the vision analysis service fails (network error, rate limit, or malformed response)
- **THEN** the system returns a 5xx (or appropriate 4xx for client-caused errors) response describing the failure, and no meal entry is created

#### Scenario: Vision API returns data that fails schema validation
- **WHEN** the vision analysis service's response does not conform to the expected structured schema
- **THEN** the system treats this as an analysis failure and returns an error rather than passing malformed data to the client

### Requirement: Analyze photos in their upright orientation
The system SHALL apply a submitted photo's orientation metadata (EXIF) before analysis, so a photo whose pixels are stored rotated is analyzed as the user saw it when taking it.

#### Scenario: Photo stored rotated with an orientation tag
- **WHEN** the client submits a JPEG whose pixels are stored rotated and whose EXIF orientation tag indicates the rotation
- **THEN** the image sent for analysis is in its upright orientation, with width and height swapped accordingly for 90° rotations

#### Scenario: Photo without orientation metadata
- **WHEN** the client submits a photo with no EXIF orientation tag
- **THEN** the image is analyzed in its stored orientation, unchanged
