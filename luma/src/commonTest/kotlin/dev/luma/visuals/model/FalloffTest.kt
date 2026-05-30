package dev.luma.visuals.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FalloffTest {
    @Test
    fun linearFalloffMatchesExpectedValues() {
        assertEquals(1f, Falloff.Linear.transform(0f), 0.0001f)
        assertEquals(0.5f, Falloff.Linear.transform(0.5f), 0.0001f)
        assertEquals(0f, Falloff.Linear.transform(1f), 0.0001f)
    }

    @Test
    fun smoothFalloffStaysWithinUnitRange() {
        val values = listOf(0f, 0.2f, 0.5f, 0.8f, 1f).map(Falloff.Smooth::transform)
        assertTrue(values.all { it in 0f..1f })
        assertTrue(values.zipWithNext().all { (left, right) -> left >= right })
    }

    @Test
    fun sharpFalloffDecaysMoreAggressivelyThanEaseOut() {
        val normalizedDistance = 0.6f
        assertTrue(Falloff.Sharp.transform(normalizedDistance) < Falloff.EaseOut.transform(normalizedDistance))
    }
}
