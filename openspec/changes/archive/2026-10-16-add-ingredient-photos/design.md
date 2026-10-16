# Design

## Context

- **Backend:**
  - `POST /api/meals/analyze` uses multer (memory storage, 15 MB per file) with `upload.single("photo")`. The rate limiter runs before multer.
  - The route checks the photo's content type (`detectImageType`) and preprocesses it (`preprocessImage`: EXIF rotation, long edge 1568 px, JPEG 80).
  - `analyzeMeal` sends one image block followed by the text prompt. `analysisPrompt` picks `PHOTO_INSTRUCTION` or `TEXT_INSTRUCTION` by `hasImage`.
- **App, Capture:**
  - one `photo: ByteArray?`, from the in-app camera (`CameraCapture`) or the single-image picker (`rememberImagePickerLauncher`, `PickVisualMedia`);
  - `canAnalyze(hasPhoto, description)`;
  - back first discards the photo (`captureBackAction`).
- **App, sending:** `FoodTrackerApiClient.analyzeMeal(photo, description, portion, language)` sends the photo as is, often several MB from the camera.
- **App, without a connection (#71):** `CaptureQueueRepository` keeps one `photoFile` per `capture_queue` row (Room version 4) in `CapturePhotoStore`.
- The app already depends on `androidx.exifinterface` (used for the photo preview).

## Goals / Non-Goals

**Goals:**
- More accurate estimates when the plate hides ingredients, using photos of them, in one analysis call.
- No surprise in size: requests and kept captures stay small.

**Non-Goals:**
- Storing the photos in the backend, or per-ingredient review.
- Changing the rate limit or the model.

## Decisions

1. **API shape.** The route uses `upload.fields([{ name: "photo", maxCount: 1 }, { name: "ingredients", maxCount: 5 }])`. Multer's own `LIMIT_UNEXPECTED_FILE` (a sixth ingredient) already becomes a 400 through the existing upload error handling, with an explicit message. The route then checks, in this order:
   - the total size of all files is at most 40 MB, a constant (`MAX_TOTAL_UPLOAD_BYTES`);
   - every file passes `detectImageType`;
   - there is something to analyze (a photo, an ingredient or a description).

   Every image goes through `preprocessImage`. The model is called only after all checks pass.

2. **Content sent to Claude.** `analyzeMeal` takes `image?` and `ingredients: PreprocessedImage[]` and builds the content as label/image pairs: a text block "Photo of the meal:" before the meal image, and "Photo of an ingredient used to prepare it (n of m):" before each ingredient. The prompt goes last.
   - **Prompt with ingredients:** a new `INGREDIENTS_INSTRUCTION` joins the photo or text instruction. It says to estimate what was eaten in the meal, to use the ingredient photos for what the meal photo can't show (quantities, nutrition labels, brands, cooking fat), to read the label's values per stated amount when visible, and never to list an ingredient twice when it is already visible on the plate.
   - **Without a meal photo:** the base becomes the text instruction, and the ingredient photos are what is known about the meal.
   - **Logging:** records the number of ingredient photos, never their content.

3. **Downscale on the phone.** A new `expect fun ByteArray.downscaledJpeg(maxLongEdge: Int = 1568): ByteArray` is applied to every photo added to Capture, the meal photo included:
   - **Android:** decode with `inSampleSize` to avoid a large bitmap, apply the EXIF rotation (the re-encoded file has no EXIF), scale to the long edge and compress to JPEG 85;
   - **iOS:** returns the bytes unchanged (unverified).

   The backend still preprocesses, so its output doesn't change. A 4-8 MB camera photo becomes a few hundred KB, which matters for six photos over mobile data and in the offline storage.

4. **Capture UI.**
   - **State:** a list `ingredients: List<ByteArray>`, at most `MAX_INGREDIENT_PHOTOS = 5`.
   - **Layout:** an "Ingredients (optional)" section below the meal photo, with a horizontal row of 72 dp thumbnails, each with a close button, then "Camera" and "Gallery" buttons while there is room.
   - **Camera:** reuses `CameraCapture` with a target (meal or ingredient).
   - **Gallery:** `rememberMultiImagePickerLauncher(maxItems)` (Android `PickMultipleVisualMedia`) when two or more slots remain, and the single picker when one remains, since the contract requires `maxItems > 1`.
   - **Rules:** `canAnalyze(hasPhoto, hasIngredients, description)`. Back first discards the meal photo as today, then leaves. Ingredient photos are removed one by one with their own button.
   - **Favorites:** stay hidden once anything was added.

5. **Without a connection.** `capture_queue` gains a nullable `ingredient_files` column with the photos' file names as a JSON list; Room version 5 auto-migrates it from 4. `analyzeOrKeep` writes them as `<clientId>-ingredient-<n>.jpg`; `analyzeWaiting` reads them all and fails the row with `MISSING_PHOTO_ERROR` if any is gone; `discard` and `complete` delete them all.

## Risks / Trade-offs

- **Cost:** each ingredient photo adds about 1,500 input tokens to the call (five add about 7,500). → Capped at 5; stated in the proposal.
- **Memory on the backend:** multer keeps up to 6 files in memory before the total check (worst case about 90 MB) for one request. → Single user, and the analysis rate limit still applies. Multer can't enforce a total by itself.
- **Re-encoding the meal photo on the phone** changes the bytes sent for every analysis. → Same long edge the backend keeps; checked on the emulator with a real analysis.
- **iOS** keeps sending full-size photos until its downscaler is written.
