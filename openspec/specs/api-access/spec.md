# api-access Specification

## Purpose
Controls who may use the backend API and how often the paid meal analysis may be called, so the backend is safe to run on a shared home network.

## Requirements

### Requirement: Access token on every API request
The backend SHALL require an access token, configured by the owner, on every request under `/api/`.
- **Configuration:** the server SHALL refuse to start without a token, or with one shorter than 16 characters.
- **Sending:** a request SHALL carry the token as `Authorization: Bearer <token>`.
- **Rejection:** a request without the header, with another scheme, or with a different token SHALL be rejected with 401 and an error message, before any data is read or changed and before any analysis is attempted.
- **Comparison:** it SHALL NOT reveal through timing how close a wrong token is to the right one.

#### Scenario: Request without a token
- **WHEN** a client calls `GET /api/goals` without an `Authorization` header
- **THEN** the response is 401 with an error message and no data

#### Scenario: Wrong token
- **WHEN** a client sends `Authorization: Bearer` with a token that differs from the configured one
- **THEN** the response is 401 and nothing is created, changed or analyzed

#### Scenario: Correct token
- **WHEN** the client sends the configured token
- **THEN** the API behaves exactly as without this requirement

#### Scenario: Missing configuration
- **WHEN** the server starts without a token configured, or with one shorter than 16 characters
- **THEN** it exits with a message explaining how to set one

#### Scenario: Form posted from another website
- **WHEN** a web page on another origin posts a meal photo form to `/api/meals/analyze`
- **THEN** the request carries no token and is rejected with 401, so no analysis runs

### Requirement: Analysis rate limit
The backend SHALL accept at most a configured number of meal analyses per minute (20 by default). An analysis request over the limit SHALL be rejected with 429, an error message and a `Retry-After` header, without reading the uploaded photo and without calling the analysis service.

#### Scenario: Over the limit
- **WHEN** the 21st analysis request arrives within one minute with the default limit
- **THEN** it gets 429 with `Retry-After`, and the analysis service is not called

#### Scenario: After the window
- **WHEN** a minute has passed since the oldest counted analysis
- **THEN** a new analysis is accepted
