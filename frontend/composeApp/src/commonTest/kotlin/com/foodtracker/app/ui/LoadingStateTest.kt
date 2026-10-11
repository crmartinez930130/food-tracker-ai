package com.foodtracker.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class LoadingStateTest {

    @Test
    fun `loading with nothing to show yet is the initial spinner`() {
        assertEquals(LoadingPresentation.INITIAL, loadingPresentation(isLoading = true, hasContent = false))
    }

    @Test
    fun `reloading over shown data marks it stale`() {
        assertEquals(LoadingPresentation.STALE, loadingPresentation(isLoading = true, hasContent = true))
    }

    @Test
    fun `not loading is ready, with or without content`() {
        assertEquals(LoadingPresentation.READY, loadingPresentation(isLoading = false, hasContent = true))
        assertEquals(LoadingPresentation.READY, loadingPresentation(isLoading = false, hasContent = false))
    }
}
