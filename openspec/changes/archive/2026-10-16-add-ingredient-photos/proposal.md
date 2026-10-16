## Why

An analysis sees one photo of the plate and an optional description. What usually moves the calories most is what the plate photo can't show: oil, sauces, cheese, the brand of a product and its nutrition label. Letting the user add photos of the ingredients used (packages, labels, raw amounts) gives the model that information.

Tracked as GitHub issue #79: https://github.com/crmartinez930130/food-tracker-ai/issues/79

## What Changes

- **Backend:** `POST /api/meals/analyze` accepts, besides `photo`, up to 5 files named `ingredients`. Each is checked like the photo (JPEG or PNG by content, at most 15 MB) and preprocessed the same way, and all of them together are limited to 40 MB. More than 5, a file that isn't an image, or too much in total gets 400. An analysis needs a plate photo, an ingredient photo or a description.
- **Prompt:** each image goes to Claude after a short label: "Photo of the meal:" or "Photo of an ingredient used to prepare it (N of M):". The instruction tells the model to estimate what was eaten, to use the ingredient photos for what the plate doesn't show (amounts, nutrition labels, brands), and not to count an ingredient twice when it's already visible on the plate.
- **App:**
  - Capture gets an optional "Ingredients" section: thumbnails with a remove action, and add-from-camera / add-from-gallery (several at once) while there are fewer than 5.
  - Analyze is available with a plate photo, an ingredient photo or a description.
  - Photos are downscaled on the phone before sending (long edge 1568 px, JPEG), the size the backend keeps anyway, so 6 photos stay a small upload.
- **Without a connection (#71):** a kept capture also keeps its ingredient photos, sends them when analyzed, and deletes them when saved or discarded (database version 5).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `meal-photo-analysis`: the analysis takes optional ingredient photos.
- `meal-capture`: Capture lets the user add ingredient photos, and they count as something to analyze.
- `offline-queue`: kept captures keep their ingredient photos.

## Impact

- **Backend:** `src/routes/meals.ts` (multer fields, limits, checks), `src/services/vision-analysis.ts` (several images, labels, prompt), tests.
- **App:**
  - `FoodTrackerApiClient.analyzeMeal` takes the ingredient photos;
  - `CaptureScreen` gets the ingredients section;
  - the image picker gains a multi-select variant;
  - a photo downscaler (`expect`/`actual`);
  - `CaptureQueueRepository` and `capture_queue` store several files (Room version 5);
  - strings in Spanish and English.
- **Cost:** each ingredient photo adds roughly 1,500 input tokens to the analysis call. One analysis is still one call, so the rate limit is unchanged.
- **Deployments:** the backend change needs the laptop backend redeployed after merge.
