## ADDED Requirements

### Requirement: Analyze photos in their upright orientation
The system SHALL apply a submitted photo's orientation metadata (EXIF) before analysis, so a photo whose pixels are stored rotated is analyzed as the user saw it when taking it.

#### Scenario: Photo stored rotated with an orientation tag
- **WHEN** the client submits a JPEG whose pixels are stored rotated and whose EXIF orientation tag indicates the rotation
- **THEN** the image sent for analysis is in its upright orientation, with width and height swapped accordingly for 90° rotations

#### Scenario: Photo without orientation metadata
- **WHEN** the client submits a photo with no EXIF orientation tag
- **THEN** the image is analyzed in its stored orientation, unchanged
