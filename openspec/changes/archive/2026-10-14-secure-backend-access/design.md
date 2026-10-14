## Context

- **The backend:** `createApp` wires express with `express.json()` and five routers under `/api/*`, plus an error handler. `config.ts` reads `ANTHROPIC_API_KEY` (required), and `HOST` (default `0.0.0.0`), `PORT` and `DB_PATH`. The analysis route uses multer with memory storage, a 15 MB limit and a `fileFilter` on the client-declared mimetype, then `preprocessImage` (sharp: rotate, resize, JPEG).
- **Backend tests:** 127 `request(app)` calls across the route tests use `buildTestApp()`.
- **The app:**
  - one `FoodTrackerApiClient(createHttpClient())` lives in `App`;
  - `BackendConfig.baseUrl` is set by `MainActivity` from `AppPreferences`;
  - the reminder receiver builds its own client from the stored preferences;
  - "Test connection" builds a throwaway client for the typed address;
  - errors become `ApiException(message)` with the backend's `{error}` text, and screens show it through `errorText`.

## Goals / Non-Goals

**Goals:**
- No API access without the owner's token. That covers LAN users, cross-site forms and DNS rebinding.
- A bounded cost for `/analyze`.
- Untrusted bytes reach `sharp` only if they are JPEG or PNG.
- No known high-severity vulnerabilities in production dependencies.
- The app keeps working once the token is set, with clear messages when it isn't.

**Non-Goals:**
- HTTPS and exposure to the internet.
- Users and accounts.
- Token rotation in the UI beyond editing the field.
- Keeping the token out of Android backups (#61).
- GitHub settings (#62).

## Decisions

### D1. One static bearer token
- **Configuration:**
  - `API_TOKEN` is required. `parseApiToken(value)` is pure and tested: it rejects a missing value and one shorter than 16 characters, with a message that suggests `openssl rand -hex 24`.
  - `.env.example` documents the variable with a placeholder (`change-me-to-a-random-value`), and the README explains the setup.
  - The placeholder is long enough to pass the length check, and it is public. So `parseApiToken` rejects that exact value too: a copied `.env.example` can't leave the backend open with a known token.
- **The middleware:** `requireAccessToken(expected)` is mounted on `/api` before the routers.
  - It reads `Authorization`, requires the `Bearer ` scheme (case-insensitive), and compares the SHA-256 digests of the given and expected tokens with `crypto.timingSafeEqual`. Equal-length buffers mean neither the length nor the content leaks through timing.
  - On failure it answers 401 with `{ "error": "Missing or invalid access token" }` and `WWW-Authenticate: Bearer`.
- **Why a static token:** this is a single-user, home-network backend. A random shared secret entered once in the app is enough, and it needs no login flow.
- **Why the token covers the browser attacks:**
  - a cross-site form can't set `Authorization`, and a script would need a CORS preflight, which is not answered;
  - a DNS-rebound page runs on the attacker's origin and doesn't know the token.

  So no `Host` allowlist is needed. That avoids breaking the phone's changing LAN address.

### D2. A global sliding-window limit on analysis
- **The limiter:** `createRateLimiter({ max, windowMs, now })` keeps the timestamps of accepted calls in the last window.
  - `tryAcquire()` returns `{ allowed: true }`, or `{ allowed: false, retryAfterSeconds }`.
  - It is pure, with an injected clock, and tested.
- **The middleware:** it runs on `POST /api/meals/analyze` after the token check and **before multer**, so a rejected call never buffers an upload. Because it counts before the upload is read, every authorized attempt counts toward the limit, even one rejected afterwards (for example, an unsupported image). That was seen during verification, and it's intended: the limit bounds attempts, not only paid calls. A rejected call gets 429 `{ "error": "Too many analyses, try again in N s" }` with `Retry-After`.
- **Global, not per IP:** there's one user, and requests already passed the token, so the limit is about cost and a runaway client, not fairness.
- **Default:** 20 per minute, configurable with `ANALYZE_RATE_LIMIT_PER_MINUTE`.

### D3. Image type by signature
- **The check:** `detectImageType(buffer)` returns `"jpeg"` for `FF D8 FF`, `"png"` for `89 50 4E 47 0D 0A 1A 0A`, and `null` otherwise. It is pure and tested, including an SVG and a HEIF header labeled `image/jpeg`.
- **Where:** the route calls it before `preprocessImage`, and `null` becomes 400 "Unsupported image format; only JPEG and PNG are accepted".
- **The multer filter stays** as a cheap early rejection.
- **Defense in depth:** `preprocessImage` passes `limitInputPixels` explicitly (24 MP, ample for phone photos) and `failOn: "error"`.

### D4. Dependency upgrades
- **Versions:** `sharp` ^0.35.5, `drizzle-orm` ^0.45.4 and `drizzle-kit` at the matching release.
- **multer 2.x:** a clean `npm ci` flagged multer 1.x as deprecated for vulnerabilities fixed in 2.x, although `npm audit` didn't report them. multer is the upload parser, so it's upgraded too. It needs no code change.
- **Upload errors:** while smoke-testing multer 2, a photo over 15 MB returned 500, because multer's `LIMIT_FILE_SIZE` error wasn't mapped. The error handler now maps `MulterError`: 413 "Photo too large; the limit is 15 MB" for size, and 400 "Invalid upload: …" otherwise. Both are tested.
- **`allowScripts`:** npm 11 reads `allowScripts`. The new `drizzle-kit` brings `esbuild@0.25.12`, whose postinstall is approved, and `sharp` 0.35 no longer has an install script, so its entry is gone.
- **Breaking changes** are handled where tests show them. No schema or migration change is expected.
- **Express:** `app.disable("x-powered-by")`.
- **Check:** `npm audit --omit=dev` must report no high or critical findings.

### D5. App: the token travels with every request
- **Storage:** `AppPreferences` gains `accessToken: String?`, trimmed, with blank meaning null. It is stored like the other preferences (backups are #61).
- **The header:** `createHttpClient(engine, tokenProvider: () -> String? = { BackendConfig.accessToken })` installs `defaultRequest`, which adds `Authorization: Bearer …` when the token is non-null. `defaultRequest` runs per request, so a saved token applies immediately.
  - `MainActivity` sets `BackendConfig.accessToken` next to `baseUrl`;
  - the reminder receiver passes the stored token;
  - "Test connection" passes the typed one.
- **Errors:** `ApiException` gains `status: Int`. `errorText` maps 401 to the localized `error_unauthorized` ("El servidor rechazó el token de acceso. Revisalo en Opciones → Servidor."). Other errors keep showing the backend's text.

### D6. App: the token field
- **Where:** under the address in the Server card. It is an `OutlinedTextField` with `PasswordVisualTransformation`, and a trailing eye toggle (`visibility` / `visibility_off` icons) to show it.
- **Save:** saves both the address and the token, normalized.
- **Restore default:** resets only the address.
- **Test connection:** uses the typed address and token, and reports:
  - `Connected`;
  - `Unauthorized` (new: "El servidor rechazó el token");
  - `Timeout`;
  - `Failed(detail)`.
- **Hint:** "Usá el mismo valor que API_TOKEN en el .env del backend."

## Risks / Trade-offs

- **An existing install breaks until the token is set.** → It's expected and documented. The 401 message tells the user where to fix it.
- **A static token leaks if the phone or the `.env` leaks.** → It's rotated by changing both. That's acceptable for a home backend. HTTPS is out of scope, and on the LAN the token travels in clear text like the data itself (see #61).
- **Breaking changes in the `drizzle-orm` or `sharp` majors.** → The 246 backend tests and the emulator flow cover the usage.

## Migration Plan

1. The owner adds `API_TOKEN` to `backend/.env`, for example with `openssl rand -hex 24`.
2. The owner restarts the backend.
3. In the app, Options → Server → the token, then Save.

Rollback: revert the commit.
