## 1. Backend

- [x] 1.1 Add `parseApiToken` and the config fields, `requireAccessToken` (D1) and `createRateLimiter` with its middleware (D2). Mount them in `createApp` and the meals router, and disable `x-powered-by`. Make `buildTestApp` send a token by default, with an option to send none. Verify with tests:
  - the config: missing, short and valid tokens;
  - the token: missing header, wrong scheme, wrong token and right token, across routes;
  - the limiter: under the limit, at it, over it, and after the window, with the `Retry-After` value;
  - the route: 429 without the analysis being called.
- [x] 1.2 Add `detectImageType` and the route check (D3), plus `limitInputPixels` and `failOn` in `preprocessImage`. Verify with tests: JPEG, PNG, an SVG declared as JPEG, a HEIF header and empty bytes.
- [x] 1.3 Upgrade `sharp`, `drizzle-orm` and `drizzle-kit` (D4) and fix any breaking changes. Update `.env.example` and the README. Verify with `npm run typecheck`, `npm test`, and `npm audit --omit=dev` (no high findings).

## 2. App

- [x] 2.1 Add `AppPreferences.accessToken`, `BackendConfig.accessToken`, `createHttpClient(tokenProvider)`, `ApiException.status` and the 401 mapping in `errorText` (D5). Wire `MainActivity` and `ReminderReceiver`. Verify with `FoodTrackerApiClientTest`: the header is sent when a token is set and not sent without one, and a 401 carries its status. Also test the preferences round-trip.
- [x] 2.2 Add the token field with the show toggle, Save, and the connection test reporting `Unauthorized` (D6), plus the icons and strings in both languages. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 3. Verification

- [x] 3.1 With the backend running on a throwaway test token (never printed), on the emulator, in light and dark and in Spanish and English:
  - without a token in the app, screens show the localized 401 message;
  - the connection test reports the rejected token;
  - after saving the right token, Home, History, Summary, analysis and saving work;
  - a reminder check uses the token;
  - `curl` without a token gets 401, and a disguised SVG gets 400;
  - with `ANALYZE_RATE_LIMIT_PER_MINUTE=1`, a second analysis within the minute gets 429 (one real call, not 21).

  Restore the app's settings afterwards. Tell the owner to add `API_TOKEN` to their `.env` and the app.
