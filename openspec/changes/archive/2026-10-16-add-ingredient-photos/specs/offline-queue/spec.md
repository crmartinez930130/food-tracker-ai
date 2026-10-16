## ADDED Requirements

### Requirement: Kept captures keep their ingredient photos
A capture kept without a connection SHALL also keep its ingredient photos in the app's private storage, SHALL send them when it is analyzed, and SHALL delete them when it is saved or discarded. A kept capture whose ingredient photo file is gone SHALL fail like one whose meal photo is gone.

#### Scenario: Plate and ingredients offline
- **WHEN** the user analyzes a plate photo with two ingredient photos without a connection, and the connection comes back
- **THEN** the analysis is sent with the three photos, and discarding or saving the capture deletes all three files
