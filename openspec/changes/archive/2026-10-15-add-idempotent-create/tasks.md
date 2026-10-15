## 1. Database

- [x] 1.1 Add a nullable `clientId` (`client_id`) text column with a unique index to `meals` and `water_logs` in `src/db/schema.ts`, and generate the migration with `npm run db:generate` (D2). Verify that the generated SQL only adds the columns and indexes, and with a test that opens a database at the previous migration with meals and water in it, applies the new one, and finds the old rows intact with `clientId` `null`.

## 2. Meals

- [x] 2.1 Add `clientId` to `SaveMealInput` and `MealEntry` (`src/domain/meal.ts`) and to `toMealEntry`. Add `saveMeal`, which looks up by `clientId` and inserts the meal and its items in one transaction, returning `{ entry, created }`; `createMeal` keeps returning the entry, through it (D3, D4). Verify with `test/db/meal-repository.test.ts`: a first save creates and returns `created: true`; a second save with the same id returns the stored meal with `created: false` and the meal count stays at one; a save without `clientId` always creates; the stored meal has its items.
- [x] 2.2 Add `clientId: z.uuid()` (optional, lower-cased) to `SaveMealRequestSchema` and answer 201 or 200 from `POST /api/meals` (D1, D3, D5). Verify with `test/routes/meals.test.ts`: 201 with `clientId` on the first save; 200 with the same body on a retry, with a different body, and with the id in upper case, and `GET /api/meals?date=` lists the meal once; 400 for a non-UUID id with nothing stored; two saves without `clientId` create two meals, both with `clientId: null`.

## 3. Water

- [x] 3.1 Add `logWater`, which takes an optional `clientId`, looks it up and inserts in one transaction, returning `{ entry, created }`; `createWaterLog` keeps returning the entry, through it (D3). Verify with `test/db/water-repository.test.ts`: first log creates, a retry returns the stored entry without adding to the day's total, and logs without `clientId` always create.
- [x] 3.2 Add `clientId` to `LogWaterRequestSchema` and answer 201 or 200 from `POST /api/water` (D1, D3, D5). Verify with `test/routes/water.test.ts`: 201 then 200 on a retry with the entry stored once in `GET /api/water`, upper-case id treated as the same entry, 400 for a non-UUID id, and responses including `clientId` (`null` when not sent).

## 4. Verification

- [x] 4.1 Run `npm run typecheck` and `npm test`. Then, against a copy of a real database started with a throwaway test token (never printed): the migration applies on startup, existing meals and water still appear, and `curl` shows 201 then 200 for a repeated `POST /api/meals` and `POST /api/water` with the same `clientId`, with the day listing each once. Delete the test entries afterwards.
