package dev.luma.visuals.defaults

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.luma.visuals.pattern.PatternBlur
import dev.luma.visuals.pattern.PatternDepth
import dev.luma.visuals.pattern.PatternElement
import dev.luma.visuals.pattern.PatternShadow
import dev.luma.visuals.pattern.PatternStyle
import dev.luma.visuals.pattern.animation.PatternInteraction
import dev.luma.visuals.pattern.animation.PatternRotation
import dev.luma.visuals.pattern.layout.PatternLayout

/** Preset styles for fast pattern prototyping. */
object PatternDefaults {
    private const val MagneticFieldSeed = 0x4D41474EL

    /** Returns a soft floating dot field with subtle depth and interaction response. */
    @Composable
    fun softDots(): PatternStyle = PatternStyle(
        element = PatternElement.Dot(radius = 5.dp, color = MaterialTheme.colorScheme.primaryContainer),
        layout = PatternLayout.Random(seed = 7L, density = 1.4f),
        shadow = PatternShadow.Drop(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), blurRadius = 18.dp),
        blur = PatternBlur.Soft(radius = 10.dp, passes = 2),
        rotation = PatternRotation.None,
        interaction = PatternInteraction.DistanceBasedOffset(maxOffset = 14.dp, intensity = 0.75f),
        alpha = 0.92f,
        depth = PatternDepth.Parallax(maxOffset = 8.dp, intensity = 0.28f),
    )

    /** Returns a gridded stripe pattern with gentle uniform motion. */
    @Composable
    fun ferriteStripes(): PatternStyle = PatternStyle(
        element = PatternElement.Shape(
            size = DpSize(28.dp, 8.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ),
        layout = PatternLayout.Grid(spacing = 36.dp, rowOffset = 18.dp, density = 1.1f),
        shadow = PatternShadow.Drop(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            offset = DpOffset(0.dp, 6.dp),
        ),
        blur = PatternBlur.Soft(radius = 8.dp, passes = 2, alphaMultiplier = 0.12f),
        rotation = PatternRotation.Uniform(degreesPerSecond = 4f),
        interaction = PatternInteraction.UniformOffset(maxOffset = 10.dp, intensity = 0.45f),
        alpha = 0.78f,
        elementTint = MaterialTheme.colorScheme.secondary,
    )

    /** Returns a radial field of custom diamond sprites with distributed rotation. */
    @Composable
    fun floatingSprites(): PatternStyle = PatternStyle(
        element = PatternElement.Shape(
            size = DpSize(22.dp, 22.dp),
            shape = GenericShape { size, _ ->
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width / 2f, size.height)
                lineTo(0f, size.height / 2f)
                close()
            },
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        layout = PatternLayout.Radial(ringSpacing = 48.dp, density = 1.15f),
        shadow = PatternShadow.Drop(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), blurRadius = 16.dp),
        blur = PatternBlur.Soft(radius = 12.dp, passes = 3, alphaMultiplier = 0.14f),
        rotation = PatternRotation.Distributed(baseDegreesPerSecond = 14f, variation = 10f, seed = 11L),
        interaction = PatternInteraction.DistanceBasedOffset(maxOffset = 20.dp, intensity = 0.65f),
        alpha = 0.88f,
        depth = PatternDepth.Parallax(maxOffset = 10.dp, intensity = 0.32f),
    )

    /** Returns a magnetic field-like line pattern inspired by the Horizon compass background. */
    @Composable
    fun magneticNorthField(): PatternStyle {
        val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        return PatternStyle(
            element = PatternElement.Custom(size = DpSize(4.dp, 16.dp)) { scope ->
                val lengthFraction = normalizedNodeValue(scope.nodeIndex, 0) * 0.45f + 0.35f
                val strokeFraction = normalizedNodeValue(scope.nodeIndex, 1) * 0.45f + 0.28f
                val localAlpha = normalizedNodeValue(scope.nodeIndex, 2) * 0.45f + 0.2f
                val color = lerp(
                    start = lineColor.copy(alpha = lineColor.alpha * 0.45f),
                    stop = lineColor,
                    fraction = localAlpha,
                )
                val start = Offset(scope.size.width / 2f, scope.size.height * (0.5f - lengthFraction / 2f))
                val end = Offset(scope.size.width / 2f, scope.size.height * (0.5f + lengthFraction / 2f))
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = scope.size.width * strokeFraction,
                    alpha = scope.alpha,
                    cap = StrokeCap.Round,
                )
            },
            layout = PatternLayout.Uneven(seed = MagneticFieldSeed, density = 3.7f, clusterCount = 7, clusterSpread = 0.14f),
            shadow = PatternShadow.None,
            rotation = PatternRotation.Follow(
                wobbleAmplitudeDegrees = 2f,
                wobbleSpeedMultiplier = 0.35f,
                variation = 6f,
                seed = MagneticFieldSeed,
            ),
            alpha = 1f,
        )
    }

    private fun normalizedNodeValue(nodeIndex: Int, salt: Int): Float {
        var value = MagneticFieldSeed.toInt() xor (nodeIndex * 0x45D9F3B) xor (salt * 0x27D4EB2D)
        value = value xor (value ushr 16)
        value *= 0x7FEB352D
        value = value xor (value ushr 15)
        value *= 0x846CA68B.toInt()
        value = value xor (value ushr 16)
        return (value ushr 1) / Int.MAX_VALUE.toFloat()
    }
}
