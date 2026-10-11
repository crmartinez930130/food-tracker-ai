package com.foodtracker.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class BackNavigationTest {

    @Test
    fun `capture back discards a photo awaiting confirmation`() {
        assertEquals(CaptureBack.DISCARD_PHOTO, captureBackAction(hasPhotoToConfirm = true, isAnalyzing = false))
    }

    @Test
    fun `capture back exits when there is no photo`() {
        assertEquals(CaptureBack.EXIT, captureBackAction(hasPhotoToConfirm = false, isAnalyzing = false))
    }

    @Test
    fun `capture back is ignored while the photo is being analyzed`() {
        assertEquals(CaptureBack.IGNORE, captureBackAction(hasPhotoToConfirm = true, isAnalyzing = true))
    }

    @Test
    fun `review back shows the discard prompt`() {
        assertEquals(ReviewBack.SHOW_PROMPT, reviewBackAction(isSaving = false, isDiscardPromptShown = false))
    }

    @Test
    fun `review back while the prompt is shown dismisses it`() {
        assertEquals(ReviewBack.DISMISS_PROMPT, reviewBackAction(isSaving = false, isDiscardPromptShown = true))
    }

    @Test
    fun `review back is ignored while saving, even with the prompt shown`() {
        assertEquals(ReviewBack.IGNORE, reviewBackAction(isSaving = true, isDiscardPromptShown = false))
        assertEquals(ReviewBack.IGNORE, reviewBackAction(isSaving = true, isDiscardPromptShown = true))
    }
}
