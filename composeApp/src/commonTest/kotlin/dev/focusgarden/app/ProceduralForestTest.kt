package dev.focusgarden.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProceduralForestTest {
    @Test
    fun sameSeedAlwaysCreatesSameValue() {
        assertEquals(pseudo(42), pseudo(42))
    }

    @Test
    fun generatedValuesStayNormalized() {
        repeat(1_000) { assertTrue(pseudo(it) in 0f..1f) }
    }
}
