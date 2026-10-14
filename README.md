# food-tracker-ai

Personal meal-tracking app: take a photo of a meal, get an AI-estimated breakdown of foods, calories, and macros (protein/fat/carbs), review and correct it, and track daily/weekly totals.

Single-user, runs entirely on your own machine/home network. No cloud deployment, no accounts.

## Architecture

```
KMP + Compose app (Android/iOS)  <-- REST/JSON -->  backend/ (Node.js + TypeScript)  <-->  Claude API (vision)
                                                              |
                                                        SQLite (local file)
```

The app never talks to Claude directly — all vision analysis is proxied through the backend, which is the only place holding your Anthropic API key.

## Backend

### Setup

```bash
cd backend
npm install
cp .env.example .env
```

Edit `backend/.env` and set your Anthropic key and an access token of your own:

```
ANTHROPIC_API_KEY=sk-ant-...
API_TOKEN=<a random value of at least 16 characters, e.g. the output of `openssl rand -hex 24`>
```

The server refuses to start without `API_TOKEN`. Every `/api` request must send it as `Authorization: Bearer <token>`; in the app, enter the same value in Options → Server → Access token.

Optional settings in `.env` (defaults shown): `ANTHROPIC_MODEL=claude-opus-5`, `HOST=0.0.0.0`, `PORT=3000`, `DB_PATH=data/food-tracker.sqlite`, `ANALYZE_RATE_LIMIT_PER_MINUTE=20` (meal analyses accepted per minute; each one is a paid API call).

### Run

```bash
npm run dev      # watches and restarts on changes
# or
npm run build && npm start
```

Database migrations run automatically on startup. To apply migrations without starting the server: `npm run db:migrate`. After changing `src/db/schema.ts`, generate a new migration with `npm run db:generate` and commit the generated SQL file under `src/db/migrations/`.

### Test

```bash
npm test
```

### Reaching the backend from your phone

The server binds to `0.0.0.0` by default, so it's reachable from other devices on your home network — not just `localhost`. Find your machine's LAN IP (e.g. `hostname -I` on Linux, or check your network settings) and enter `http://<that-ip>:3000` in the app's Options → Server, together with your access token. `localhost` only works if the app runs on the same machine (e.g. an Android emulator with the right host mapping) — a phone on the same Wi-Fi needs the actual LAN IP.

### Backing up your data

There is no automatic backup. The entire history lives in `backend/data/food-tracker.sqlite`. To back it up, just copy that file somewhere safe while the server is stopped (or use SQLite's `.backup` command if you want to do it while running).

## Frontend

The Kotlin Multiplatform + Compose Multiplatform app (Android-first, iOS as a secondary target) has not been scaffolded yet — see `openspec/changes/setup-project-foundation/tasks.md` for its status. Once set up, it will live under `frontend/` and this section will cover how to build and run it.

## Workflow

Every issue is delivered as a Pull Request, checked by CI and reviewed by an automated Claude reviewer before a manual merge. See [CONTRIBUTING.md](CONTRIBUTING.md).
