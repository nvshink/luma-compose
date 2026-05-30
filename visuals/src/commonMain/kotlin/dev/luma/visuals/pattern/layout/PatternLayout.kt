package dev.luma.visuals.pattern.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.luma.visuals.model.OffsetFraction
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Strategy used to position elements inside [dev.luma.visuals.pattern.PatternBackground]. */
sealed interface PatternLayout {
    /**
     * Randomly distributes elements across the container.
     *
     * @property seed Seed used to keep placement deterministic.
     * @property density Density multiplier used to derive the element count.
     */
    @Immutable
    data class Random(
        val seed: Long,
        val density: Float = 1f,
    ) : PatternLayout

    /**
     * Distributes elements unevenly by concentrating them around seeded clusters.
     *
     * @property seed Seed used to keep placement deterministic.
     * @property density Density multiplier used to derive the element count.
     * @property clusterCount Number of hotspots used to bias placement.
     * @property clusterSpread Fraction of the container used as cluster radius.
     * @property backgroundBias Fraction of elements that stay globally scattered.
     */
    @Immutable
    data class Uneven(
        val seed: Long,
        val density: Float = 1f,
        val clusterCount: Int = 5,
        val clusterSpread: Float = 0.18f,
        val backgroundBias: Float = 0.2f,
    ) : PatternLayout

    /**
     * Arranges elements into a regular grid.
     *
     * @property spacing Base spacing between grid cells.
     * @property rowOffset Horizontal offset applied to even rows.
     * @property columnOffset Horizontal offset applied to odd rows.
     * @property density Density multiplier that tightens or loosens the grid.
     */
    @Immutable
    data class Grid(
        val spacing: Dp,
        val rowOffset: Dp = 0.dp,
        val columnOffset: Dp = 0.dp,
        val density: Float = 1f,
    ) : PatternLayout

    /**
     * Arranges elements on concentric rings around a center point.
     *
     * @property center Fractional center of the radial layout.
     * @property ringSpacing Distance between rings.
     * @property elementsPerRing Optional fixed element count per ring.
     * @property density Density multiplier used when deriving the automatic ring count.
     */
    @Immutable
    data class Radial(
        val center: OffsetFraction = OffsetFraction.Center,
        val ringSpacing: Dp,
        val elementsPerRing: Int? = null,
        val density: Float = 1f,
    ) : PatternLayout

    /**
     * Delegates position generation to caller-provided logic.
     *
     * @property provider Position generator that returns fractional element coordinates.
     */
    @Immutable
    data class Custom(
        val provider: PatternPositionProvider,
    ) : PatternLayout
}

/**
 * Input available to custom layout providers.
 *
 * @property containerSize Pixel size of the drawing container.
 * @property elementSize Pixel size of the active pattern element.
 * @property density Density used to convert dp-based values when needed.
 */
@Immutable
data class PatternLayoutScope(
    val containerSize: Size,
    val elementSize: Size,
    val density: Density,
)

/** Produces element positions in fractional coordinates. */
fun interface PatternPositionProvider {
    /** Returns element positions for the provided [scope]. */
    fun positions(scope: PatternLayoutScope): List<OffsetFraction>
}

internal object PatternLayoutResolver {
    fun resolve(layout: PatternLayout, scope: PatternLayoutScope): List<Offset> = when (layout) {
        is PatternLayout.Random -> randomPositions(layout, scope)
        is PatternLayout.Uneven -> unevenPositions(layout, scope)
        is PatternLayout.Grid -> gridPositions(layout, scope)
        is PatternLayout.Radial -> radialPositions(layout, scope)
        is PatternLayout.Custom -> layout.provider.positions(scope).map { it.clamped().toOffset(scope.containerSize) }
    }

    private fun randomPositions(layout: PatternLayout.Random, scope: PatternLayoutScope): List<Offset> {
        val area = scope.containerSize.width * scope.containerSize.height
        val count = max(1, ((area / 14_000f) * layout.density.coerceAtLeast(0.05f)).roundToInt())
        val random = Random(layout.seed)
        val halfWidth = scope.elementSize.width / 2f
        val halfHeight = scope.elementSize.height / 2f
        return List(count) {
            Offset(
                x = random.nextFloat() * (scope.containerSize.width - halfWidth * 2f).coerceAtLeast(0f) + halfWidth,
                y = random.nextFloat() * (scope.containerSize.height - halfHeight * 2f).coerceAtLeast(0f) + halfHeight,
            )
        }
    }

    private fun unevenPositions(layout: PatternLayout.Uneven, scope: PatternLayoutScope): List<Offset> {
        val area = scope.containerSize.width * scope.containerSize.height
        val count = max(1, ((area / 14_000f) * layout.density.coerceAtLeast(0.05f)).roundToInt())
        val random = Random(layout.seed)
        val halfWidth = scope.elementSize.width / 2f
        val halfHeight = scope.elementSize.height / 2f
        val minX = halfWidth
        val maxX = (scope.containerSize.width - halfWidth).coerceAtLeast(minX)
        val minY = halfHeight
        val maxY = (scope.containerSize.height - halfHeight).coerceAtLeast(minY)
        val clusterCount = layout.clusterCount.coerceAtLeast(1)
        val clusterSpread = layout.clusterSpread.coerceIn(0.02f, 0.6f)
        val backgroundBias = layout.backgroundBias.coerceIn(0f, 1f)
        val radiusX = scope.containerSize.width * clusterSpread
        val radiusY = scope.containerSize.height * clusterSpread
        val clusters = List(clusterCount) {
            Offset(
                x = random.nextFloat() * (maxX - minX) + minX,
                y = random.nextFloat() * (maxY - minY) + minY,
            )
        }
        return List(count) {
            if (random.nextFloat() < backgroundBias) {
                Offset(
                    x = random.nextFloat() * (maxX - minX) + minX,
                    y = random.nextFloat() * (maxY - minY) + minY,
                )
            } else {
                val center = clusters[random.nextInt(clusters.size)]
                val localX = (random.nextFloat() - random.nextFloat()) * radiusX
                val localY = (random.nextFloat() - random.nextFloat()) * radiusY
                Offset(
                    x = (center.x + localX).coerceIn(minX, maxX),
                    y = (center.y + localY).coerceIn(minY, maxY),
                )
            }
        }
    }

    private fun gridPositions(layout: PatternLayout.Grid, scope: PatternLayoutScope): List<Offset> {
        return with(scope.density) {
            val densityScale = layout.density.coerceAtLeast(0.25f)
            val step = (layout.spacing.toPx() / densityScale).coerceAtLeast(scope.elementSize.maxDimension)
            val rowOffset = layout.rowOffset.toPx()
            val columnOffset = layout.columnOffset.toPx()
            val positions = mutableListOf<Offset>()
            val halfWidth = scope.elementSize.width / 2f
            val halfHeight = scope.elementSize.height / 2f
            var row = 0
            var y = halfHeight
            while (y <= scope.containerSize.height - halfHeight + 1f) {
                val baseX = halfWidth + if (row % 2 == 0) rowOffset else columnOffset
                var x = baseX
                while (x <= scope.containerSize.width - halfWidth + 1f) {
                    positions += Offset(x = x.coerceIn(halfWidth, scope.containerSize.width - halfWidth), y = y)
                    x += step
                }
                row += 1
                y += step
            }
            positions
        }
    }

    private fun radialPositions(layout: PatternLayout.Radial, scope: PatternLayoutScope): List<Offset> {
        return with(scope.density) {
            val center = layout.center.clamped().toOffset(scope.containerSize)
            val ringSpacingPx = layout.ringSpacing.toPx().coerceAtLeast(scope.elementSize.maxDimension)
            val maxDistance = max(
                max(center.x, scope.containerSize.width - center.x),
                max(center.y, scope.containerSize.height - center.y),
            )
            val rings = ceil(maxDistance / ringSpacingPx).toInt().coerceAtLeast(1)
            val positions = mutableListOf(center)
            for (ringIndex in 1..rings) {
                val radius = ringSpacingPx * ringIndex
                val count = layout.elementsPerRing?.coerceAtLeast(1)
                    ?: max(6, ((2f * PI.toFloat() * radius) / (ringSpacingPx / layout.density.coerceAtLeast(0.25f))).roundToInt())
                repeat(count) { index ->
                    val angle = (PI * 2.0 * index / count).toFloat()
                    val point = Offset(
                        x = center.x + cos(angle) * radius,
                        y = center.y + sin(angle) * radius,
                    )
                    if (point.x in 0f..scope.containerSize.width && point.y in 0f..scope.containerSize.height) {
                        positions += point
                    }
                }
            }
            positions
        }
    }
}
