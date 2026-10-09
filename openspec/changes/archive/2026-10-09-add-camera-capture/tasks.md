# Tasks

## 1. Backend: upright photos

- [x] 1.1 Apply the EXIF orientation in `backend/src/services/image-preprocessing.ts` (`sharp(input).rotate()` before resizing); verify in `backend/test/services/image-preprocessing.test.ts` that a JPEG with orientation 6 comes out with width/height swapped and that an untagged image keeps its dimensions, and that `npm test` and `npm run typecheck` pass

## 2. CameraX setup

- [x] 2.1 Add a `camerax = "1.6.2"` version and `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-compose` libraries to `frontend/gradle/libs.versions.toml`, and add them to `androidMain.dependencies`; verify `:composeApp:compileDebugKotlinAndroid` resolves and compiles

## 3. Camera composable

- [x] 3.1 Add `expect @Composable fun CameraCapture(onCaptured, onCancel, onUseGallery, modifier)` in `commonMain/.../ui/CameraCapture.kt` and an iOS `actual` stub (message + back button); verify the common and iOS metadata compile (`:composeApp:compileKotlinMetadata` or the Android build if iOS compilation isn't available on Linux)
- [x] 3.2 Android `actual` (`androidMain/.../ui/CameraCapture.android.kt`): permission check/request with denied and permanently-denied messages (gallery + cancel actions), `CameraXViewfinder` bound via `ProcessCameraProvider.awaitInstance` in a `LaunchedEffect` with `unbindAll` on dispose, shutter button capturing to a cache file (~2 MP `ResolutionSelector`) read to bytes and deleted, system back = cancel; verify `:composeApp:assembleDebug` builds

## 4. Capture screen

- [x] 4.1 Rework `CaptureScreen`: idle state with "Take photo" (primary) and "Choose from gallery"; in-place camera mode; confirm state with the decoded thumbnail and "Retake"/"Choose again" + "Analyze"; analysis path unchanged; verify it compiles and `:composeApp:testDebugUnitTest` passes

- [x] 4.2 Decode the confirm thumbnail upright: `expect fun ByteArray.decodePreviewBitmap(maxDimension)` with an Android `actual` that applies the EXIF orientation (`androidx.exifinterface`, declared explicitly) and downsamples, plus an iOS `actual` with plain decoding; verify `exifRotationDegrees`/`sampleSizeFor` in `androidUnitTest/PhotoPreviewTest` and that a camera photo's thumbnail shows upright on the emulator

## 5. End-to-end verification

- [x] 5.1 On the emulator with the permission revoked (`adb shell pm revoke com.foodtracker.app android.permission.CAMERA`): "Take photo" prompts; deny → explanation with gallery/cancel shown, no crash; "Choose from gallery" from there works; grant on retry → viewfinder appears
- [x] 5.2 On the emulator, camera path: capture a photo (emulator started with `-camera-back imagefile:<meal photo>`, see design.md Risks), confirm thumbnail shows upright, "Retake" reopens the camera, "Analyze" → Review with a real analysis; cancel from the viewfinder returns without analyzing
- [x] 5.3 On the emulator, gallery regression: choose from gallery → thumbnail → Analyze → Review; save nothing (or delete any saved meal afterwards)
