## Why

The pre-publication security review found the backend is the weak point, whether the repository is public or not:
- **No authentication:** it listens on `0.0.0.0:3000`, with the port open to the LAN. Anyone on the same Wi-Fi can read, change or delete the user's meals, weight and water, and run `/api/meals/analyze` on the owner's Anthropic key.
- **Browser attacks:** a web page opened on that network can post a multipart form to `/analyze` without a CORS preflight, and DNS rebinding could read data.
- **Untrusted images:** the uploaded image is checked only by the client-declared mimetype before `sharp` decodes it, and `sharp` ≤ 0.35.5 has known high-severity issues in libvips, libheif and librsvg.
- **Outdated `drizzle-orm`:** the version in use has a high SQL-identifier advisory. It isn't exploitable here, but it should be fixed.
- **No limit on `/analyze`:** each call costs money.

Tracked as GitHub issue #60: https://github.com/crmartinez930130/food-tracker-ai/issues/60

## What Changes

- **Access token:**
  - a required `API_TOKEN` environment variable, at least 16 characters, without which the server refuses to start;
  - every `/api/*` request must carry `Authorization: Bearer <token>`, otherwise it gets **401**;
  - the comparison is constant-time.

  A foreign web page can't know the token, and can't send that header without a preflight. That also closes the cross-site form and DNS-rebinding paths.
- **Analysis limit:** `/api/meals/analyze` allows a fixed number of analyses per minute (20 by default, configurable), and answers **429** with `Retry-After` past it. The check runs before the upload is parsed.
- **Image check by content:** the photo's first bytes must be a JPEG or PNG signature before anything decodes it. Any other content gets 400, whatever mimetype it declares.
- **Dependencies:** `sharp` 0.35.5 and `drizzle-orm` 0.45.4 (with a matching `drizzle-kit`), and `X-Powered-By` disabled.
- **App:**
  - an "Access token" field in Options → Server, hidden like a password with a show toggle, saved with the address;
  - every request sends the token, including the reminder's meal check;
  - "Test connection" tests the address and token being edited, and reports a rejected token separately;
  - a 401 anywhere shows a localized "the server rejected the access token, check it in Options" message, instead of the backend's English text.

## Capabilities

### New Capabilities
- `api-access`: who may use the backend API (the access token) and how often the paid analysis may be called.

### Modified Capabilities
- `meal-photo-analysis`: the photo's format is checked by its content, not by its declared type.
- `app-settings`: the server settings include the access token, and the connection test reports a rejected token.

## Impact

- **Backend:**
  - `config.ts` adds `API_TOKEN` (required) and `ANALYZE_RATE_LIMIT_PER_MINUTE`;
  - new `src/http/access-token.ts` and `src/http/rate-limit.ts`, both pure and middleware;
  - `app.ts` adds the auth middleware and drops `x-powered-by`;
  - the meals router adds the limit and the content check;
  - `image-preprocessing.ts` adds `detectImageType`;
  - `.env.example` and the README document how to generate a token;
  - the test helper sends a token by default.
- **App:**
  - `AppPreferences.accessToken`, `BackendConfig.accessToken`;
  - `createHttpClient(tokenProvider)` adds the header to every request;
  - `ApiException.status`, and `errorText` maps 401;
  - the Options server card gets the new field;
  - `ReminderReceiver` and `MainActivity` pass the token;
  - new `visibility` / `visibility_off` icons, and strings.
- **Deployment:** after this change the owner must add `API_TOKEN` to `backend/.env` and enter the same token in the app. Until then, every request gets 401.
