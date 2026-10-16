## ADDED Requirements

### Requirement: Ingredient photos
The analysis endpoint SHALL accept, besides the optional meal photo, up to 5 optional ingredient photos. Each SHALL be accepted only when its content is a JPEG or PNG image of at most 15 MB, and all photos of one request together SHALL be at most 40 MB. A request with more than 5 ingredient photos, with an ingredient file that isn't a JPEG or PNG image, or over the total size SHALL get a 400 with an error message, without calling the model. A request SHALL be analyzable when it has a meal photo, at least one ingredient photo, or a description. The model SHALL receive each photo labelled as the meal or as an ingredient used to prepare it, and SHALL be instructed to estimate what was eaten, using the ingredient photos for what the meal photo can't show (amounts, nutrition labels, brands) without counting an ingredient twice.

#### Scenario: Meal and ingredient photos
- **WHEN** the client sends a meal photo and two ingredient photos
- **THEN** the model receives the three photos, each with its label, and the analysis is returned as usual

#### Scenario: Ingredients only
- **WHEN** the client sends two ingredient photos and a description, without a meal photo
- **THEN** the analysis runs on them

#### Scenario: Too many ingredient photos
- **WHEN** the client sends six ingredient photos
- **THEN** the response is 400 and the model isn't called

#### Scenario: Ingredient file that isn't an image
- **WHEN** one of the ingredient files is a PDF declared as a JPEG
- **THEN** the response is 400 and the model isn't called
