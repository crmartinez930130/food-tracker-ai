# Proposal

## Why

This is a greenfield personal project: there is no backend, no frontend, and no database yet. Before any feature work can start, the project needs its foundational scaffolding — a Node/TypeScript backend that can talk to the Claude vision API and persist data locally, and a Kotlin Multiplatform + Compose Multiplatform app that can capture a meal photo and show the result. Establishing this now, with the core end-to-end flow (photo → AI analysis → confirm → save → summary) working for a trivial case, de-risks every subsequent feature change.

## What Changes

- Scaffold the monorepo: `backend/` (Node.js + TypeScript) and `frontend/` (Kotlin Multiplatform + Compose Multiplatform, Android-first).
- Backend: Express (or equivalent minimal HTTP framework) REST API, SQLite database via Drizzle ORM (`better-sqlite3` driver) with versioned migrations (`drizzle-kit`).
- Backend: integration with the Anthropic Claude API (vision-capable model) to analyze a meal photo and return structured nutrition data (foods, estimated grams, calories, protein, fat, carbs) via `messages.parse()` with a Zod-defined output schema — not free text.
- Backend: image preprocessing (resize/compress with `sharp`) before sending to Claude, to control token cost.
- Backend: REST endpoints to analyze a photo (without saving), confirm and save a meal entry, list/edit/delete meal entries, and compute daily/weekly nutrition summaries.
- Frontend: Compose Multiplatform screens for capturing/selecting a photo, reviewing and correcting the AI analysis before confirming, viewing the daily history, and viewing a daily/weekly summary.
- API key handling: `ANTHROPIC_API_KEY` lives only in the backend's local environment (`.env`, gitignored); the app never talks to Claude directly.
- No multi-user, no auth, no cloud deployment, no barcode scanning, no push notifications, no wearables — explicitly out of scope for this change.

## Capabilities

### New Capabilities
- `meal-photo-analysis`: sending a meal photo to Claude's vision API and receiving a structured (foods, quantities, calories, macros) analysis, without persisting it.
- `meal-tracking`: confirming an analysis (with user corrections) into a saved meal entry, and listing/viewing/editing/deleting saved entries.
- `nutrition-summary`: computing and returning daily and weekly totals (calories and macros) from saved meal entries.

### Modified Capabilities
(none — greenfield project, nothing exists yet)

## Impact

- **New code**: entire `backend/` and `frontend/` trees, plus root-level monorepo config (`.gitignore`, README).
- **New dependencies (backend)**: `@anthropic-ai/sdk`, `zod`, `express` (or equivalent), `drizzle-orm`, `better-sqlite3`, `drizzle-kit`, `sharp`, `multer` (or equivalent for multipart upload), `dotenv`.
- **New dependencies (frontend)**: Kotlin Multiplatform + Compose Multiplatform Gradle setup, Ktor client (or equivalent) for REST calls, a camera/image-picker library for Android (and iOS if time permits).
- **External systems**: Anthropic Claude API (billed per use on the user's own key). No other external services.
- **Data**: new local SQLite database file (`backend/data/food-tracker.sqlite`), not committed to git.
- **Deployment**: none — runs on the user's own machine/home network only.
