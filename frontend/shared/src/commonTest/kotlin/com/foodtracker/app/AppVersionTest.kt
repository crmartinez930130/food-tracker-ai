package com.foodtracker.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun `version code is major times 10000 plus minor times 100 plus patch`() {
        assertEquals(200, versionCodeOf("0.2.0"))
        assertEquals(10305, versionCodeOf("1.3.5"))
        assertEquals(9999, versionCodeOf("0.99.99"))
    }

    @Test
    fun `invalid versions have no code`() {
        listOf("0.3", "1.2.100", "1.100.0", "v1.2.3", "1.2.3-beta", "").forEach {
            assertNull(versionCodeOf(it), it)
        }
    }

    @Test
    fun `a later version always has a higher code`() {
        val ordered = listOf("0.2.0", "0.2.1", "0.2.99", "0.3.0", "0.99.99", "1.0.0", "1.0.1")
        val codes = ordered.map { versionCodeOf(it)!! }
        assertEquals(codes.sorted(), codes)
        assertEquals(codes.distinct(), codes)
    }

    @Test
    fun `the generated constants follow version properties and the formula`() {
        assertEquals(versionCodeOf(AppVersion.NAME), AppVersion.CODE)
        assertTrue(AppVersion.COMMIT.isNotBlank())
    }
}
