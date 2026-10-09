## Why

Logging a meal today means leaving the app, taking a photo with the camera app, coming back and picking it from the gallery. In Cal AI and Foodvisor the main flow is shooting the photo right there, in the app, and the gallery is the secondary path. While checking the camera path I also found that the backend ignores a photo's EXIF orientation, so phone photos (camera or gallery) can reach Claude sideways. Tracked as GitHub issue #5: https://github.com/crmartinez930130/food-tracker-ai/issues/5

## What Changes

- **In-app camera on the capture screen.** "Take photo" opens a CameraX viewfinder inside the app with a shutter button. "Choose from gallery" keeps the current photo picker.
- **Camera permission requested at runtime** the first time the camera is opened. If it is denied, the screen explains why it's needed and offers the gallery instead. It never crashes or fails silently.
- **Confirm before analyzing.** After capturing or picking, the capture screen shows a preview of the photo with "Retake" or "Choose again", plus "Analyze". The photo then follows the existing path: bytes → `POST /api/meals/analyze` → Review.
- **Backend: photos are oriented upright before analysis.** Preprocessing applies the EXIF orientation, which fixes rotated camera photos and phone gallery photos alike.
- iOS keeps a stub. The camera button there shows that the feature isn't available yet.

## Capabilities

### New Capabilities
- `meal-capture`: how the app obtains the meal photo: camera or gallery, camera permission handling, and confirming the photo before it is analyzed.

### Modified Capabilities
- `meal-photo-analysis`: adds a requirement that photos are analyzed in their upright orientation according to their EXIF metadata.

## Impact

- **Frontend (Android):**
  - New dependencies in `androidMain`: CameraX 1.6.2 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-compose`), plus an explicit declaration of `androidx.exifinterface`, which CameraX already pulls in transitively. It is used to show the photo thumbnail upright.
  - New `expect`/`actual` `CameraCapture` composable, with a stub on iOS.
  - `CaptureScreen` is reworked with the two buttons and a photo preview.
- **Backend:** `services/image-preprocessing.ts` applies the EXIF orientation, with a new test.
- **Manifest:** none. `android.permission.CAMERA` is already declared.
- **API:** unchanged.
