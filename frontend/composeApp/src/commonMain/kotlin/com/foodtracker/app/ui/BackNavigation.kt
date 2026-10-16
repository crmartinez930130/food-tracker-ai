package com.foodtracker.app.ui

/** What system back does on the capture screen (outside the camera, which handles its own back). */
enum class CaptureBack { IGNORE, DISCARD_PHOTO, EXIT }

fun captureBackAction(hasPhotoToConfirm: Boolean, isAnalyzing: Boolean): CaptureBack = when {
    // The in-flight analysis would still navigate to Review after a "discard", so back waits for it.
    isAnalyzing -> CaptureBack.IGNORE
    hasPhotoToConfirm -> CaptureBack.DISCARD_PHOTO
    else -> CaptureBack.EXIT
}

/** Longest meal description the backend accepts. */
const val MAX_DESCRIPTION_LENGTH = 500

/** Whether there is something to analyze: a photo, a non-blank description, or both. */
fun canAnalyze(hasPhoto: Boolean, description: String, hasIngredients: Boolean = false): Boolean =
    hasPhoto || hasIngredients || description.isNotBlank()

/** Most ingredient photos one analysis takes (the backend's limit); each adds to the analysis cost. */
const val MAX_INGREDIENT_PHOTOS = 5

/** Appends dictated text to what the user already typed, separated by a space, within the limit. */
fun appendDictation(current: String, dictated: String): String {
    val addition = dictated.trim()
    if (addition.isEmpty()) return current
    val joined = if (current.isBlank()) addition else "${current.trimEnd()} $addition"
    return joined.take(MAX_DESCRIPTION_LENGTH)
}

/** What system back does on the review screen, which never leaves without confirmation. */
enum class ReviewBack { IGNORE, DISMISS_PROMPT, SHOW_PROMPT }

fun reviewBackAction(isSaving: Boolean, isDiscardPromptShown: Boolean): ReviewBack = when {
    // Handled-but-ignored rather than disabled: a disabled handler would let back reach the
    // activity and close the app mid-save.
    isSaving -> ReviewBack.IGNORE
    isDiscardPromptShown -> ReviewBack.DISMISS_PROMPT
    else -> ReviewBack.SHOW_PROMPT
}
