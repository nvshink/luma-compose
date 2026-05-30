package dev.luma.visuals.glow

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.luma.visuals.model.OffsetFraction

/**
 * Runtime inputs used by [FollowGlow].
 *
 * @property center Target glow center in fractional coordinates.
 * @property currentCenter Optional immediate center that bypasses animated interpolation when present.
 * @property intensity Multiplier applied to the rendered glow alpha.
 * @property isAnimated Enables animated interpolation toward [center] when [currentCenter] is absent.
 */
@Immutable
data class GlowState(
    val center: OffsetFraction,
    val currentCenter: OffsetFraction? = null,
    val intensity: Float = 1f,
    val isAnimated: Boolean = false,
)

/** Shape model used when drawing a glow mask. */
sealed interface GlowShape {
    data object Circle : GlowShape
    data object Ellipse : GlowShape

    /**
     * Rounded rectangle glow mask.
     *
     * @property cornerRadius Corner radius applied to the glow bounds.
     */
    @Immutable
    data class RoundedRect(
        val cornerRadius: Dp,
    ) : GlowShape

    /**
     * Custom glow mask renderer.
     *
     * @property draw Drawing lambda invoked for each glow pass.
     */
    @Immutable
    data class Custom(
        val draw: DrawScope.(GlowScope) -> Unit,
    ) : GlowShape
}

/** Describes how the inner color fades to transparent. */
enum class GlowFalloff {
    Radial,
    Soft,
    Focused,
}

/**
 * Draw-time metadata for custom glow masks.
 *
 * @property center Center point of the glow within the local drawing area.
 * @property size Expanded glow bounds including blur spread.
 * @property alpha Effective alpha for the current draw pass.
 * @property blurRadiusPx Blur radius expressed in pixels.
 * @property brush Gradient brush prepared for the glow shape.
 * @property blendMode Blend mode used for the glow draw pass.
 */
@Immutable
data class GlowScope(
    val center: Offset,
    val size: Size,
    val alpha: Float,
    val blurRadiusPx: Float,
    val brush: Brush,
    val blendMode: BlendMode,
)

/**
 * Immutable style bundle for [FollowGlow].
 *
 * @property color Base glow color at the center of the gradient.
 * @property alpha Base alpha before runtime intensity is applied.
 * @property blurRadius Soft expansion radius around the glow shape.
 * @property size Base size of the glow shape before blur expansion.
 * @property offset Local offset applied to the glow inside its host container without changing container size.
 * @property shape Mask shape used to render the glow.
 * @property falloff Gradient profile used between the center and the edge.
 * @property animationSpec Follow animation used when interpolating between glow centers.
 * @property blendMode Blend mode used when compositing the glow.
 */
@Immutable
data class GlowStyle(
    val color: Color,
    val alpha: Float = 0.4f,
    val blurRadius: Dp = 96.dp,
    val size: DpSize = DpSize(220.dp, 220.dp),
    val offset: DpOffset = DpOffset.Zero,
    val shape: GlowShape = GlowShape.Circle,
    val falloff: GlowFalloff = GlowFalloff.Radial,
    val animationSpec: FiniteAnimationSpec<OffsetFraction> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    ),
    val blendMode: BlendMode = BlendMode.SrcOver,
)
