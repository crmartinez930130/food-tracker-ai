## Context

- **The prompt:** `analyzeMealPhoto` sends the image plus a fixed English instruction, and parses the result with `zodOutputFormat(MealAnalysisSchema)`. The schema has an item `name` and optional `notes`.
- **The route:** it uses multer `upload.single("photo")`, so other multipart text fields arrive in `req.body`.
- **The app's language:** after #34, `LocalAppLocale.current` inside the app's `AppEnvironment` returns the resolved tag, `es` or `en`.

## Goals / Non-Goals

**Goals:**
- Analysis texts match the app's language, with no new failure modes.

**Non-Goals:**
- Translating stored meals.
- Translating the CSV.
- Supporting languages beyond es and en.
- Translating confidence values. They are enums, and the app already localizes their labels.

## Decisions

### D1. A multipart field, defaulting to English
- **Validation:** `language` is checked with a small allow-list, `es` or `en`, case-insensitive.
- **Fallback:** anything else, including a missing value, becomes `en`.
- **No 400:** the client only ever sends es or en, and an older client (or `curl`) without the field must keep working.
- **Alternative:** a query parameter. Rejected: the request is already a multipart form, and a form field keeps everything about the photo together.

### D2. A language line appended to the prompt
- **The line:** `ANALYSIS_PROMPT` gains one final sentence, for example "Write each item's name and the notes in Spanish (es). Use common names a person in a Spanish-speaking country would recognize." For English: "Write each item's name and the notes in English."
- **What stays fixed:** the JSON schema, the numbers and the enum values stay language-neutral, since the structured output format enforces them.
- **Building the prompt:** `analysisPrompt(language)` is a pure, exported function, so its tests don't need the API.
- **Alternative:** translating the result afterwards. Rejected: it doubles the cost and latency, and names produced natively read better.

### D3. The app sends the displayed language
- `CaptureScreen` reads `LocalAppLocale.current` in composition and passes it to `analyzeMealPhoto(bytes, language)`.
- The client appends `language` as a form field next to `photo`.

### D4. Short, uniform macro labels in Review
- The resources `review_protein_g`, `review_fat_g` and `review_carbs_g` become "Proteína (g)/Grasa (g)/Carbs (g)" and "Protein (g)/Fat (g)/Carbs (g)".
- "Carbs" is widely used in Spanish nutrition apps and fits a third of the row.
- The layout itself doesn't change.

## Risks / Trade-offs

- **The model occasionally mixes languages, for example a brand name.** → This is acceptable, since names are editable in Review before saving.
- **Long Spanish names in the item field.** → The field already wraps and is editable.

## Migration Plan

None. The field is optional and its default keeps today's behavior.
