package com.foodtracker.app.ui

/** What system back does on the capture screen (outside the camera, which handles its own back). */
enum class CaptureBack { DISCARD_PHOTO, EXIT }

fun captureBackAction(hasPhotoToConfirm: Boolean): CaptureBack =
    if (hasPhotoToConfirm) CaptureBack.DISCARD_PHOTO else CaptureBack.EXIT

/** What system back does on the review screen, which never leaves without confirmation. */
enum class ReviewBack { IGNORE, DISMISS_PROMPT, SHOW_PROMPT }

fun reviewBackAction(isSaving: Boolean, isDiscardPromptShown: Boolean): ReviewBack = when {
    // Handled-but-ignored rather than disabled: a disabled handler would let back reach the
    // activity and close the app mid-save.
    isSaving -> ReviewBack.IGNORE
    isDiscardPromptShown -> ReviewBack.DISMISS_PROMPT
    else -> ReviewBack.SHOW_PROMPT
}
