## Why

The app is about to queue meals and water logged without a connection and send them later (#69, #70). A queued send can reach the backend and be stored while the response is lost (Wi-Fi drops, app killed), so the app retries it, and today every retry creates another meal or water entry: the user's totals silently double. The backend needs a way to recognise a retry of something it already stored.

Tracked as GitHub issue #68: https://github.com/crmartinez930130/food-tracker-ai/issues/68

## What Changes

- `POST /api/meals` and `POST /api/water` accept an optional `clientId`: a UUID the phone generates once per entry and repeats on every retry.
- The backend stores it with the entry, unique per table. When a request arrives with a `clientId` that is already stored, nothing new is created: the response is **200** with the entry already stored, in the same shape as a creation (201). The repeated request's other fields are ignored.
- A `clientId` that isn't a UUID is rejected with **400** and nothing is stored. UUIDs are compared case-insensitively.
- Meal and water entries in API responses include `clientId` (`null` when the entry was created without one), so the app can match its queued entries with the stored ones.
- Requests without `clientId` behave exactly as today. Nothing is breaking: the field is optional and existing entries get `null`.
- A database migration adds a nullable `client_id` column with a unique index to `meals` and `water_logs`.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `meal-tracking`: saving a meal accepts a client id, and a repeated save with the same id returns the stored meal instead of creating another.
- `water-tracking`: logging water accepts a client id, with the same replay behaviour.

## Impact

- **Backend:** `src/db/schema.ts` and a new generated migration under `src/db/migrations/`; `src/db/meal-repository.ts` and `src/db/water-repository.ts` (look up by client id, insert); `src/domain/meal.ts` (`clientId` on input and entry); `src/routes/schemas.ts`, `src/routes/meals.ts` and `src/routes/water.ts` (validation and 200 vs 201); tests in `test/routes` and `test/db`.
- **API:** additive only - a new optional request field and a new response field.
- **App:** no change in this issue; it starts sending `clientId` in #70. Its `kotlinx.serialization` JSON already ignores unknown response fields.
- **Deployments:** the migration runs automatically on startup, on the PC and on the laptop backend once it is redeployed.
