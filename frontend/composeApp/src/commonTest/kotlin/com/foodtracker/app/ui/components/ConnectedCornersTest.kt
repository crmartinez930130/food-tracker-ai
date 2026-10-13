package com.foodtracker.app.ui.components

import com.foodtracker.app.ui.components.CornerKind.FULL
import com.foodtracker.app.ui.components.CornerKind.INNER
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectedCornersTest {

    @Test
    fun `outer edges are round, inner edges small`() {
        assertEquals(ConnectedCorners(FULL, INNER, INNER, FULL), connectedCorners(0, 3, selected = false))
        assertEquals(ConnectedCorners(INNER, INNER, INNER, INNER), connectedCorners(1, 3, selected = false))
        assertEquals(ConnectedCorners(INNER, FULL, FULL, INNER), connectedCorners(2, 3, selected = false))
    }

    @Test
    fun `selected and single buttons are full pills`() {
        assertEquals(ConnectedCorners(FULL, FULL, FULL, FULL), connectedCorners(1, 3, selected = true))
        assertEquals(ConnectedCorners(FULL, FULL, FULL, FULL), connectedCorners(0, 1, selected = false))
    }
}
