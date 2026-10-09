# Design

## Context

- **`CaptureScreen`** (commonMain) has a single "Choose a photo" button. It is backed by the `expect`/`actual` `rememberImagePickerLauncher`, which uses `PickVisualMedia` on Android and a stub on iOS. It keeps the picked bytes in local state and shows "Photo selected (N KB)" plus "Analyze".
- **Navigation** is the hand-rolled `AppState.screen`. Leaving a screen disposes its local state.
- **Manifest:** `android.permission.CAMERA` is already declared. The AVD's back camera is `virtualscene`.
- **Backend:** `preprocessImage` (sharp) resizes and re-encodes to JPEG, but never applies the EXIF orientation.

See proposal.md for motivation. The user chose an in-app CameraX camera over the system camera intent.

## Goals / Non-Goals

**Goals:**
- An in-app camera that is fully Compose-native on Android, with all platform-specific code (CameraX and the permission) behind one `expect`/`actual` composable.
- A single confirm-then-analyze path for both photo sources.
- Upright photos for analysis, whatever their source.

**Non-Goals:**
- Flash, zoom, front-camera switching, tap-to-focus or barcode scanning. They are possible later on the same CameraX base.
- A real iOS camera. iOS keeps a stub, as the issue states.
- Changing the Review flow or the analysis API.

## Decisions

**1. CameraX 1.6.2 with `camera-compose`'s `CameraXViewfinder`, not `PreviewView` inside an `AndroidView`.**
- The `Preview` use case publishes its `SurfaceRequest` into a `MutableStateFlow`, and `CameraXViewfinder(surfaceRequest)` renders it.
- Binding happens in a `LaunchedEffect`: `ProcessCameraProvider.awaitInstance(context)`, then `bindToLifecycle(lifecycleOwner, DEFAULT_BACK_CAMERA, preview, imageCapture)`, then `awaitCancellation()`. In `finally` it calls `unbindAll()`, so the camera is released when the composable leaves.
- The four artifacts (`camera-core`, `camera-camera2`, `camera-lifecycle` and `camera-compose`) go in the version catalog under one `camerax` version, as `androidMain` dependencies.
- *Alternative:* `PreviewView` through `AndroidView`, or `LifecycleCameraController`. These mix the View system into a Compose screen, and `CameraXViewfinder` is now the recommended Compose path.

**2. The camera is an in-place mode of `CaptureScreen`, through `expect @Composable fun CameraCapture(onCaptured: (ByteArray) -> Unit, onCancel: () -> Unit, onUseGallery: () -> Unit, modifier)`.**
- `CaptureScreen` keeps `showCamera` and `photo` in local state. While `showCamera` is true it renders `CameraCapture` instead of its normal content, so the captured bytes land straight in the same state the gallery picker writes to. No new `AppState` route or back plumbing is needed.
- The Android `actual` owns the whole permission flow and the CameraX session, and handles system back as cancel.
- The iOS `actual` shows "Camera isn't available on this platform yet" with a button to go back.
- *Alternative:* a separate `Screen.Camera` route. It would dispose `CaptureScreen` and force passing the bytes through `AppState`, with no benefit.

**3. Permission flow inside the Android `actual`.**
- On entry it checks `ContextCompat.checkSelfPermission(CAMERA)`. If the permission isn't granted, it launches `RequestPermission` once.
- **Granted:** it shows the viewfinder.
- **Denied:** it shows an explanation ("The camera is needed to take a photo of your meal") with three actions: "Allow camera", which asks again; "Choose from gallery", which calls `onUseGallery`; and "Cancel".
- **Permanently denied:** this is a second denial, where the system no longer prompts and `shouldShowRequestPermissionRationale` is false. The text adds that it can be enabled in the system settings. It doesn't deep-link there, to stay in scope. Nothing can crash: `bindToLifecycle` only runs once the permission is granted.

**4. Capture to a temporary file, not to an in-memory `ImageProxy`.**
- `ImageCapture.takePicture(OutputFileOptions(cacheFile), executor, callback)` is wrapped in `suspendCancellableCoroutine`. The file is read into a `ByteArray` on `Dispatchers.IO` and then deleted.
- CameraX writes a correct EXIF orientation into saved files. With in-memory `ImageProxy` I would have to apply `rotationDegrees` myself.
- `ImageCapture` uses `CAPTURE_MODE_MINIMIZE_LATENCY` and a `ResolutionSelector` targeting about 1920×1440. The backend downsizes to 1568 px anyway, so full-sensor photos would only make uploads slower.

**5. Backend auto-orient with `sharp().rotate()`.**
- `rotate()` with no arguments applies the EXIF orientation and strips the tag before resizing.
- It fixes camera photos and also phone gallery photos, which carry the same tags.
- It is tested with a synthetic JPEG whose orientation is set to 6 (rotated 90°): the output dimensions must be swapped. A photo without the tag must come out unchanged.

**6. Confirm step with a thumbnail.**
- After either source, `CaptureScreen` shows the photo, decoded off the main thread, plus the actions:
  - "Retake", which reopens the camera, or "Choose again", which reopens the picker, depending on the source.
  - "Analyze".
- *Correction found during implementation:* `decodeToImageBitmap()` ignores the EXIF orientation, so camera photos showed sideways in the confirm thumbnail, even though the backend analyzes them upright. The thumbnail now uses an `expect fun ByteArray.decodePreviewBitmap(maxDimension)`.
  - The Android `actual` reads the orientation with `androidx.exifinterface`, which was already a transitive dependency of `camera-core` and is now declared explicitly, then rotates the bitmap. It also downsamples with `inSampleSize` to about 1080 px, which settles the memory risk below.
  - The iOS `actual` keeps plain decoding, because photo input is still stubbed there.
  - The pure helpers (`exifRotationDegrees` and `sampleSizeFor`) are unit-tested in `androidUnitTest`.
- The idle state shows two buttons: "Take photo" (primary, the main flow per the issue) and "Choose from gallery" (secondary).

## Risks / Trade-offs

- **[The emulator's virtual-scene camera shows a 3D room, not food]** → The poster approach was tried first, but the poster isn't in the default camera view and the scene pose can't be driven from the console. Instead, the verification starts the emulator with `-camera-back imagefile:<meal photo>`, so the back camera renders the meal photo directly.
- **[CameraX adds roughly 1 to 2 MB to the APK]** → An accepted cost for the primary capture flow.
- **[Decoding a full-size photo for the thumbnail is memory-heavy]** → Resolved by the downsampling in `decodePreviewBitmap` (Decision 6), on top of bounding capture to about 2 MP (Decision 4).
