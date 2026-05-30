package dev.luma.visuals.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.pow

/**
 * Fractional point inside a container where `0f..1f` maps to the full width and height.
 *
 * @property x Horizontal fraction relative to the container width.
 * @property y Vertical fraction relative to the container height.
 */
@Immutable
data class OffsetFraction(
    val x: Float,
    val y: Float,
) {
    /** Converts this fractional coordinate into a pixel [Offset] inside [size]. */
    fun toOffset(size: Size): Offset = Offset(
        x = size.width * x.coerceIn(0f, 1f),
        y = size.height * y.coerceIn(0f, 1f),
    )

    /** Returns a copy clamped to the inclusive `0f..1f` range on both axes. */
    fun clamped(): OffsetFraction = OffsetFraction(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
    )

    companion object {
        /** Center point of a rectangular container. */
        val Center = OffsetFraction(0.5f, 0.5f)

        /** Creates a fractional coordinate from a pixel [offset] inside [size]. */
        fun fromOffset(offset: Offset, size: Size): OffsetFraction {
            if (size.width <= 0f || size.height <= 0f) return Center
            return OffsetFraction(
                x = (offset.x / size.width).coerceIn(0f, 1f),
                y = (offset.y / size.height).coerceIn(0f, 1f),
            )
        }
    }
}

/** Maps normalized distance to a `0f..1f` influence value. */
sealed interface Falloff {
    /** Transforms a normalized distance into an influence amount. */
    fun transform(normalizedDistance: Float): Float

    data object Linear : Falloff {
        override fun transform(normalizedDistance: Float): Float = 1f - normalizedDistance.coerceIn(0f, 1f)
    }

    data object EaseIn : Falloff {
        override fun transform(normalizedDistance: Float): Float = (1f - normalizedDistance.coerceIn(0f, 1f)).pow(2)
    }

    data object EaseOut : Falloff {
        override fun transform(normalizedDistance: Float): Float {
            val value = 1f - normalizedDistance.coerceIn(0f, 1f)
            return 1f - (1f - value).pow(2)
        }
    }

    data object Smooth : Falloff {
        override fun transform(normalizedDistance: Float): Float {
            val value = 1f - normalizedDistance.coerceIn(0f, 1f)
            return value * value * (3f - 2f * value)
        }
    }

    data object Sharp : Falloff {
        override fun transform(normalizedDistance: Float): Float = (1f - normalizedDistance.coerceIn(0f, 1f)).pow(4)
    }
}
