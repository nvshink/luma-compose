package dev.luma.visuals.pattern.layout

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.luma.visuals.model.OffsetFraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PatternLayoutResolverTest {
    private val scope = PatternLayoutScope(
        containerSize = Size(320f, 200f),
        elementSize = Size(12f, 12f),
        density = Density(1f),
    )

    @Test
    fun randomLayoutIsDeterministicForSameSeed() {
        val first = PatternLayoutResolver.resolve(PatternLayout.Random(seed = 42L, density = 1.1f), scope)
        val second = PatternLayoutResolver.resolve(PatternLayout.Random(seed = 42L, density = 1.1f), scope)

        assertEquals(first, second)
    }

    @Test
    fun randomLayoutChangesWhenSeedChanges() {
        val first = PatternLayoutResolver.resolve(PatternLayout.Random(seed = 1L, density = 1.1f), scope)
        val second = PatternLayoutResolver.resolve(PatternLayout.Random(seed = 2L, density = 1.1f), scope)

        assertNotEquals(first, second)
    }

    @Test
    fun unevenLayoutIsDeterministicForSameSeed() {
        val first = PatternLayoutResolver.resolve(PatternLayout.Uneven(seed = 42L, density = 1.4f), scope)
        val second = PatternLayoutResolver.resolve(PatternLayout.Uneven(seed = 42L, density = 1.4f), scope)

        assertEquals(first, second)
    }

    @Test
    fun unevenLayoutKeepsPointsInsideBounds() {
        val positions = PatternLayoutResolver.resolve(
            PatternLayout.Uneven(seed = 7L, density = 1.4f, clusterCount = 4),
            scope,
        )

        assertTrue(positions.isNotEmpty())
        assertTrue(positions.all { it.x in 0f..scope.containerSize.width && it.y in 0f..scope.containerSize.height })
    }

    @Test
    fun gridLayoutKeepsPointsInsideBounds() {
        val positions = PatternLayoutResolver.resolve(
            PatternLayout.Grid(spacing = 32.dp, rowOffset = 16.dp, density = 1f),
            scope,
        )

        assertTrue(positions.isNotEmpty())
        assertTrue(positions.all { it.x in 0f..scope.containerSize.width && it.y in 0f..scope.containerSize.height })
    }

    @Test
    fun radialLayoutStartsFromConfiguredCenter() {
        val center = OffsetFraction(0.25f, 0.75f)
        val positions = PatternLayoutResolver.resolve(
            PatternLayout.Radial(center = center, ringSpacing = 28.dp, elementsPerRing = 6),
            scope,
        )

        assertEquals(center.toOffset(scope.containerSize), positions.first())
    }
}
